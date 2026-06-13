package com.bankrestapi.service;

import com.bankrestapi.dto.BankDtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BankService {
    List<AccountResponse> accounts(String username);
    AccountResponse balance(Long accountId, String username);
    Page<AccountResponse> allAccounts(Pageable pageable);
    AccountDetailResponse account(Long id);
    AccountDetailResponse createAccount(AccountCreateRequest request);
    AccountDetailResponse updateAccount(Long id, AccountUpdateRequest request);
    void closeAccount(Long id);
    AccountResponse setAccountStatus(Long id, boolean active);
    TransferResponse deposit(Long accountId, DepositRequest request, String actorUsername);
    TransferResponse transfer(TransferRequest request, String username);
    TransferResponse approve(Long transactionId, ApprovalRequest request, String checkerUsername);
    OtpService.Challenge requestTransferOtp(String username);
    void retryInterbank(Long transactionId);
    Page<StatementResponse> statement(Long accountId, String username, Pageable pageable);
    void setupPin(String username, SetupPinRequest request);
    void changePin(String username, ChangePinRequest request);
}
