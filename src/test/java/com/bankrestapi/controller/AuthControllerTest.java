package com.bankrestapi.controller;

import com.bankrestapi.dto.AuthDtos.*;
import com.bankrestapi.service.AuthService;
import com.bankrestapi.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {
    AuthService auth = mock(AuthService.class);
    RateLimitService rateLimit = mock(RateLimitService.class);
    AuthController controller = new AuthController(auth, rateLimit);

    @Test
    void loginReturnsIssuedTokens() {
        LoginRequest request = new LoginRequest("admin", "password");
        TokenResponse tokens = new TokenResponse("access", "refresh", "Bearer", 300);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(auth.login(request)).thenReturn(tokens);

        assertEquals("access", controller.login(request, servletRequest).data().accessToken());
        verify(rateLimit).check(eq("login"), eq("127.0.0.1"), eq(10), any());
    }

    @Test
    void logoutPassesAccessAndRefreshTokens() {
        controller.logout("Bearer access", new LogoutRequest("refresh"));
        verify(auth).logout("access", "refresh");
    }
}
