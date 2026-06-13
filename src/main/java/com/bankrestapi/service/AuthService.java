package com.bankrestapi.service;

import com.bankrestapi.dto.AuthDtos.LoginRequest;
import com.bankrestapi.dto.AuthDtos.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(String token);
    void logout(String accessToken, String refreshToken);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
}
