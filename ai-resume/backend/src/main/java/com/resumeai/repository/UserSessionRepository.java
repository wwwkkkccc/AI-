package com.resumeai.repository;

import com.resumeai.model.UserSession;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 用户会话数据访问接口，提供user_sessions表的查询和清理操作
 */
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    // 根据token查询会话
    Optional<UserSession> findByToken(String token);
    // 根据token查询未过期的有效会话
    Optional<UserSession> findByTokenAndExpiresAtAfter(String token, Instant now);
    // 删除所有已过期的会话，返回删除数量
    long deleteByExpiresAtBefore(Instant now);
    // 根据token删除会话（用于登出），返回删除数量
    long deleteByToken(String token);
}
