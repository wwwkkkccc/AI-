package com.resumeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.dto.AnalyzeResponse;
import com.resumeai.dto.BatchStatusResponse;
import com.resumeai.dto.BatchSubmitResponse;
import com.resumeai.model.AnalysisBatch;
import com.resumeai.model.AnalysisJob;
import com.resumeai.model.UserAccount;
import com.resumeai.repository.AnalysisBatchRepository;
import com.resumeai.repository.AnalysisJobRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 批量简历分析服务
 */
@Service
public class BatchAnalysisService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_FAILED = "FAILED";

    private final AnalysisBatchRepository analysisBatchRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final AnalysisQueueService analysisQueueService;
    private final ObjectMapper objectMapper;

    public BatchAnalysisService(
            AnalysisBatchRepository analysisBatchRepository,
            AnalysisJobRepository analysisJobRepository,
            AnalysisQueueService analysisQueueService,
            ObjectMapper objectMapper) {
        this.analysisBatchRepository = analysisBatchRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.analysisQueueService = analysisQueueService;
        this.objectMapper = objectMapper;
    }

    /**
     * 提交批量分析任务
     */
    @Transactional
    public BatchSubmitResponse submitBatch(
            MultipartFile[] files,
            String jdText,
            String targetRole,
            UserAccount user) {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("no files provided");
        }
        if (files.length > 50) {
            throw new IllegalArgumentException("too many files, max 50");
        }

        // 创建批次记录
        String batchId = UUID.randomUUID().toString().replace("-", "");
        AnalysisBatch batch = new AnalysisBatch();
        batch.setId(batchId);
        batch.setUserId(user.getId());
        batch.setJobCount(files.length);
        batch.setCompletedCount(0);
        batch.setStatus(STATUS_PENDING);
        batch.setTargetRole(clean(targetRole));
        batch.setJdText(clean(jdText));
        batch.setCreatedAt(Instant.now());
        analysisBatchRepository.save(batch);

        // 为每个文件创建分析任务
        List<String> jobIds = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                var response = analysisQueueService.enqueue(file, jdText, targetRole, null, user);
                String jobId = response.getJobId();
                jobIds.add(jobId);

                // 更新任务的批次ID
                AnalysisJob job = analysisJobRepository.findById(jobId).orElse(null);
                if (job != null) {
                    job.setBatchId(batchId);
                    analysisJobRepository.save(job);
                }
            } catch (Exception ex) {
                // 跳过失败的文件，继续处理其他文件
            }
        }

        // 更新实际创建的任务数
        batch.setJobCount(jobIds.size());
        analysisBatchRepository.save(batch);

        BatchSubmitResponse response = new BatchSubmitResponse();
        response.setBatchId(batchId);
        response.setJobIds(jobIds);
        response.setStatus(STATUS_PENDING);
        response.setMessage("batch submitted with " + jobIds.size() + " jobs");
        return response;
    }

    /**
     * 查询批次状态
     */
    @Transactional(readOnly = true)
    public BatchStatusResponse getBatchStatus(String batchId, UserAccount user) {
        AnalysisBatch batch = analysisBatchRepository.findByIdAndUserId(batchId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("batch not found"));

        // 查询批次下的所有任务
        List<AnalysisJob> jobs = analysisJobRepository.findByBatchId(batchId);

        // 统计完成数量
        int completedCount = 0;
        List<BatchStatusResponse.BatchJobResult> results = new ArrayList<>();

        for (AnalysisJob job : jobs) {
            if (STATUS_DONE.equals(job.getStatus()) || STATUS_FAILED.equals(job.getStatus())) {
                completedCount++;
            }

            BatchStatusResponse.BatchJobResult result = new BatchStatusResponse.BatchJobResult();
            result.setJobId(job.getId());
            result.setFilename(job.getFilename());
            result.setStatus(job.getStatus());

            // 如果任务已完成，解析结果
            if (STATUS_DONE.equals(job.getStatus()) && job.getResultJson() != null) {
                try {
                    AnalyzeResponse analyzeResult = objectMapper.readValue(
                            job.getResultJson(), AnalyzeResponse.class);
                    result.setResult(analyzeResult);
                    if (analyzeResult.getScore() != null) {
                        result.setScore(analyzeResult.getScore());
                    }
                } catch (Exception ignore) {
                    // 解析失败，跳过
                }
            }

            results.add(result);
        }

        // 按分数降序排序（已完成的任务）
        results.sort(Comparator.comparing(
                r -> r.getScore() != null ? r.getScore() : -1.0,
                Comparator.reverseOrder()));

        // 更新批次状态
        String batchStatus = STATUS_PENDING;
        if (completedCount > 0 && completedCount < batch.getJobCount()) {
            batchStatus = STATUS_PROCESSING;
        } else if (completedCount == batch.getJobCount()) {
            batchStatus = STATUS_DONE;
        }

        // 如果批次状态变化，更新数据库
        if (!batchStatus.equals(batch.getStatus())) {
            batch.setStatus(batchStatus);
            batch.setCompletedCount(completedCount);
            if (STATUS_DONE.equals(batchStatus) && batch.getFinishedAt() == null) {
                batch.setFinishedAt(Instant.now());
            }
            analysisBatchRepository.save(batch);
        }

        BatchStatusResponse response = new BatchStatusResponse();
        response.setBatchId(batch.getId());
        response.setStatus(batchStatus);
        response.setJobCount(batch.getJobCount());
        response.setCompletedCount(completedCount);
        response.setProgress(batch.getJobCount() > 0 ? (completedCount * 100 / batch.getJobCount()) : 0);
        response.setTargetRole(batch.getTargetRole());
        response.setCreatedAt(batch.getCreatedAt());
        response.setFinishedAt(batch.getFinishedAt());
        response.setResults(results);

        return response;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
