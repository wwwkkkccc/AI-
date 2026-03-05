package com.resumeai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 用户会话表，管理用户登录后的会话token及其有效期
 */
@Entity
@Table(name = "user_sessions")
public class UserSession {
    // 自增主键ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 会话token（唯一）
    @Column(name = "token", nullable = false, unique = true, length = 128)
    private String token;

    // 关联的用户ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 会话创建时间
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // 会话过期时间
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // 用户最后活跃时间
    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
