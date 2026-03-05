package com.resumeai.repository;

import com.resumeai.model.AnalysisRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
