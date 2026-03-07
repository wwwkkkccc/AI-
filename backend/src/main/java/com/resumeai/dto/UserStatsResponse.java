package com.resumeai.dto;

import java.util.List;
import java.util.Map;

public class UserStatsResponse {
    private Long totalAnalyses;
    private Double avgScore;
    private List<ScoreHistoryItem> scoreHistory;
    private List<KeywordCount> topMatchedKeywords;
    private List<KeywordCount> topMissingKeywords;

    public Long getTotalAnalyses() {
        return totalAnalyses;
    }

    public void setTotalAnalyses(Long totalAnalyses) {
        this.totalAnalyses = totalAnalyses;
    }

    public Double getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(Double avgScore) {
        this.avgScore = avgScore;
    }

    public List<ScoreHistoryItem> getScoreHistory() {
        return scoreHistory;
    }

    public void setScoreHistory(List<ScoreHistoryItem> scoreHistory) {
        this.scoreHistory = scoreHistory;
    }

    public List<KeywordCount> getTopMatchedKeywords() {
        return topMatchedKeywords;
    }

    public void setTopMatchedKeywords(List<KeywordCount> topMatchedKeywords) {
        this.topMatchedKeywords = topMatchedKeywords;
    }

    public List<KeywordCount> getTopMissingKeywords() {
        return topMissingKeywords;
    }

    public void setTopMissingKeywords(List<KeywordCount> topMissingKeywords) {
        this.topMissingKeywords = topMissingKeywords;
    }

    public static class ScoreHistoryItem {
        private String date;
        private Double score;
        private String targetRole;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public String getTargetRole() {
            return targetRole;
        }

        public void setTargetRole(String targetRole) {
            this.targetRole = targetRole;
        }
    }

    public static class KeywordCount {
        private String keyword;
        private Long count;

        public KeywordCount(String keyword, Long count) {
            this.keyword = keyword;
            this.count = count;
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }
}
