package com.bankrestapi.service.impl;

import com.bankrestapi.service.*;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {
    private static final String PREFIX = "bank:token:blacklist:";
    private final StringRedisTemplate redisTemplate;

    
    @Override
    public void revoke(String token, Instant expiresAt) {
        long ttl = Math.max(1, Duration.between(Instant.now(), expiresAt).toSeconds());
        redisTemplate.opsForValue().set(PREFIX + token, "revoked", ttl, TimeUnit.SECONDS);
    }

    
    @Override
    public boolean isRevoked(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }
}
