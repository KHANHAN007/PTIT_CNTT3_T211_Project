package com.bankrestapi.service.impl;

import com.bankrestapi.service.*;

import com.bankrestapi.model.User;
import com.bankrestapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {
    private final UserRepository users;
    @Value("${app.security.login-max-attempts}") private int maxAttempts;
    @Value("${app.security.lock-duration-seconds}") private long lockDurationSeconds;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    
    @Override
    public void failed(String username) {
        users.findByUsername(username).ifPresent(user -> {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= maxAttempts) {
                user.setLockedUntil(LocalDateTime.now().plusSeconds(lockDurationSeconds));
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    
    @Override
    public void succeeded(String username) {
        users.findByUsername(username).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        });
    }
}
