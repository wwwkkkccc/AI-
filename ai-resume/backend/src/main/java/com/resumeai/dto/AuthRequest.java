package com.resumeai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户认证请求 DTO，用于登录/注册接口的请求体
 */
public class AuthRequest {
    /** 用户名，长度 4~32 */
    @NotBlank
    @Size(min = 4, max = 32)
    private String username;

    /** 密码，长度 8~64 */
    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
