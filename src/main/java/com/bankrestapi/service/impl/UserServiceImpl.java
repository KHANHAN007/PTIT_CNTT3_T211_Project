package com.bankrestapi.service.impl;

import com.bankrestapi.service.*;

import com.bankrestapi.dto.UserDtos.*;
import com.bankrestapi.exception.BusinessException;
import com.bankrestapi.model.*;
import com.bankrestapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    
    @Override
    public UserResponse register(CreateRequest request) {
        Role role = request.role() == null ? Role.CUSTOMER : request.role();
        if (userRepository.existsByUsernameOrEmailOrPhone(request.username(), request.email(), request.phone())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Username, email or phone already exists");
        }
        User user = userRepository.save(User.builder()
                .username(request.username()).password(passwordEncoder.encode(request.password()))
                .email(request.email()).fullName(request.fullName()).phone(request.phone())
                .role(role).enabled(true).kyc(false).build());
        if (role == Role.CUSTOMER) {
            accountRepository.save(Account.builder()
                    .accountNumber(String.format("RB%010d", Math.abs(random.nextLong()) % 10_000_000_000L))
                    .owner(user).balance(BigDecimal.ZERO).active(false).build());
        }
        return map(user);
    }

    
    @Override
    public UserResponse registerCustomer(CreateRequest request) {
        return register(new CreateRequest(request.username(), request.password(), request.email(),
                request.fullName(), request.phone(), Role.CUSTOMER));
    }

    @Transactional(readOnly = true)
    
    @Override
    public Page<UserResponse> list(String search, Pageable pageable) {
        return userRepository.projectUsers(search == null ? "" : search, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public UserResponse findById(Long id) {
        return map(get(id));
    }

    @Transactional(readOnly = true)
    @Override
    public UserResponse findByUsername(String username) {
        return map(getByUsername(username));
    }

    @Transactional(readOnly = true)
    @Override
    public UserResponse findByEmail(String email) {
        return map(userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found")));
    }

    @Transactional
    
    @Override
    public UserResponse update(Long id, UpdateRequest request) {
        User user = get(id);
        if (request.email() != null) user.setEmail(request.email());
        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.role() != null) user.setRole(request.role());
        if (request.enabled() != null) user.setEnabled(request.enabled());
        return map(user);
    }

    @Transactional
    
    @Override
    public UserResponse staffUpdate(Long id, UpdateRequest request) {
        return update(id, new UpdateRequest(request.email(), request.fullName(), request.phone(), null, request.enabled()));
    }

    @Transactional
    
    @Override
    public void delete(Long id) {
        User user = get(id);
        user.setEnabled(false);
    }

    @Transactional(readOnly = true)
    @Override
    public User get(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserResponse map(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), u.getPhone(),
                u.getRole(), u.isEnabled(), u.isKyc(), u.getCreatedAt());
    }
}
