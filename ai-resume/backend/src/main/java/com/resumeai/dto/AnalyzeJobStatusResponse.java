package com.resumeai.dto;

import java.time.Instant;

/**
 * 简历分析任务状态查询响应 DTO，用于轮询异步分析任务的执行状态和结果
 */
public class AnalyzeJobStatusResponse {
    /** 异步任务 ID */
    private String jobId;
    /** 任务状态（如 QUEUED、RUNNING、COMPLETED、FAILED） */
    private String status;
    /** 当前在队列中的位置（排队中时有值） */
    private Long queuePosition;
    /** 任务失败时的错误信息 */
    private String errorMessage;
    /** 分析完成后的结果 */
    private AnalyzeResponse result;
    /** 任务创建时间 */
    private Instant createdAt;
    /** 任务开始执行时间 */
    private Instant startedAt;
    /** 任务完成时间 */
    private Instant finishedAt;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(Long queuePosition) {
        this.queuePosition = queuePosition;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public AnalyzeResponse getResult() {
        return result;
    }

    public void setResult(AnalyzeResponse result) {
        this.result = result;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
