package com.resumeai.dto;

import java.util.List;

/**
 * 面试消息响应
 */
public class InterviewMessageResponse {

    private Long sessionId;
    private String targetRole;
    private String status;
    private Integer questionCount;
    private Double totalScore;
    private List<MessageItem> messages;
    private InterviewReport report;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
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

    public List<MessageItem> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageItem> messages) {
        this.messages = messages;
    }

    public InterviewReport getReport() {
        return report;
    }

    public void setReport(InterviewReport report) {
        this.report = report;
    }

    public static class MessageItem {
        private Long id;
        private String role;
        private String content;
        private Double score;
        private String feedback;
        private String createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public String getFeedback() {
            return feedback;
        }

        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class InterviewReport {
        private String report;
        private List<String> strengths;
        private List<String> weaknesses;
        private List<String> suggestions;

        public String getReport() {
            return report;
        }

        public void setReport(String report) {
            this.report = report;
        }

        public List<String> getStrengths() {
            return strengths;
        }

        public void setStrengths(List<String> strengths) {
            this.strengths = strengths;
        }

        public List<String> getWeaknesses() {
            return weaknesses;
        }

        public void setWeaknesses(List<String> weaknesses) {
            this.weaknesses = weaknesses;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
        }
    }
}
