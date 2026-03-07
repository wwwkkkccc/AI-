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
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Queue-based async analysis executor.
 * Uses Redis ZSet as priority queue and DB as durable job state.
 */
@Service
public class AnalysisQueueService {

    private static final String QUEUE_KEY = "resume_ai:analysis:queue:zset";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_FAILED = "FAILED";

    // Non-VIP jobs are offset so VIP users are always dequeued first.
    private static final long NORMAL_PRIORITY_OFFSET = 1_000_000_000_000_000L;

    private final StringRedisTemplate redisTemplate;
    private final AnalysisJobRepository analysisJobRepository;
    private final UserAccountRepository userAccountRepository;
    private final AnalyzeService analyzeService;
    private final ObjectMapper objectMapper;
    private final Path uploadDir;
    private final ExecutorService workerPool;
    private final Semaphore workerSlots;
    private final int consumeBatchSize;

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

    @PostConstruct
    public void initQueue() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to create upload dir");
        }
        restorePendingJobs();
    }

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

    private void restorePendingJobs() {
        List<AnalysisJob> pending = analysisJobRepository.findByStatus(STATUS_PENDING);
        for (AnalysisJob job : pending) {
            Double score = redisTemplate.opsForZSet().score(QUEUE_KEY, job.getId());
            if (score == null) {
                redisTemplate.opsForZSet().add(QUEUE_KEY, job.getId(), queueScore(job));
            }
        }
    }

    private double queueScore(AnalysisJob job) {
        long offset = job.getPriorityLevel() != null && job.getPriorityLevel() == 0 ? 0L : NORMAL_PRIORITY_OFFSET;
        return offset + job.getCreatedAt().toEpochMilli();
    }

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

