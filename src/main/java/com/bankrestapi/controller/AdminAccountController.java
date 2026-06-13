package com.bankrestapi.controller;

import com.bankrestapi.audit.AuditedAction;
import com.bankrestapi.dto.ApiResponse;
import com.bankrestapi.dto.BankDtos.*;
import com.bankrestapi.service.BankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {
    private final BankService bankService;

    @GetMapping
    public ApiResponse<Page<AccountResponse>> accounts(Pageable pageable) {
        return ApiResponse.ok("Accounts retrieved", bankService.allAccounts(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<AccountDetailResponse> account(@PathVariable Long id) {
        return ApiResponse.ok("Account retrieved", bankService.account(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AuditedAction("ACCOUNT_CREATE")
    public ApiResponse<AccountDetailResponse> create(@Valid @RequestBody AccountCreateRequest request) {
        return ApiResponse.ok("Account created", bankService.createAccount(request));
    }

    @PutMapping("/{id}")
    @AuditedAction("ACCOUNT_UPDATE")
    public ApiResponse<AccountDetailResponse> update(@PathVariable Long id, @Valid @RequestBody AccountUpdateRequest request) {
        return ApiResponse.ok("Account updated", bankService.updateAccount(id, request));
    }

    @PutMapping("/{id}/status")
    @AuditedAction("ACCOUNT_STATUS_CHANGE")
    public ApiResponse<AccountResponse> status(@PathVariable Long id, @Valid @RequestBody AccountStatusRequest request) {
        return ApiResponse.ok("Account status updated", bankService.setAccountStatus(id, request.active()));
    }

    @PostMapping("/{id}/deposit")
    @AuditedAction("ACCOUNT_DEPOSIT")
    public ApiResponse<TransferResponse> deposit(@PathVariable Long id,
                                                  @Valid @RequestBody DepositRequest request,
                                                  Authentication authentication) {
        return ApiResponse.ok("Deposit completed", bankService.deposit(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AuditedAction("ACCOUNT_CLOSE")
    public void close(@PathVariable Long id) {
        bankService.closeAccount(id);
    }
}
