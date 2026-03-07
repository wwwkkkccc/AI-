package com.resumeai.repository;

import com.resumeai.model.InterviewKbItem;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 面试知识库题目数据访问接口，提供interview_kb_items表的查询和删除操作
 */
public interface InterviewKbItemRepository extends JpaRepository<InterviewKbItem, Long> {
    // 查询最新的3000条面试题，按ID倒序
    List<InterviewKbItem> findTop3000ByOrderByIdDesc();

    // 根据文档ID分页查询面试题，按ID正序
    Page<InterviewKbItem> findByDocIdOrderByIdAsc(Long docId, Pageable pageable);

    // 根据分类ID分页查询面试题，按ID正序
    Page<InterviewKbItem> findByCategoryIdOrderByIdAsc(Long categoryId, Pageable pageable);

    // 根据文档ID删除该文档下的所有面试题
    void deleteByDocId(Long docId);
}
