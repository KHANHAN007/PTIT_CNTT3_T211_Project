package com.bankrestapi.service;

import java.time.Duration;

public interface RateLimitService {
    void check(String bucket, String subject, int maxRequests, Duration window);
}
