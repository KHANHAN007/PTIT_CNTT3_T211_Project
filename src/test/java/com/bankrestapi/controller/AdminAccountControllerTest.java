package com.bankrestapi.controller;

import com.bankrestapi.dto.BankDtos.*;
import com.bankrestapi.service.BankService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminAccountControllerTest {
    BankService bank = mock(BankService.class);
    AdminAccountController controller = new AdminAccountController(bank);

    @Test
    void createDelegatesToBankService() {
        AccountCreateRequest request = new AccountCreateRequest(1L, "VND");
        AccountDetailResponse response = new AccountDetailResponse(2L, "RB1", 1L, "customer",
                BigDecimal.ZERO, "VND", false, LocalDateTime.now());
        when(bank.createAccount(request)).thenReturn(response);
        assertEquals("RB1", controller.create(request).data().accountNumber());
    }

    @Test
    void closeDelegatesToBankService() {
        controller.close(2L);
        verify(bank).closeAccount(2L);
    }

    @Test
    void depositDelegatesToBankService() {
        Authentication authentication = mock(Authentication.class);
        DepositRequest request = new DepositRequest(new BigDecimal("100000"), "Cash deposit");
        TransferResponse response = new TransferResponse(3L, "DEP-1", new BigDecimal("100000"),
                "CASH", "RB1", com.bankrestapi.model.TransactionType.DEPOSIT,
                "COMPLETED", 0, LocalDateTime.now());
        when(authentication.getName()).thenReturn("admin");
        when(bank.deposit(2L, request, "admin")).thenReturn(response);

        assertEquals("DEP-1", controller.deposit(2L, request, authentication).data().reference());
    }
}
