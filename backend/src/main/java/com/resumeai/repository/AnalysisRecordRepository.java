package com.resumeai.repository;

import com.resumeai.model.AnalysisRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 分析记录数据访问接口，提供analysis_records表的分页查询操作
 */
public interface AnalysisRecordRepository extends JpaRepository<AnalysisRecord, Long> {
    // 根据用户ID分页查询分析记录，按ID倒序
    Page<AnalysisRecord> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
    // 分页查询所有分析记录，按ID倒序
    Page<AnalysisRecord> findAllByOrderByIdDesc(Pageable pageable);
    // 根据用户名模糊搜索分析记录（忽略大小写），按ID倒序
    Page<AnalysisRecord> findByUsernameContainingIgnoreCaseOrderByIdDesc(String username, Pageable pageable);

    @Query(
            value = """
                    SELECT
                      ar.id AS id,
                      ar.user_id AS userId,
                      ar.username AS username,
                      ar.filename AS filename,
                      ar.target_role AS targetRole,
                      ar.score AS score,
                      ar.coverage AS coverage,
                      ar.optimized_summary AS optimizedSummary,
                      ar.model_used AS modelUsed,
                      TRIM(REPLACE(REPLACE(SUBSTRING(COALESCE(ar.resume_text, ''), 1, 300), '\r', ' '), '\n', ' ')) AS resumePreview,
                      TRIM(REPLACE(REPLACE(SUBSTRING(COALESCE(ar.jd_text, ''), 1, 240), '\r', ' '), '\n', ' ')) AS jdPreview,
                      ar.created_at AS createdAt
                    FROM analysis_records ar
                    WHERE ar.user_id = :userId
                    ORDER BY ar.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM analysis_records ar
                    WHERE ar.user_id = :userId
                    """,
            nativeQuery = true
    )
    Page<AnalysisHistoryListProjection> pageSummaryByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(
            value = """
                    SELECT
                      ar.id AS id,
                      ar.user_id AS userId,
                      ar.username AS username,
                      ar.filename AS filename,
                      ar.target_role AS targetRole,
                      ar.score AS score,
                      ar.coverage AS coverage,
                      ar.optimized_summary AS optimizedSummary,
                      ar.model_used AS modelUsed,
                      TRIM(REPLACE(REPLACE(SUBSTRING(COALESCE(ar.resume_text, ''), 1, 300), '\r', ' '), '\n', ' ')) AS resumePreview,
                      TRIM(REPLACE(REPLACE(SUBSTRING(COALESCE(ar.jd_text, ''), 1, 240), '\r', ' '), '\n', ' ')) AS jdPreview,
                      ar.created_at AS createdAt
                    FROM analysis_records ar
                    WHERE (:username = '' OR LOWER(ar.username) LIKE CONCAT('%', LOWER(:username), '%'))
                    ORDER BY ar.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM analysis_records ar
                    WHERE (:username = '' OR LOWER(ar.username) LIKE CONCAT('%', LOWER(:username), '%'))
                    """,
            nativeQuery = true
    )
    Page<AnalysisHistoryListProjection> pageSummaryForAdmin(@Param("username") String username, Pageable pageable);
}
