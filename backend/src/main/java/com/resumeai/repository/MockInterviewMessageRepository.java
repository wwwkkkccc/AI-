package com.resumeai.repository;

import com.resumeai.model.MockInterviewMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 模拟面试消息数据访问接口
 */
public interface MockInterviewMessageRepository extends JpaRepository<MockInterviewMessage, Long> {
    List<MockInterviewMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    int countBySessionId(Long sessionId);
}
