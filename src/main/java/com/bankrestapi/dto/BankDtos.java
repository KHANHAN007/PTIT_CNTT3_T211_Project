package com.bankrestapi.dto;

import com.bankrestapi.model.KycStatus;
import com.bankrestapi.model.TransactionType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class BankDtos {
    private BankDtos() {}

    public record AccountResponse(Long id, String accountNumber, BigDecimal balance, boolean active) {}
    public record AccountStatusRequest(@NotNull Boolean active) {}
    public record AccountCreateRequest(@NotNull Long ownerId, @Pattern(regexp = "[A-Z]{3}") String currency) {}
    public record AccountUpdateRequest(@Pattern(regexp = "[A-Z]{3}") String currency, Boolean active) {}
    public record DepositRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal amount,
                                 @Size(max = 255) String description) {}
    public record AccountDetailResponse(Long id, String accountNumber, Long ownerId, String ownerUsername,
                                        BigDecimal balance, String currency, boolean active, LocalDateTime createdAt) {}
    public record TransferRequest(
            @NotNull Long sourceAccountId,
            Long targetAccountId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            TransactionType type,
            @Size(max = 255) String description,
            String pin,
            String otpChallengeId,
            String otp,
            String idempotencyKey,
            String externalBankCode,
            String externalAccountNumber) {}
    public record TransferResponse(Long id, String reference, BigDecimal amount, String fromAccount,
                                   String toAccount, TransactionType type, String status,
                                   Integer riskScore, LocalDateTime createdAt) {}
    public record StatementResponse(Long id, String reference, String direction, BigDecimal amount,
                                    String counterpartyAccount, String description, LocalDateTime createdAt) {}
    public record KycResponse(Long id, Long userId, String username, String documentUrl,
                              KycStatus status, String rejectionReason, LocalDateTime updatedAt) {}
    public record KycDecision(@NotNull KycStatus status, String reason) {}
    public record SetupPinRequest(@NotBlank @Size(min = 4, max = 12) String pin) {}
    public record ChangePinRequest(@NotBlank String currentPin, @NotBlank @Size(min = 4, max = 12) String newPin) {}
    public record OtpResponse(String challengeId, long expiresInSeconds) {}
    public record ApprovalRequest(@NotNull com.bankrestapi.model.ApprovalDecision decision, String reason) {}
}
