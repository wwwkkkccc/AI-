package com.resumeai.dto;

import java.time.Instant;

/**
 * 管理员用户列表项 DTO，用于管理员查询用户列表时返回的单个用户信息
 */
public class AdminUserItem {
    /** 用户 ID */
    private Long id;
    /** 用户名 */
    private String username;
    /** 用户角色（如 ADMIN、USER） */
    private String role;
    /** 是否为 VIP 用户 */
    private Boolean vip;
    /** 是否被拉黑 */
    private Boolean blacklisted;
    /** 账号创建时间 */
    private Instant createdAt;
    /** 最后登录时间 */
    private Instant lastLoginAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getVip() {
        return vip;
    }

    public void setVip(Boolean vip) {
        this.vip = vip;
    }

    public Boolean getBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(Boolean blacklisted) {
        this.blacklisted = blacklisted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
