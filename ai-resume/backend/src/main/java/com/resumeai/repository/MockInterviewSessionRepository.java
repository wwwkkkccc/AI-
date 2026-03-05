package com.resumeai.repository;

import com.resumeai.model.MockInterviewSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 模拟面试会话仓库 */
public interface MockInterviewSessionRepository extends JpaRepository<MockInterviewSession, Long> {
    List<MockInterviewSession> findByUserIdOrderByIdDesc(Long userId);
}
