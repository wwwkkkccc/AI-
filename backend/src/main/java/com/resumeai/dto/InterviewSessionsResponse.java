package com.resumeai.dto;

import java.util.List;

/**
 * 面试会话列表响应
 */
public class InterviewSessionsResponse {

    private List<SessionItem> items;
    private int page;
    private int size;
    private long total;
    private int totalPages;

    public List<SessionItem> getItems() {
        return items;
    }

    public void setItems(List<SessionItem> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public static class SessionItem {
        private Long id;
        private String targetRole;
        private String status;
        private Integer questionCount;
        private Double totalScore;
        private String createdAt;
        private String finishedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTargetRole() {
            return targetRole;
        }

        public void setTargetRole(String targetRole) {
            this.targetRole = targetRole;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getQuestionCount() {
            return questionCount;
        }

        public void setQuestionCount(Integer questionCount) {
            this.questionCount = questionCount;
        }

        public Double getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(Double totalScore) {
            this.totalScore = totalScore;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getFinishedAt() {
            return finishedAt;
        }

        public void setFinishedAt(String finishedAt) {
            this.finishedAt = finishedAt;
        }
    }
}
