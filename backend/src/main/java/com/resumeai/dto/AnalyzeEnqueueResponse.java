package com.resumeai.dto;

/**
 * 简历分析任务入队响应 DTO，用于返回异步分析任务提交后的排队信息
 */
public class AnalyzeEnqueueResponse {
    /** 异步任务 ID */
    private String jobId;
    /** 任务状态（如 QUEUED） */
    private String status;
    /** 当前在队列中的位置 */
    private Long queuePosition;
    /** 是否享有 VIP 优先级 */
    private Boolean vipPriority;
    /** 提示信息 */
    private String message;

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

    public Boolean getVipPriority() {
        return vipPriority;
    }

    public void setVipPriority(Boolean vipPriority) {
        this.vipPriority = vipPriority;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
