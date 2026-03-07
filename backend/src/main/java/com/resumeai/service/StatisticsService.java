package com.resumeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.dto.AdminStatsResponse;
import com.resumeai.dto.AnalyzeResponse;
import com.resumeai.dto.UserStatsResponse;
import com.resumeai.model.AnalysisRecord;
import com.resumeai.repository.AnalysisRecordRepository;
import com.resumeai.repository.UserAccountRepository;
import com.resumeai.repository.UserSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AnalysisRecordRepository analysisRecordRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    public StatisticsService(
            AnalysisRecordRepository analysisRecordRepository,
            UserAccountRepository userAccountRepository,
            UserSessionRepository userSessionRepository,
            EntityManager entityManager,
            ObjectMapper objectMapper) {
        this.analysisRecordRepository = analysisRecordRepository;
        this.userAccountRepository = userAccountRepository;
        this.userSessionRepository = userSessionRepository;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(Long userId) {
        UserStatsResponse response = new UserStatsResponse();

        // 总分析次数
        String countSql = "SELECT COUNT(*) FROM analysis_records WHERE user_id = :userId";
        Query countQuery = entityManager.createNativeQuery(countSql);
        countQuery.setParameter("userId", userId);
        Number countResult = (Number) countQuery.getSingleResult();
        response.setTotalAnalyses(countResult.longValue());

        // 平均分数
        String avgSql = "SELECT AVG(score) FROM analysis_records WHERE user_id = :userId";
        Query avgQuery = entityManager.createNativeQuery(avgSql);
        avgQuery.setParameter("userId", userId);
        Number avgResult = (Number) avgQuery.getSingleResult();
        response.setAvgScore(avgResult != null ? avgResult.doubleValue() : 0.0);

        // 最近 20 条分数历史
        Pageable pageable = PageRequest.of(0, 20);
        List<AnalysisRecord> recentRecords = analysisRecordRepository.findByUserIdOrderByIdDesc(userId, pageable).getContent();
        List<UserStatsResponse.ScoreHistoryItem> scoreHistory = new ArrayList<>();
        for (AnalysisRecord record : recentRecords) {
            UserStatsResponse.ScoreHistoryItem item = new UserStatsResponse.ScoreHistoryItem();
            item.setDate(formatDate(record.getCreatedAt()));
            item.setScore(record.getScore());
            item.setTargetRole(clean(record.getTargetRole()));
            scoreHistory.add(item);
        }
        response.setScoreHistory(scoreHistory);

        // 统计匹配和缺失关键词
        Map<String, Long> matchedMap = new HashMap<>();
        Map<String, Long> missingMap = new HashMap<>();

        for (AnalysisRecord record : recentRecords) {
            if (record.getResultJson() != null && !record.getResultJson().isBlank()) {
                try {
                    AnalyzeResponse result = objectMapper.readValue(record.getResultJson(), AnalyzeResponse.class);
                    if (result.getMatchedKeywords() != null) {
                        for (String kw : result.getMatchedKeywords()) {
                            matchedMap.put(kw, matchedMap.getOrDefault(kw, 0L) + 1);
                        }
                    }
                    if (result.getMissingKeywords() != null) {
                        for (String kw : result.getMissingKeywords()) {
                            missingMap.put(kw, missingMap.getOrDefault(kw, 0L) + 1);
                        }
                    }
                } catch (Exception ignore) {
                    // 解析失败跳过
                }
            }
        }

        // Top 10 匹配关键词
        List<UserStatsResponse.KeywordCount> topMatched = matchedMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> new UserStatsResponse.KeywordCount(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        response.setTopMatchedKeywords(topMatched);

        // Top 10 缺失关键词
        List<UserStatsResponse.KeywordCount> topMissing = missingMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> new UserStatsResponse.KeywordCount(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        response.setTopMissingKeywords(topMissing);

        return response;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getAdminStats() {
        AdminStatsResponse response = new AdminStatsResponse();

        // 总用户数
        long totalUsers = userAccountRepository.count();
        response.setTotalUsers(totalUsers);

        // 30 天内活跃用户数（有登录会话）
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        String activeUsersSql = "SELECT COUNT(DISTINCT user_id) FROM user_sessions WHERE created_at >= :since";
        Query activeUsersQuery = entityManager.createNativeQuery(activeUsersSql);
        activeUsersQuery.setParameter("since", thirtyDaysAgo);
        Number activeUsersResult = (Number) activeUsersQuery.getSingleResult();
        response.setActiveUsers(activeUsersResult.longValue());

        // 总分析次数
        long totalAnalyses = analysisRecordRepository.count();
        response.setTotalAnalyses(totalAnalyses);

        // 平均分数
        String avgSql = "SELECT AVG(score) FROM analysis_records";
        Query avgQuery = entityManager.createNativeQuery(avgSql);
        Number avgResult = (Number) avgQuery.getSingleResult();
        response.setAvgScore(avgResult != null ? avgResult.doubleValue() : 0.0);

        // 最近 30 天每日分析量
        String dailySql = "SELECT DATE(created_at) as date, COUNT(*) as count " +
                          "FROM analysis_records " +
                          "WHERE created_at >= :since " +
                          "GROUP BY DATE(created_at) " +
                          "ORDER BY date DESC";
        Query dailyQuery = entityManager.createNativeQuery(dailySql);
        dailyQuery.setParameter("since", thirtyDaysAgo);
        @SuppressWarnings("unchecked")
        List<Object[]> dailyResults = dailyQuery.getResultList();
        List<AdminStatsResponse.DailyCount> dailyCounts = new ArrayList<>();
        for (Object[] row : dailyResults) {
            String date = row[0].toString();
            Long count = ((Number) row[1]).longValue();
            dailyCounts.add(new AdminStatsResponse.DailyCount(date, count));
        }
        response.setDailyAnalysisCounts(dailyCounts);

        // Top 10 热门岗位
        String rolesSql = "SELECT target_role, COUNT(*) as count " +
                          "FROM analysis_records " +
                          "WHERE target_role IS NOT NULL AND target_role != '' " +
                          "GROUP BY target_role " +
                          "ORDER BY count DESC " +
                          "LIMIT 10";
        Query rolesQuery = entityManager.createNativeQuery(rolesSql);
        @SuppressWarnings("unchecked")
        List<Object[]> rolesResults = rolesQuery.getResultList();
        List<AdminStatsResponse.RoleCount> popularRoles = new ArrayList<>();
        for (Object[] row : rolesResults) {
            String role = row[0].toString();
            Long count = ((Number) row[1]).longValue();
            popularRoles.add(new AdminStatsResponse.RoleCount(role, count));
        }
        response.setPopularRoles(popularRoles);

        return response;
    }

    private String formatDate(Instant instant) {
        if (instant == null) return "";
        return instant.atZone(ZoneId.systemDefault()).format(DATE_FORMATTER);
    }

    private String clean(String value) {
        return value == null ? "" : value;
    }
}
