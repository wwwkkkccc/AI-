package com.resumeai.service;

import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Lightweight fixed-window rate limiter backed by Redis counters.
 */
@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks and updates endpoint quota for one user.
     * Admin requests bypass the limit.
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
            // Fail-open to avoid availability issues caused by Redis outages.
            return true;
        }
    }
}
