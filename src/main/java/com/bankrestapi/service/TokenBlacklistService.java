package com.bankrestapi.service;

import java.time.Instant;

public interface TokenBlacklistService {
    void revoke(String token, Instant expiresAt);
    boolean isRevoked(String token);
}
