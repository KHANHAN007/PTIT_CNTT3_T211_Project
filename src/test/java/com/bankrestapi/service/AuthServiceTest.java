package com.bankrestapi.service;

import com.bankrestapi.model.RefreshToken;
import com.bankrestapi.repository.RefreshTokenRepository;
import com.bankrestapi.repository.UserRepository;
import com.bankrestapi.security.JwtService;
import com.bankrestapi.service.impl.AuthServiceImpl;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock AuthenticationManager authenticationManager;
    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock TokenBlacklistService blacklist;
    @Mock JwtService jwtService;
    @Mock HashingService hashingService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock StringRedisTemplate redis;
    @Mock NotificationService notifications;
    @Mock PasswordEncoder passwordEncoder;
    @Mock LoginAttemptService loginAttempts;
    @InjectMocks AuthServiceImpl service;

    @Test
    void logoutBlacklistsAccessAndRevokesAllUserRefreshTokens() {
        Claims claims = mock(Claims.class);
        RefreshToken first = RefreshToken.builder().revoked(false).build();
        RefreshToken second = RefreshToken.builder().revoked(false).build();
        when(jwtService.claims("access")).thenReturn(claims);
        when(claims.get("type")).thenReturn("access");
        when(claims.getSubject()).thenReturn("customer");
        when(claims.getExpiration()).thenReturn(Date.from(Instant.now().plusSeconds(60)));
        when(refreshTokens.findByUserUsername("customer")).thenReturn(List.of(first, second));

        service.logout("access", null);

        verify(blacklist).revoke(eq("access"), any(Instant.class));
        assert first.isRevoked();
        assert second.isRevoked();
    }

    @Test
    void refreshUsesPessimisticLockLookup() {
        when(hashingService.sha256("missing")).thenReturn("hash");

        try {
            service.refresh("missing");
        } catch (RuntimeException ignored) {
        }

        verify(refreshTokens).findByTokenHashForUpdate("hash");
        verify(refreshTokens, never()).findByTokenHash(anyString());
    }
}
