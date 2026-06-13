package com.bankrestapi.service.impl;

import com.bankrestapi.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private static final String PREFIX = "rt_";
    private static final int TOKEN_BYTES = 32;
    private static final int IV_BYTES = 12;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();
    private final long expirationMs;

    public RefreshTokenServiceImpl(@Value("${app.encryption.key}") String encryptionKey,
                                   @Value("${app.jwt.refresh-expiration-ms}") long expirationMs) {
        this.key = new SecretKeySpec(sha256(encryptionKey), "AES");
        this.expirationMs = expirationMs;
    }

    @Override
    public String generate() {
        try {
            byte[] plaintext = new byte[TOKEN_BYTES];
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(plaintext);
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);
            byte[] token = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, token, 0, iv.length);
            System.arraycopy(ciphertext, 0, token, iv.length, ciphertext.length);
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(token);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not generate encrypted refresh token", ex);
        }
    }

    @Override
    public Instant expiration() {
        return Instant.now().plusMillis(expirationMs);
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
