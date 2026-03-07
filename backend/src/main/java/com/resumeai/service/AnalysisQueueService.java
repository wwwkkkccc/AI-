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
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 分析任务排队服务。
 * <p>
 * 职责：
 * 1. 接收分析请求并将上传文件落盘
 * 2. 以 Redis ZSet 维护任务队列（VIP 用户优先）
 * 3. 定时消费队列并调用 AnalyzeService 执行分析
 * 4. 提供任务状态查询（排队中/处理中/完成/失败）
 */
@Service
public class AnalysisQueueService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisQueueService.class);
    private static final String QUEUE_KEY = "resume_ai:analysis:queue:zset";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_FAILED = "FAILED";
    // 普通用户的优先级偏移量，保证 VIP 用户（偏移为 0）始终排在前面
    private static final long NORMAL_PRIORITY_OFFSET = 1_000_000_000_000_000L;

    private final Map<String, List<SseEmitter>> jobEmitters = new ConcurrentHashMap<>();

    private final StringRedisTemplate redisTemplate;
    private final AnalysisJobRepository analysisJobRepository;
    private final UserAccountRepository userAccountRepository;
    private final AnalyzeService analyzeService;
    private final ObjectMapper objectMapper;
    private final Path uploadDir;
    private final ExecutorService workerPool;
    private final Semaphore workerSlots;
    private final int consumeBatchSize;

    /** 构造函数，注入依赖并初始化上传目录路径。 */
    public AnalysisQueueService(
            StringRedisTemplate redisTemplate,
            AnalysisJobRepository analysisJobRepository,
            UserAccountRepository userAccountRepository,
            AnalyzeService analyzeService,
            ObjectMapper objectMapper,
            @Value("${app.queue.upload-dir:/tmp/resume-ai-jobs}") String uploadDir,
            @Value("${app.queue.worker-count:2}") int workerCount,
            @Value("${app.queue.consume-batch-size:2}") int consumeBatchSize) {
        this.redisTemplate = redisTemplate;
        this.analysisJobRepository = analysisJobRepository;
        this.userAccountRepository = userAccountRepository;
        this.analyzeService = analyzeService;
        this.objectMapper = objectMapper;
        this.uploadDir = Path.of(uploadDir);
        int safeWorkers = Math.max(1, workerCount);
        this.workerPool = Executors.newFixedThreadPool(safeWorkers);
        this.workerSlots = new Semaphore(safeWorkers);
        this.consumeBatchSize = Math.max(1, consumeBatchSize);
    }

    /** 服务启动时创建上传目录，并把数据库中的待处理任务重新放回队列。 */
    @PostConstruct
    public void initQueue() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to create upload dir");
        }
        restorePendingJobs();
    }

    /**
     * 将分析请求入队。
     * <p>
     * 流程：校验用户状态 -> 解析 JD 文本 -> 保存上传文件 -> 创建任务记录 -> 写入 Redis 有序集合。
     *
     * @param file       简历文件
     * @param jdText     职位描述文本
     * @param targetRole 目标岗位名称（可选）
     * @param jdImage    JD 截图（可选，用于 OCR）
     * @param user       当前登录用户
     * @return 入队结果，包含 jobId、队列位置和 VIP 优先标识
     */
    @Transactional
    public AnalyzeEnqueueResponse enqueue(
            MultipartFile file,
            String jdText,
            String targetRole,
            MultipartFile jdImage,
            UserAccount user) {
        // 黑名单用户禁止提交
        if (Boolean.TRUE.equals(user.getBlacklisted())) {
            throw new ForbiddenException("account is blacklisted");
        }
        String finalJdText = analyzeService.resolveJdText(jdText, jdImage);
        analyzeService.validateSubmission(file, finalJdText, targetRole);

        String jobId = UUID.randomUUID().toString().replace("-", "");
        String filename = safeFilename(file.getOriginalFilename());
        Path filePath = uploadDir.resolve(jobId + "_" + filename);

        // 将上传文件写入磁盘，后续由消费者读取
        try {
            Files.write(filePath, file.getBytes());
        } catch (IOException ex) {
            throw new IllegalArgumentException("failed to save upload file");
        }

        // 创建任务记录并持久化到数据库
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

        // 写入 Redis ZSet，score 越小越先被消费
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

    /**
     * 查询任务状态。
     * 普通用户只能查看自己的任务，管理员可查看全部。
     *
     * @param jobId     任务 ID
     * @param requester 请求者用户
     * @param adminMode 是否管理员模式
     * @return 任务状态详情，完成时包含分析结果
     */
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

        // 排队中时返回当前队列位置
        if (STATUS_PENDING.equals(job.getStatus())) {
            Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY, job.getId());
            response.setQueuePosition(rank == null ? null : rank + 1);
        }

        // 已完成时反序列化分析结果
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

    /**
     * 定时消费队列，每次仅弹出并处理一个任务，避免并发冲突和资源争抢。
     * 调度间隔由配置 app.queue.poll-ms 控制，默认 700ms。
     */
    @Scheduled(fixedDelayString = "${app.queue.poll-ms:700}")
    public void consumeOne() {
        for (int i = 0; i < consumeBatchSize; i++) {
            if (!workerSlots.tryAcquire()) {
                break;
            }
            TypedTuple<String> tuple = redisTemplate.opsForZSet().popMin(QUEUE_KEY);
            if (tuple == null || tuple.getValue() == null) {
                workerSlots.release();
                break;
            }
            String jobId = tuple.getValue();
            workerPool.submit(() -> {
                try {
                    processJob(jobId);
                } finally {
                    workerSlots.release();
                }
            });
        }
    }

    /**
     * 真正执行任务：更新状态为处理中 -> 调用分析服务 -> 保存结果 -> 清理临时文件。
     * 任何异常都会被捕获并标记为 FAILED。
     */
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
            notifyEmitters(jobId, Map.of("status", STATUS_PROCESSING));

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
            notifyEmitters(jobId, Map.of("status", STATUS_DONE, "result", result));
        } catch (Exception ex) {
            job.setStatus(STATUS_FAILED);
            job.setFinishedAt(Instant.now());
            job.setErrorMessage(cut(clean(ex.getMessage()), 1000));
            analysisJobRepository.save(job);
            notifyEmitters(jobId, Map.of("status", STATUS_FAILED, "error", clean(ex.getMessage())));
        } finally {
            cleanupJobFile(job);
            completeEmitters(jobId);
        }
    }

    /** 应用重启恢复：将数据库中的 PENDING 任务重新补回 Redis 队列。 */
    private void restorePendingJobs() {
        List<AnalysisJob> pending = analysisJobRepository.findByStatus(STATUS_PENDING);
        for (AnalysisJob job : pending) {
            Double score = redisTemplate.opsForZSet().score(QUEUE_KEY, job.getId());
            if (score == null) {
                redisTemplate.opsForZSet().add(QUEUE_KEY, job.getId(), queueScore(job));
            }
        }
    }

    /**
     * 队列评分策略：VIP 偏移为 0，普通用户偏移为 NORMAL_PRIORITY_OFFSET。
     * score 越小越先被 popMin 取出，因此 VIP 始终优先。
     */
    private double queueScore(AnalysisJob job) {
        long offset = job.getPriorityLevel() != null && job.getPriorityLevel() == 0 ? 0L : NORMAL_PRIORITY_OFFSET;
        return offset + job.getCreatedAt().toEpochMilli();
    }

    /** 临时文件清理，采用 best-effort 策略，不让清理异常影响主流程。 */
    private void cleanupJobFile(AnalysisJob job) {
        if (job == null || job.getFilePath() == null || job.getFilePath().isBlank()) return;
        try {
            Files.deleteIfExists(Path.of(job.getFilePath()));
        } catch (IOException ignore) {
            // 尽力清理，忽略失败
        }
    }

    /**
     * 安全化文件名：移除非法字符，限制长度，统一小写。
     * @param filename 原始文件名
     * @return 安全的文件名
     */
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

    /** 空值安全的字符串清理。 */
    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    /** 截断字符串到指定最大长度。 */
    private String cut(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, max);
    }

    public void registerEmitter(String jobId, SseEmitter emitter) {
        jobEmitters.computeIfAbsent(jobId, k -> new ArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(jobId, emitter));
        emitter.onTimeout(() -> removeEmitter(jobId, emitter));
        emitter.onError(e -> removeEmitter(jobId, emitter));
    }

    public void removeEmitter(String jobId, SseEmitter emitter) {
        List<SseEmitter> emitters = jobEmitters.get(jobId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                jobEmitters.remove(jobId);
            }
        }
    }

    private void notifyEmitters(String jobId, Map<String, Object> data) {
        List<SseEmitter> emitters = jobEmitters.get(jobId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(data));
            } catch (Exception ex) {
                log.warn("Failed to send SSE event for job {}: {}", jobId, ex.getMessage());
                deadEmitters.add(emitter);
            }
        }
        for (SseEmitter dead : deadEmitters) {
            removeEmitter(jobId, dead);
        }
    }

    private void completeEmitters(String jobId) {
        List<SseEmitter> emitters = jobEmitters.remove(jobId);
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.complete();
                } catch (Exception ignore) {
                    // 尽力完成
                }
            }
        }
    }

    @PreDestroy
    public void shutdownWorkers() {
        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(8, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            workerPool.shutdownNow();
        }
    }
}
