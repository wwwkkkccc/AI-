package com.resumeai.service;

/**
 * 未认证异常（对应 HTTP 401）。
 * 当请求缺少有效的身份凭证时抛出。
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
