package com.bankrestapi.service.impl;

import com.bankrestapi.service.*;

import com.bankrestapi.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;
import com.bankrestapi.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private final StringRedisTemplate redisTemplate;
    private final HashingService hashingService;
    private final UserRepository users;
    private final NotificationService notifications;
    private final SecureRandom random = new SecureRandom();
    @Value("${app.security.otp-expiration-seconds}")
    private long expirationSeconds;

    
    @Override
    public Challenge create(String username) {
        String id = UUID.randomUUID().toString();
        String otp = String.format("%06d", random.nextInt(1_000_000));
        redisTemplate.opsForValue().set("bank:otp:" + id, username + ":" + hashingService.sha256(otp),
                Duration.ofSeconds(expirationSeconds));
        users.findByUsername(username).ifPresent(user ->
                notifications.send(user.getEmail(), "Rikkei Bank transfer OTP",
                        "Your transfer OTP is " + otp + ". It expires in " + expirationSeconds + " seconds."));
        return new Challenge(id, otp, expirationSeconds);
    }

    
    @Override
    public void verify(String challengeId, String otp, String username) {
        String key = "bank:otp:" + challengeId;
        String expected = redisTemplate.opsForValue().get(key);
        if (expected == null || !expected.equals(username + ":" + hashingService.sha256(otp))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Invalid or expired OTP");
        }
        redisTemplate.delete(key);
    }

}
