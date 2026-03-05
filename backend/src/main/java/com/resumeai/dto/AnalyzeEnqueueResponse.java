package com.resumeai.dto;

public class AnalyzeEnqueueResponse {
    private String jobId;
    private String status;
    private Long queuePosition;
    private Boolean vipPriority;
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
