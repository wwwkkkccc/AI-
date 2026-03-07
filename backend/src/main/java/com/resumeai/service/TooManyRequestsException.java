package com.resumeai.service;

/**
 * 请求频率超限异常，对应 HTTP 429 Too Many Requests
 */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
