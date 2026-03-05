package com.resumeai.dto;

/**
 * 用户信息响应 DTO，用于返回当前登录用户的基本信息
 */
public class UserInfoResponse {
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
}
