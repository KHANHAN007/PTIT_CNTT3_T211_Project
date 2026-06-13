package com.bankrestapi.service;

import java.time.Instant;

public interface RefreshTokenService {
    String generate();
    Instant expiration();
}
