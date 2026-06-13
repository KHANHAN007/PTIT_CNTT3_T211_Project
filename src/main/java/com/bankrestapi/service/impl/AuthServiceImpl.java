package com.bankrestapi.service.impl;

import com.bankrestapi.service.*;

import com.bankrestapi.dto.AuthDtos.*;
import com.bankrestapi.exception.BusinessException;
import com.bankrestapi.model.*;
import com.bankrestapi.repository.*;
import com.bankrestapi.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtService jwtService;
    private final HashingService hashingService;
    private final RefreshTokenService refreshTokenService;
    private final StringRedisTemplate redisTemplate;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    @Value("${app.security.password-reset-expiration-seconds}") private long resetExpirationSeconds;

    @Transactional
    
    @Override
    public TokenResponse login(LoginRequest request) {
        User user = findUser(request.username());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException ex) {
            loginAttemptService.failed(request.username());
            throw ex;
        }
        loginAttemptService.succeeded(request.username());
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    
    @Override
    public TokenResponse refresh(String token) {
        RefreshToken saved = refreshTokenRepository.findByTokenHashForUpdate(hashingService.sha256(token))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (saved.isRevoked() || saved.getExpiresAt().isBefore(Instant.now())) {
            revokeFamily(saved.getFamilyId());
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Refresh token expired, revoked or reused");
        }
        saved.setRevoked(true);
        return issueTokens(saved.getUser(), saved.getFamilyId());
    }

    @Transactional
    
    @Override
    public void logout(String accessToken, String refreshToken) {
        try {
            var accessClaims = jwtService.claims(accessToken);
            if (!"access".equals(accessClaims.get("type"))) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid access token");
            }
            tokenBlacklistService.revoke(accessToken, accessClaims.getExpiration().toInstant());
            if (refreshToken != null && !refreshToken.isBlank()) {
                refreshTokenRepository.findByTokenHashForUpdate(hashingService.sha256(refreshToken))
                        .ifPresent(token -> revokeFamily(token.getFamilyId()));
            }
            refreshTokenRepository.findByUserUsername(accessClaims.getSubject())
                    .forEach(token -> token.setRevoked(true));
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid access token");
        }
    }

    private TokenResponse issueTokens(User user) {
        return issueTokens(user, UUID.randomUUID().toString());
    }

    private TokenResponse issueTokens(User user, String familyId) {
        String access = jwtService.createAccessToken(user);
        String refresh = refreshTokenService.generate();
        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(hashingService.sha256(refresh)).familyId(familyId).user(user)
                .expiresAt(refreshTokenService.expiration())
                .revoked(false).build());
        return new TokenResponse(access, refresh, "Bearer", jwtService.getAccessExpirationSeconds());
    }

    
    @Override
    public void forgotPassword(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set("bank:password-reset:" + hashingService.sha256(token),
                    user.getUsername(), Duration.ofSeconds(resetExpirationSeconds));
            notificationService.send(user.getEmail(), "Rikkei Bank password reset", "Reset token: " + token);
        });
    }

    @Transactional
    
    @Override
    public void resetPassword(String token, String newPassword) {
        String key = "bank:password-reset:" + hashingService.sha256(token);
        String username = redisTemplate.opsForValue().get(key);
        if (username == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token");
        User user = findUser(username);
        user.setPassword(passwordEncoder.encode(newPassword));
        redisTemplate.delete(key);
        refreshTokenRepository.findAll().stream().filter(t -> t.getUser().getId().equals(user.getId()))
                .forEach(t -> t.setRevoked(true));
    }

    private void revokeFamily(String familyId) {
        refreshTokenRepository.findByFamilyId(familyId).forEach(t -> t.setRevoked(true));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    }
}
