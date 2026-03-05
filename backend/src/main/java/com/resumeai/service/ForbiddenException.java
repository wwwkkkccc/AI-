package com.resumeai.service;

/**
 * 权限不足异常（对应 HTTP 403）。
 * 当用户已认证但无权执行目标操作时抛出。
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
