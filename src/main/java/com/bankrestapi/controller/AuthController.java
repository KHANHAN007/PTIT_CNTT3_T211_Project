package com.bankrestapi.controller;

import com.bankrestapi.audit.AuditedAction;
import com.bankrestapi.dto.ApiResponse;
import com.bankrestapi.dto.AuthDtos.*;
import com.bankrestapi.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RateLimitService rateLimitService;

    @PostMapping("/login")
    @AuditedAction("AUTH_LOGIN")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        rateLimitService.check("login", servletRequest.getRemoteAddr(), 10, Duration.ofMinutes(1));
        return ApiResponse.ok("Login successful", authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok("Token rotated", authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @AuditedAction("AUTH_LOGOUT")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String header,
                                    @RequestBody(required = false) LogoutRequest request) {
        authService.logout(header.substring(7), request == null ? null : request.refreshToken());
        return ApiResponse.ok("Logout successful", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgot(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest servletRequest) {
        rateLimitService.check("forgot-password", servletRequest.getRemoteAddr(), 5, Duration.ofMinutes(10));
        authService.forgotPassword(request.email());
        return ApiResponse.ok("If the email exists, reset instructions were sent", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> reset(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ApiResponse.ok("Password reset successful", null);
    }
}
