package com.resumeai.dto;

import java.time.Instant;

/**
 * 用户认证响应 DTO，用于登录/注册成功后返回令牌和用户信息
 */
public class AuthResponse {
    /** JWT 令牌 */
    private String token;
    /** 令牌过期时间 */
    private Instant expiresAt;
    /** 当前登录用户信息 */
    private UserInfoResponse user;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UserInfoResponse getUser() {
        return user;
    }

    public void setUser(UserInfoResponse user) {
        this.user = user;
    }
}
