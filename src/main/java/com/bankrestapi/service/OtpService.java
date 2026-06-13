package com.bankrestapi.service;

public interface OtpService {
    Challenge create(String username);
    void verify(String challengeId, String otp, String username);

    record Challenge(String id, String otp, long expiresInSeconds) {}
}
