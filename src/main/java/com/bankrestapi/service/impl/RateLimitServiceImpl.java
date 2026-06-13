package com.bankrestapi.service.impl;

import com.bankrestapi.service.*;

import com.bankrestapi.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {
    private final StringRedisTemplate redisTemplate;

    
    @Override
    public void check(String bucket, String subject, int maxRequests, Duration window) {
        String key = "bank:rate:" + bucket + ":" + subject;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) redisTemplate.expire(key, window);
        if (count != null && count > maxRequests) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");
        }
    }
}
