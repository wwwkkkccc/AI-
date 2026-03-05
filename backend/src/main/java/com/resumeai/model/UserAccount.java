package com.resumeai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 用户账户表，存储用户的基本信息、角色权限及账户状态
 */
@Entity
@Table(name = "user_accounts")
public class UserAccount {
    // 自增主键ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 用户名（唯一）
    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    // 密码哈希值
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // 用户角色（如 admin、user）
    @Column(name = "role", nullable = false, length = 16)
    private String role;

    // 是否为VIP用户
    @Column(name = "vip", nullable = false)
    private Boolean vip = false;

    // 是否被拉黑
    @Column(name = "blacklisted", nullable = false)
    private Boolean blacklisted = false;

    // 注册时的IP地址
    @Column(name = "register_ip", nullable = false, length = 64)
    private String registerIp;

    // 注册日期
    @Column(name = "register_date", nullable = false)
    private LocalDate registerDate;

    // 账户创建时间
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // 最后登录时间
    @Column(name = "last_login_at")
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public String getRegisterIp() {
        return registerIp;
    }

    public void setRegisterIp(String registerIp) {
        this.registerIp = registerIp;
    }

    public LocalDate getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(LocalDate registerDate) {
        this.registerDate = registerDate;
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
