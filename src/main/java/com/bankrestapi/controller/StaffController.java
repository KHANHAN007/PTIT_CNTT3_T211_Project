package com.bankrestapi.controller;

import com.bankrestapi.audit.AuditedAction;
import com.bankrestapi.dto.*;
import com.bankrestapi.dto.BankDtos.*;
import com.bankrestapi.service.*;
import com.bankrestapi.dto.UserDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {
    private final KycService kycService;
    private final BankService bankService;
    private final UserService userService;

    @GetMapping("/users")
    public ApiResponse<Page<UserResponse>> users(@RequestParam(defaultValue = "") String search, Pageable pageable) {
        return ApiResponse.ok("Users retrieved", userService.list(search, pageable));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<UserResponse> findUserById(@PathVariable Long id) {
        return ApiResponse.ok("User retrieved", userService.findById(id));
    }

    @GetMapping("/users/by-username/{username}")
    public ApiResponse<UserResponse> findUserByUsername(@PathVariable String username) {
        return ApiResponse.ok("User retrieved", userService.findByUsername(username));
    }

    @GetMapping("/users/by-email")
    public ApiResponse<UserResponse> findUserByEmail(@RequestParam String email) {
        return ApiResponse.ok("User retrieved", userService.findByEmail(email));
    }

    @PutMapping("/users/{id}")
    @AuditedAction("STAFF_USER_UPDATE")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateRequest request) {
        return ApiResponse.ok("User updated", userService.staffUpdate(id, request));
    }

    @GetMapping("/kyc/pending")
    public ApiResponse<Page<KycResponse>> pending(Pageable pageable) {
        return ApiResponse.ok("Pending KYC retrieved", kycService.pending(pageable));
    }

    @PutMapping("/kyc/{id}/decision")
    @AuditedAction("KYC_DECISION")
    public ApiResponse<KycResponse> decide(@PathVariable Long id, @Valid @RequestBody KycDecision request,
                                           Authentication authentication) {
        return ApiResponse.ok("KYC reviewed", kycService.decide(id, request, authentication.getName()));
    }

    @PutMapping("/transfers/{id}/approval")
    @AuditedAction("TRANSFER_APPROVAL")
    public ApiResponse<TransferResponse> approve(@PathVariable Long id, @Valid @RequestBody ApprovalRequest request,
                                                  Authentication authentication) {
        return ApiResponse.ok("Transfer reviewed", bankService.approve(id, request, authentication.getName()));
    }

    @PostMapping("/accounts/{id}/deposit")
    @AuditedAction("ACCOUNT_DEPOSIT")
    public ApiResponse<TransferResponse> deposit(@PathVariable Long id,
                                                  @Valid @RequestBody DepositRequest request,
                                                  Authentication authentication) {
        return ApiResponse.ok("Deposit completed", bankService.deposit(id, request, authentication.getName()));
    }
}
