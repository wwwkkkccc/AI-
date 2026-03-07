package com.resumeai.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * API 限流服务，使用 Redis 实现滑动窗口限流
 */
@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 检查是否超过限流阈值
     * @param userId 用户 ID
     * @param endpoint 端点标识（如 "analyze", "generate", "chat"）
     * @param maxRequests 时间窗口内最大请求数
     * @param windowSeconds 时间窗口（秒）
     * @param isAdmin 是否为管理员（管理员豁免限流）
     * @return true 表示允许请求，false 表示超限
     */
    public boolean checkRateLimit(Long userId, String endpoint, int maxRequests, int windowSeconds, boolean isAdmin) {
        if (isAdmin) {
            return true;
        }

        String key = "rate:" + userId + ":" + endpoint;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                count = 0L;
            }

            if (count == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }

            return count <= maxRequests;
        } catch (Exception ex) {
            return true;
        }
    }
}
