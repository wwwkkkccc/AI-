package com.resumeai.repository;

import com.resumeai.model.UserSession;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByToken(String token);
    Optional<UserSession> findByTokenAndExpiresAtAfter(String token, Instant now);
    long deleteByExpiresAtBefore(Instant now);
    long deleteByToken(String token);
}
