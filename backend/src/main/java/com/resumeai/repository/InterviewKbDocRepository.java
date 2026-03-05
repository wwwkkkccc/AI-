package com.resumeai.repository;

import com.resumeai.model.InterviewKbDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 面试知识库文档数据访问接口，提供interview_kb_docs表的分页查询操作
 */
public interface InterviewKbDocRepository extends JpaRepository<InterviewKbDoc, Long> {
    // 分页查询所有文档，按ID倒序
    Page<InterviewKbDoc> findAllByOrderByIdDesc(Pageable pageable);
}
