package com.resumeai.dto;

import com.resumeai.dto.AnalyzeResponse;
import java.time.Instant;
import java.util.List;

public class BatchStatusResponse {
    private String batchId;
    private String status;
    private Integer jobCount;
    private Integer completedCount;
    private Integer progress;
    private String targetRole;
    private Instant createdAt;
    private Instant finishedAt;
    private List<BatchJobResult> results;

    public static class BatchJobResult {
        private String jobId;
        private String filename;
        private String status;
        private Double score;
        private AnalyzeResponse result;

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public AnalyzeResponse getResult() {
            return result;
        }

        public void setResult(AnalyzeResponse result) {
            this.result = result;
        }
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getJobCount() {
        return jobCount;
    }

    public void setJobCount(Integer jobCount) {
        this.jobCount = jobCount;
    }

    public Integer getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Integer completedCount) {
        this.completedCount = completedCount;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public List<BatchJobResult> getResults() {
        return results;
    }

    public void setResults(List<BatchJobResult> results) {
        this.results = results;
    }
}
