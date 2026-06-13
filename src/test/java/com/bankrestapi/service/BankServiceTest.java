package com.bankrestapi.service;

import com.bankrestapi.dto.BankDtos.*;
import com.bankrestapi.exception.BusinessException;
import com.bankrestapi.model.*;
import com.bankrestapi.repository.*;
import com.bankrestapi.service.impl.BankServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankServiceTest {
    @Mock AccountRepository accounts;
    @Mock BankTransactionRepository transactions;
    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;
    @Mock LedgerEntryRepository ledger;
    @Mock TransferApprovalRepository approvals;
    @Mock FraudDetectionService fraud;
    @Mock OtpService otp;
    @Mock InterbankGateway interbank;
    @Mock UserService userService;
    @Mock NotificationService notifications;
    @InjectMocks BankServiceImpl service;

    User owner;
    Account account;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("customer").role(Role.CUSTOMER).kyc(true).build();
        account = Account.builder().id(10L).accountNumber("RB1").owner(owner).balance(BigDecimal.ZERO)
                .currency("VND").active(true).build();
        ReflectionTestUtils.setField(service, "singleLimit", new BigDecimal("100000000"));
        ReflectionTestUtils.setField(service, "dailyLimit", new BigDecimal("500000000"));
        ReflectionTestUtils.setField(service, "approvalThreshold", new BigDecimal("50000000"));
    }

    @Test
    void createAccountStartsAtZeroAndUsesOwnerKycStatus() {
        when(userService.get(1L)).thenReturn(owner);
        when(accounts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AccountDetailResponse response = service.createAccount(new AccountCreateRequest(1L, "USD"));

        assertEquals(BigDecimal.ZERO, response.balance());
        assertEquals("USD", response.currency());
        assertTrue(response.active());
    }

    @Test
    void closeAccountRejectsNonZeroBalance() {
        account.setBalance(BigDecimal.ONE);
        when(accounts.findById(10L)).thenReturn(Optional.of(account));

        assertThrows(BusinessException.class, () -> service.closeAccount(10L));
        verify(accounts, never()).delete(any());
    }

    @Test
    void closeAccountPreservesRecordAndDeactivatesIt() {
        when(accounts.findById(10L)).thenReturn(Optional.of(account));

        service.closeAccount(10L);

        assertFalse(account.isActive());
        verify(accounts, never()).delete(any());
    }

    @Test
    void balanceRejectsAccountOwnedByAnotherUser() {
        when(accounts.findById(10L)).thenReturn(Optional.of(account));
        assertThrows(BusinessException.class, () -> service.balance(10L, "other"));
    }

    @Test
    void interbankTransferDoesNotRequireInternalTargetAccount() {
        account.setBalance(new BigDecimal("1000"));
        owner.setPin("encoded-pin");
        when(transactions.findByIdempotencyKeyAndFromAccountOwnerUsername("key", "customer"))
                .thenReturn(Optional.empty());
        when(accounts.lockAllByIds(List.of(10L))).thenReturn(List.of(account));
        when(transactions.sumCompletedTransfersSince(eq(10L), any())).thenReturn(BigDecimal.ZERO);
        when(fraud.score(any(), any(), eq(TransactionType.INTERBANK))).thenReturn(20);
        when(encoder.matches("1234", "encoded-pin")).thenReturn(true);
        when(transactions.save(any())).thenAnswer(invocation -> {
            BankTransaction transaction = invocation.getArgument(0);
            transaction.setId(1L);
            return transaction;
        });
        when(interbank.send(any())).thenReturn(new InterbankGateway.Result(true, false, "accepted"));

        TransferResponse response = service.transfer(new TransferRequest(10L, null, new BigDecimal("100"),
                TransactionType.INTERBANK, "External transfer", "1234", "challenge", "123456", "key",
                "VCB", "0123456789"), "customer");

        assertEquals("0123456789", response.toAccount());
        assertEquals("COMPLETED", response.status());
        assertEquals(new BigDecimal("900"), account.getBalance());
    }

    @Test
    void setupPinHashesFirstTransactionPin() {
        when(users.findByUsername("customer")).thenReturn(Optional.of(owner));
        when(encoder.encode("5678")).thenReturn("encoded-pin");

        service.setupPin("customer", new SetupPinRequest("5678"));

        assertEquals("encoded-pin", owner.getPin());
    }

    @Test
    void setupPinRejectsAlreadyConfiguredPin() {
        owner.setPin("encoded-pin");
        when(users.findByUsername("customer")).thenReturn(Optional.of(owner));

        assertThrows(BusinessException.class, () -> service.setupPin("customer", new SetupPinRequest("5678")));
    }

    @Test
    void depositCreditsActiveAccountAndWritesLedger() {
        when(accounts.lockAllByIds(List.of(10L))).thenReturn(List.of(account));
        when(transactions.save(any())).thenAnswer(invocation -> {
            BankTransaction transaction = invocation.getArgument(0);
            transaction.setId(99L);
            return transaction;
        });

        TransferResponse response = service.deposit(10L, new DepositRequest(new BigDecimal("250000"), "Cash at counter"), "staff");

        assertEquals(new BigDecimal("250000"), account.getBalance());
        assertEquals(TransactionType.DEPOSIT, response.type());
        assertEquals("CASH", response.fromAccount());
        assertEquals("RB1", response.toAccount());
        assertEquals("COMPLETED", response.status());
        verify(ledger).save(argThat(entry ->
                entry.getAccount().equals(account)
                        && entry.getDirection() == LedgerDirection.CREDIT
                        && entry.getAmount().compareTo(new BigDecimal("250000")) == 0
                        && entry.getBalanceAfter().compareTo(new BigDecimal("250000")) == 0));
        verify(notifications).send(eq(owner.getEmail()), eq("Rikkei Bank deposit completed"), contains("DEP-"));
    }

    @Test
    void depositRejectsInactiveAccount() {
        account.setActive(false);
        when(accounts.lockAllByIds(List.of(10L))).thenReturn(List.of(account));

        assertThrows(BusinessException.class,
                () -> service.deposit(10L, new DepositRequest(new BigDecimal("1000"), null), "admin"));
        verify(transactions, never()).save(any());
    }
}
