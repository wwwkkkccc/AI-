package com.resumeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.dto.AnalyzeEnqueueResponse;
import com.resumeai.dto.AnalyzeJobStatusResponse;
import com.resumeai.dto.AnalyzeResponse;
import com.resumeai.model.AnalysisJob;
import com.resumeai.model.UserAccount;
import com.resumeai.repository.AnalysisJobRepository;
import com.resumeai.repository.UserAccountRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AnalysisQueueService {
    /*
     * 排队服务职责:
     * 1) 接收分析请求并落盘文件
     * 2) 以 Redis ZSet 维护任务队列（VIP 优先）
     * 3) 定时消费队列并调用 AnalyzeService 执行分析
     * 4) 提供任务状态查询（排队中/处理中/完成/失败）
     */
    private static final String QUEUE_KEY = "resume_ai:analysis:queue:zset";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_FAILED = "FAILED";
    private static final long NORMAL_PRIORITY_OFFSET = 1_000_000_000_000_000L;

    private final StringRedisTemplate redisTemplate;
    private final AnalysisJobRepository analysisJobRepository;
    private final UserAccountRepository userAccountRepository;
    private final AnalyzeService analyzeService;
    private final ObjectMapper objectMapper;
    private final Path uploadDir;

    public AnalysisQueueService(
            StringRedisTemplate redisTemplate,
            AnalysisJobRepository analysisJobRepository,
            UserAccountRepository userAccountRepository,
            AnalyzeService analyzeService,
            ObjectMapper objectMapper,
            @Value("${app.queue.upload-dir:/tmp/resume-ai-jobs}") String uploadDir) {
        this.redisTemplate = redisTemplate;
        this.analysisJobRepository = analysisJobRepository;
        this.userAccountRepository = userAccountRepository;
        this.analyzeService = analyzeService;
        this.objectMapper = objectMapper;
        this.uploadDir = Path.of(uploadDir);
    }

    // 服务启动时创建上传目录，并把数据库中的待处理任务重新放回队列
    @PostConstruct
    public void initQueue() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to create upload dir");
        }
        restorePendingJobs();
    }

    // 入队: 保存上传文件、生成任务记录、写入 Redis 有序集合
    @Transactional
    public AnalyzeEnqueueResponse enqueue(
            MultipartFile file,
            String jdText,
            String targetRole,
            MultipartFile jdImage,
            UserAccount user) {
        if (Boolean.TRUE.equals(user.getBlacklisted())) {
            throw new ForbiddenException("account is blacklisted");
        }
        String finalJdText = analyzeService.resolveJdText(jdText, jdImage);
        analyzeService.validateSubmission(file, finalJdText, targetRole);

        String jobId = UUID.randomUUID().toString().replace("-", "");
        String filename = safeFilename(file.getOriginalFilename());
        Path filePath = uploadDir.resolve(jobId + "_" + filename);

        try {
            Files.write(filePath, file.getBytes());
        } catch (IOException ex) {
            throw new IllegalArgumentException("failed to save upload file");
        }

        AnalysisJob job = new AnalysisJob();
        job.setId(jobId);
        job.setUserId(user.getId());
        job.setUsername(user.getUsername());
        job.setFilename(filename);
        job.setFilePath(filePath.toString());
        job.setJdText(clean(finalJdText));
        job.setTargetRole(clean(targetRole));
        job.setPriorityLevel(Boolean.TRUE.equals(user.getVip()) ? 0 : 1);
        job.setStatus(STATUS_PENDING);
        job.setCreatedAt(Instant.now());
        analysisJobRepository.save(job);

        double score = queueScore(job);
        redisTemplate.opsForZSet().add(QUEUE_KEY, jobId, score);
        Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY, jobId);

        AnalyzeEnqueueResponse response = new AnalyzeEnqueueResponse();
        response.setJobId(jobId);
        response.setStatus(STATUS_PENDING);
        response.setQueuePosition(rank == null ? null : rank + 1);
        response.setVipPriority(Boolean.TRUE.equals(user.getVip()));
        response.setMessage(Boolean.TRUE.equals(user.getVip()) ? "VIP priority queued" : "queued");
        return response;
    }

    // 查询任务状态; 普通用户只能看自己的任务，管理员可查看全部
    @Transactional(readOnly = true)
    public AnalyzeJobStatusResponse getJobStatus(String jobId, UserAccount requester, boolean adminMode) {
        AnalysisJob job = analysisJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("job not found"));
        if (!adminMode && !job.getUserId().equals(requester.getId())) {
            throw new ForbiddenException("cannot access this job");
        }

        AnalyzeJobStatusResponse response = new AnalyzeJobStatusResponse();
        response.setJobId(job.getId());
        response.setStatus(job.getStatus());
        response.setErrorMessage(job.getErrorMessage());
        response.setCreatedAt(job.getCreatedAt());
        response.setStartedAt(job.getStartedAt());
        response.setFinishedAt(job.getFinishedAt());

        if (STATUS_PENDING.equals(job.getStatus())) {
            Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY, job.getId());
            response.setQueuePosition(rank == null ? null : rank + 1);
        }

        if (STATUS_DONE.equals(job.getStatus()) && job.getResultJson() != null) {
            try {
                AnalyzeResponse result = objectMapper.readValue(job.getResultJson(), AnalyzeResponse.class);
                response.setResult(result);
            } catch (Exception ignore) {
                response.setErrorMessage("result parse failed");
            }
        }
        return response;
    }

    // 定时消费队列，每次仅弹出并处理一个任务，避免并发冲突和资源争抢
    @Scheduled(fixedDelayString = "${app.queue.poll-ms:700}")
    public void consumeOne() {
        TypedTuple<String> tuple = redisTemplate.opsForZSet().popMin(QUEUE_KEY);
        if (tuple == null || tuple.getValue() == null) {
            return;
        }
        processJob(tuple.getValue());
    }

    // 真正执行任务: 更新状态 -> 调分析服务 -> 保存结果 -> 清理临时文件
    private void processJob(String jobId) {
        AnalysisJob job = analysisJobRepository.findById(jobId).orElse(null);
        if (job == null || !STATUS_PENDING.equals(job.getStatus())) {
            cleanupJobFile(job);
            return;
        }

        try {
            job.setStatus(STATUS_PROCESSING);
            job.setStartedAt(Instant.now());
            job.setErrorMessage(null);
            analysisJobRepository.save(job);

            UserAccount user = userAccountRepository.findById(job.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("user not found"));
            if (Boolean.TRUE.equals(user.getBlacklisted())) {
                throw new ForbiddenException("account is blacklisted");
            }

            AnalyzeResponse result = analyzeService.processQueuedJob(job, user);
            job.setStatus(STATUS_DONE);
            job.setFinishedAt(Instant.now());
            job.setResultJson(objectMapper.writeValueAsString(result));
            analysisJobRepository.save(job);
        } catch (Exception ex) {
            job.setStatus(STATUS_FAILED);
            job.setFinishedAt(Instant.now());
            job.setErrorMessage(cut(clean(ex.getMessage()), 1000));
            analysisJobRepository.save(job);
        } finally {
            cleanupJobFile(job);
        }
    }

    // 应用重启恢复: 将数据库中的 PENDING 任务重新补回 Redis 队列
    private void restorePendingJobs() {
        List<AnalysisJob> pending = analysisJobRepository.findByStatus(STATUS_PENDING);
        for (AnalysisJob job : pending) {
            Double score = redisTemplate.opsForZSet().score(QUEUE_KEY, job.getId());
            if (score == null) {
                redisTemplate.opsForZSet().add(QUEUE_KEY, job.getId(), queueScore(job));
            }
        }
    }

    // 队列评分策略: VIP 偏移更小 => 分值更小 => 在 ZSet 中更先被 popMin 取出
    private double queueScore(AnalysisJob job) {
        long offset = job.getPriorityLevel() != null && job.getPriorityLevel() == 0 ? 0L : NORMAL_PRIORITY_OFFSET;
        return offset + job.getCreatedAt().toEpochMilli();
    }

    // 临时文件清理采用 best-effort，不让清理异常影响主流程
    private void cleanupJobFile(AnalysisJob job) {
        if (job == null || job.getFilePath() == null || job.getFilePath().isBlank()) return;
        try {
            Files.deleteIfExists(Path.of(job.getFilePath()));
        } catch (IOException ignore) {
            // best effort cleanup
        }
    }

    private String safeFilename(String filename) {
        String name = clean(filename);
        if (name.isEmpty()) {
            return "resume.txt";
        }
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.length() > 120) {
            name = name.substring(name.length() - 120);
        }
        return name.toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cut(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, max);
    }
}
