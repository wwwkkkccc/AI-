package com.resumeai.repository;

import com.resumeai.model.MockInterviewMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 模拟面试消息仓库 */
public interface MockInterviewMessageRepository extends JpaRepository<MockInterviewMessage, Long> {
    List<MockInterviewMessage> findBySessionIdOrderByIdAsc(Long sessionId);
    int countBySessionIdAndRole(Long sessionId, String role);
}
