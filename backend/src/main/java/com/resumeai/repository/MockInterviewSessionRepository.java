package com.resumeai.repository;

import com.resumeai.model.MockInterviewSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 模拟面试会话数据访问接口
 */
public interface MockInterviewSessionRepository extends JpaRepository<MockInterviewSession, Long> {
    Page<MockInterviewSession> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
