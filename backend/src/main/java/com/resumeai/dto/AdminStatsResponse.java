package com.resumeai.dto;

import java.util.List;

public class AdminStatsResponse {
    private Long totalUsers;
    private Long activeUsers;
    private Long totalAnalyses;
    private Double avgScore;
    private List<DailyCount> dailyAnalysisCounts;
    private List<RoleCount> popularRoles;

    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(Long activeUsers) {
        this.activeUsers = activeUsers;
    }

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

    public List<DailyCount> getDailyAnalysisCounts() {
        return dailyAnalysisCounts;
    }

    public void setDailyAnalysisCounts(List<DailyCount> dailyAnalysisCounts) {
        this.dailyAnalysisCounts = dailyAnalysisCounts;
    }

    public List<RoleCount> getPopularRoles() {
        return popularRoles;
    }

    public void setPopularRoles(List<RoleCount> popularRoles) {
        this.popularRoles = popularRoles;
    }

    public static class DailyCount {
        private String date;
        private Long count;

        public DailyCount(String date, Long count) {
            this.date = date;
            this.count = count;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }

    public static class RoleCount {
        private String role;
        private Long count;

        public RoleCount(String role, Long count) {
            this.role = role;
            this.count = count;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }
}
