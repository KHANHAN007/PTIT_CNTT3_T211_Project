package com.bankrestapi.dto;

import com.bankrestapi.model.Role;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class UserDtos {
    private UserDtos() {}

    public record CreateRequest(
            @NotBlank @Size(min = 4, max = 50) String username,
            @NotBlank @Size(min = 12) String password,
            @NotBlank @Email String email,
            @NotBlank String fullName,
            @NotBlank String phone,
            Role role) {}

    public record UpdateRequest(
            @Email String email,
            String fullName,
            String phone,
            Role role,
            Boolean enabled) {}

    public record UserResponse(
            Long id, String username, String email, String fullName, String phone,
            Role role, boolean enabled, boolean kyc, LocalDateTime createdAt) {}
}
