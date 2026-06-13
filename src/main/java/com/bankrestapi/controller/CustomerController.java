package com.bankrestapi.controller;

import com.bankrestapi.audit.AuditedAction;
import com.bankrestapi.dto.*;
import com.bankrestapi.dto.BankDtos.*;
import com.bankrestapi.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerController {
    private final BankService bankService;
    private final KycService kycService;
    private final RateLimitService rateLimitService;

    @GetMapping("/accounts")
    public ApiResponse<List<AccountResponse>> accounts(Authentication authentication) {
        return ApiResponse.ok("Accounts retrieved", bankService.accounts(authentication.getName()));
    }

    @GetMapping("/accounts/{accountId}/balance")
    public ApiResponse<AccountResponse> balance(@PathVariable Long accountId, Authentication authentication) {
        return ApiResponse.ok("Balance retrieved", bankService.balance(accountId, authentication.getName()));
    }

    @PostMapping("/transfers/otp")
    public ApiResponse<OtpResponse> otp(Authentication authentication) {
        OtpService.Challenge challenge = bankService.requestTransferOtp(authentication.getName());
        return ApiResponse.ok("OTP challenge created",
                new OtpResponse(challenge.id(), challenge.expiresInSeconds()));
    }

    @PostMapping("/transfers")
    @AuditedAction("TRANSFER")
    public ApiResponse<TransferResponse> transfer(@Valid @RequestBody TransferRequest request, Authentication auth,
                                                   HttpServletRequest servletRequest) {
        rateLimitService.check("transfer", auth.getName() + ":" + servletRequest.getRemoteAddr(), 30, Duration.ofMinutes(1));
        return ApiResponse.ok("Transfer processed", bankService.transfer(request, auth.getName()));
    }

    @GetMapping("/accounts/{accountId}/statement")
    public ApiResponse<Page<StatementResponse>> statement(@PathVariable Long accountId, Pageable pageable,
                                                           Authentication auth) {
        return ApiResponse.ok("Statement retrieved", bankService.statement(accountId, auth.getName(), pageable));
    }

    @PostMapping("/pin")
    @AuditedAction("PIN_SETUP")
    public ApiResponse<Void> setupPin(@Valid @RequestBody SetupPinRequest request, Authentication auth) {
        bankService.setupPin(auth.getName(), request);
        return ApiResponse.ok("PIN configured", null);
    }

    @PutMapping("/pin")
    @AuditedAction("PIN_CHANGE")
    public ApiResponse<Void> changePin(@Valid @RequestBody ChangePinRequest request, Authentication auth) {
        bankService.changePin(auth.getName(), request);
        return ApiResponse.ok("PIN changed", null);
    }

    @PostMapping(value = "/kyc", consumes = "multipart/form-data")
    public ApiResponse<KycResponse> uploadKyc(@RequestPart MultipartFile document, @RequestParam String idNumber,
                                              Authentication auth) {
        return ApiResponse.ok("KYC uploaded", kycService.upload(auth.getName(), document, idNumber));
    }
}
