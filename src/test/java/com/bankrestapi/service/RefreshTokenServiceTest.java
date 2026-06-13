package com.bankrestapi.service;

import com.bankrestapi.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenServiceTest {
    private final RefreshTokenServiceImpl service =
            new RefreshTokenServiceImpl("test-encryption-key", 86_400_000);

    @Test
    void generatesEncryptedBase64UrlToken() {
        String token = service.generate();

        assertTrue(token.startsWith("rt_"));
        assertDoesNotThrow(() -> Base64.getUrlDecoder().decode(token.substring(3)));
        assertTrue(token.length() >= 80);
    }

    @Test
    void generatesUniqueTokens() {
        assertNotEquals(service.generate(), service.generate());
    }

    @Test
    void expirationIsInFuture() {
        assertTrue(service.expiration().isAfter(Instant.now()));
    }
}
