package com.bankrestapi.service.impl;

import com.bankrestapi.service.*;

import com.bankrestapi.audit.AuditedTransfer;
import com.bankrestapi.dto.BankDtos.*;
import com.bankrestapi.exception.BusinessException;
import com.bankrestapi.model.*;
import com.bankrestapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BankServiceImpl implements BankService {
    private final AccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LedgerEntryRepository ledgerRepository;
    private final TransferApprovalRepository approvalRepository;
    private final FraudDetectionService fraudDetectionService;
    private final OtpService otpService;
    private final InterbankGateway interbankGateway;
    private final UserService userService;
    private final NotificationService notificationService;
    @Value("${app.transfer.single-limit}") private BigDecimal singleLimit;
    @Value("${app.transfer.daily-limit}") private BigDecimal dailyLimit;
    @Value("${app.transfer.approval-threshold}") private BigDecimal approvalThreshold;

    @Transactional(readOnly = true)
    
    @Override
    public List<AccountResponse> accounts(String username) {
        return accountRepository.findByOwnerUsernameOrderByIdAsc(username).stream().map(this::mapAccount).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public AccountResponse balance(Long accountId, String username) {
        return mapAccount(ownedAccount(accountId, username));
    }

    @Transactional(readOnly = true)
    
    @Override
    public Page<AccountResponse> allAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable).map(this::mapAccount);
    }

    @Transactional(readOnly = true)
    @Override
    public AccountDetailResponse account(Long id) {
        return mapAccountDetail(findAccount(id));
    }

    @Transactional
    @Override
    public AccountDetailResponse createAccount(AccountCreateRequest request) {
        User owner = userService.get(request.ownerId());
        if (owner.getRole() != Role.CUSTOMER) {
            throw new BusinessException(HttpStatus.CONFLICT, "Only customers can own bank accounts");
        }
        Account account = accountRepository.save(Account.builder()
                .accountNumber(generateAccountNumber())
                .owner(owner)
                .balance(BigDecimal.ZERO)
                .currency(request.currency() == null ? "VND" : request.currency())
                .active(owner.isKyc())
                .build());
        return mapAccountDetail(account);
    }

    @Transactional
    @Override
    public AccountDetailResponse updateAccount(Long id, AccountUpdateRequest request) {
        Account account = findAccount(id);
        if (request.currency() != null) account.setCurrency(request.currency());
        if (request.active() != null) account.setActive(request.active());
        return mapAccountDetail(account);
    }

    @Transactional
    @Override
    public void closeAccount(Long id) {
        Account account = findAccount(id);
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "Account balance must be zero before closing");
        }
        account.setActive(false);
    }

    @Transactional
    
    @Override
    public AccountResponse setAccountStatus(Long id, boolean active) {
        Account account = findAccount(id);
        account.setActive(active);
        return mapAccount(account);
    }

    @Transactional
    @Override
    public TransferResponse deposit(Long accountId, DepositRequest request, String actorUsername) {
        Account target = accountRepository.lockAllByIds(List.of(accountId)).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Account not found"));
        if (!target.isActive()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Account is inactive");
        }
        target.setBalance(target.getBalance().add(request.amount()));
        String description = request.description() == null || request.description().isBlank()
                ? "Cash deposit by " + actorUsername
                : request.description();
        BankTransaction tx = transactionRepository.save(BankTransaction.builder()
                .reference("DEP-" + UUID.randomUUID())
                .fromAccount(null)
                .toAccount(target)
                .amount(request.amount())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .riskScore(0)
                .build());
        ledgerRepository.save(LedgerEntry.builder()
                .transaction(tx)
                .account(target)
                .direction(LedgerDirection.CREDIT)
                .amount(tx.getAmount())
                .balanceAfter(target.getBalance())
                .build());
        notificationService.send(target.getOwner().getEmail(), "Rikkei Bank deposit completed",
                "Deposit " + tx.getReference() + " credited " + tx.getAmount() + " " + target.getCurrency());
        return mapTransfer(tx);
    }

    @AuditedTransfer
    @Transactional
    
    @Override
    public TransferResponse transfer(TransferRequest request, String username) {
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Idempotency key is required");
        }
        Optional<BankTransaction> duplicate = transactionRepository
                .findByIdempotencyKeyAndFromAccountOwnerUsername(request.idempotencyKey(), username);
        if (duplicate.isPresent()) {
            BankTransaction existing = duplicate.get();
            Long existingTargetId = existing.getToAccount() == null ? null : existing.getToAccount().getId();
            if (!existing.getFromAccount().getId().equals(request.sourceAccountId())
                    || !Objects.equals(existingTargetId, request.targetAccountId())
                    || !Objects.equals(existing.getExternalBankCode(), request.externalBankCode())
                    || !Objects.equals(existing.getExternalAccountNumber(), request.externalAccountNumber())
                    || existing.getAmount().compareTo(request.amount()) != 0) {
                throw new BusinessException(HttpStatus.CONFLICT, "Idempotency key was already used for another request");
            }
            return mapTransfer(existing);
        }
        if (request.amount().compareTo(singleLimit) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "Single transfer limit exceeded");
        }
        if (request.otpChallengeId() == null || request.otp() == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "OTP verification is required");
        }
        otpService.verify(request.otpChallengeId(), request.otp(), username);
        TransactionType type = request.type() == null ? TransactionType.INTERNAL : request.type();
        if (type == TransactionType.INTERNAL && request.targetAccountId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Target account is required for internal transfer");
        }
        if (type == TransactionType.INTERBANK
                && (request.externalBankCode() == null || request.externalBankCode().isBlank()
                || request.externalAccountNumber() == null || request.externalAccountNumber().isBlank())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "External bank code and external account number are required for interbank transfer");
        }
        if (request.targetAccountId() != null && request.sourceAccountId().equals(request.targetAccountId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Source and target accounts must be different");
        }
        List<Long> accountIds = type == TransactionType.INTERNAL
                ? List.of(request.sourceAccountId(), request.targetAccountId())
                : List.of(request.sourceAccountId());
        List<Account> locked = accountRepository.lockAllByIds(accountIds);
        if (locked.size() != accountIds.size()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Account not found");
        }
        Account source = locked.stream().filter(a -> a.getId().equals(request.sourceAccountId())).findFirst().orElseThrow();
        Account target = type == TransactionType.INTERNAL
                ? locked.stream().filter(a -> a.getId().equals(request.targetAccountId())).findFirst().orElseThrow()
                : null;
        if (!source.getOwner().getUsername().equals(username)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Source account does not belong to current user");
        }
        if (!source.isActive()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Source account is inactive; complete eKYC approval first");
        }
        if (target != null && !target.isActive()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Target account is inactive");
        }
        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "Insufficient balance");
        }
        if (source.getOwner().getPin() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "Transaction PIN must be configured before transfer");
        }
        if (request.pin() == null || !passwordEncoder.matches(request.pin(), source.getOwner().getPin())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Invalid transaction PIN");
        }
        BigDecimal spentToday = transactionRepository.sumCompletedTransfersSince(source.getId(),
                LocalDate.now(ZoneOffset.UTC).atStartOfDay());
        if (spentToday.add(request.amount()).compareTo(dailyLimit) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "Daily transfer limit exceeded");
        }
        int riskScore = fraudDetectionService.score(request.amount(), spentToday, type);
        TransactionStatus status = request.amount().compareTo(approvalThreshold) >= 0 || riskScore >= 70
                ? TransactionStatus.PENDING_APPROVAL : TransactionStatus.PROCESSING;
        BankTransaction tx = transactionRepository.save(BankTransaction.builder()
                .reference("TX-" + UUID.randomUUID()).idempotencyKey(request.idempotencyKey())
                .fromAccount(source).toAccount(target).amount(request.amount())
                .type(type).status(status).riskScore(riskScore)
                .externalBankCode(request.externalBankCode()).externalAccountNumber(request.externalAccountNumber())
                .description(request.description()).build());
        if (status == TransactionStatus.PENDING_APPROVAL) {
            approvalRepository.save(TransferApproval.builder().transaction(tx).build());
            return mapTransfer(tx);
        }
        executeTransfer(tx, source, target);
        return mapTransfer(tx);
    }

    @Transactional
    @Override
    public void setupPin(String username, SetupPinRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getPin() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "Transaction PIN is already configured");
        }
        user.setPin(passwordEncoder.encode(request.pin()));
    }

    @Transactional
    
    @Override
    public TransferResponse approve(Long transactionId, ApprovalRequest request, String checkerUsername) {
        TransferApproval approval = approvalRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pending approval not found"));
        BankTransaction tx = approval.getTransaction();
        if (tx.getFromAccount().getOwner().getUsername().equals(checkerUsername)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Maker cannot approve own transfer");
        }
        approval.setChecker(userService.getByUsername(checkerUsername));
        approval.setDecision(request.decision());
        approval.setReason(request.reason());
        approval.setDecidedAt(LocalDateTime.now());
        if (request.decision() == ApprovalDecision.REJECTED) {
            tx.setStatus(TransactionStatus.REJECTED);
            return mapTransfer(tx);
        }
        List<Long> ids = tx.getToAccount() == null
                ? List.of(tx.getFromAccount().getId())
                : List.of(tx.getFromAccount().getId(), tx.getToAccount().getId());
        List<Account> locked = accountRepository.lockAllByIds(ids);
        Account source = locked.stream().filter(a -> a.getId().equals(tx.getFromAccount().getId())).findFirst().orElseThrow();
        Account target = tx.getToAccount() == null ? null
                : locked.stream().filter(a -> a.getId().equals(tx.getToAccount().getId())).findFirst().orElseThrow();
        if (source.getBalance().compareTo(tx.getAmount()) < 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "Insufficient balance at approval time");
        }
        executeTransfer(tx, source, target);
        return mapTransfer(tx);
    }

    
    @Override
    public OtpService.Challenge requestTransferOtp(String username) {
        return otpService.create(username);
    }

    private void executeTransfer(BankTransaction tx, Account source, Account target) {
        if (tx.getType() == TransactionType.INTERBANK) {
            InterbankGateway.Result result = interbankGateway.send(tx);
            if (!result.accepted()) {
                tx.setStatus(result.retryable() ? TransactionStatus.PROCESSING : TransactionStatus.FAILED);
                tx.setFailureReason(result.message());
                return;
            }
        }
        source.setBalance(source.getBalance().subtract(tx.getAmount()));
        tx.setStatus(TransactionStatus.COMPLETED);
        ledgerRepository.save(LedgerEntry.builder().transaction(tx).account(source)
                .direction(LedgerDirection.DEBIT).amount(tx.getAmount()).balanceAfter(source.getBalance()).build());
        if (target != null) {
            target.setBalance(target.getBalance().add(tx.getAmount()));
            ledgerRepository.save(LedgerEntry.builder().transaction(tx).account(target)
                    .direction(LedgerDirection.CREDIT).amount(tx.getAmount()).balanceAfter(target.getBalance()).build());
        }
        notificationService.send(source.getOwner().getEmail(), "Rikkei Bank transfer completed",
                "Transfer " + tx.getReference() + " debited " + tx.getAmount() + " " + source.getCurrency());
        if (target != null) {
            notificationService.send(target.getOwner().getEmail(), "Rikkei Bank funds received",
                    "Transfer " + tx.getReference() + " credited " + tx.getAmount() + " " + target.getCurrency());
        }
    }

    @Transactional
    
    @Override
    public void retryInterbank(Long transactionId) {
        BankTransaction tx = transactionRepository.findById(transactionId).orElseThrow();
        if (tx.getStatus() != TransactionStatus.PROCESSING || tx.getType() != TransactionType.INTERBANK) return;
        List<Long> ids = tx.getToAccount() == null
                ? List.of(tx.getFromAccount().getId())
                : List.of(tx.getFromAccount().getId(), tx.getToAccount().getId());
        List<Account> locked = accountRepository.lockAllByIds(ids);
        Account source = locked.stream().filter(a -> a.getId().equals(tx.getFromAccount().getId())).findFirst().orElseThrow();
        Account target = tx.getToAccount() == null ? null
                : locked.stream().filter(a -> a.getId().equals(tx.getToAccount().getId())).findFirst().orElseThrow();
        if (source.getBalance().compareTo(tx.getAmount()) < 0) {
            tx.setStatus(TransactionStatus.FAILED);
            tx.setFailureReason("Insufficient balance during interbank retry");
            return;
        }
        executeTransfer(tx, source, target);
    }

    @Transactional(readOnly = true)
    
    @Override
    public Page<StatementResponse> statement(Long accountId, String username, Pageable pageable) {
        Account account = ownedAccount(accountId, username);
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return transactionRepository.findStatement(accountId, sorted).map(tx -> {
            Account fromAccount = tx.getFromAccount();
            Account toAccount = tx.getToAccount();
            boolean debit = fromAccount != null && fromAccount.getId().equals(accountId);
            Account counterparty = debit ? toAccount : fromAccount;
            String counterpartyNumber = counterparty == null
                    ? (tx.getExternalAccountNumber() == null ? "CASH_DEPOSIT" : tx.getExternalAccountNumber())
                    : counterparty.getAccountNumber();
            return new StatementResponse(tx.getId(), tx.getReference(), debit ? "DEBIT" : "CREDIT",
                    tx.getAmount(), counterpartyNumber, tx.getDescription(), tx.getCreatedAt());
        });
    }

    @Transactional
    
    @Override
    public void changePin(String username, ChangePinRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getPin() != null && !passwordEncoder.matches(request.currentPin(), user.getPin())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Current PIN is invalid");
        }
        user.setPin(passwordEncoder.encode(request.newPin()));
    }

    private Account ownedAccount(Long id, String username) {
        Account account = findAccount(id);
        if (!account.getOwner().getUsername().equals(username)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Account does not belong to current user");
        }
        return account;
    }

    private AccountResponse mapAccount(Account a) {
        return new AccountResponse(a.getId(), a.getAccountNumber(), a.getBalance(), a.isActive());
    }

    private AccountDetailResponse mapAccountDetail(Account account) {
        return new AccountDetailResponse(account.getId(), account.getAccountNumber(), account.getOwner().getId(),
                account.getOwner().getUsername(), account.getBalance(), account.getCurrency(), account.isActive(),
                account.getCreatedAt());
    }

    private Account findAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = String.format("RB%010d",
                    Long.remainderUnsigned(UUID.randomUUID().getMostSignificantBits(), 10_000_000_000L));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    private TransferResponse mapTransfer(BankTransaction tx) {
        return new TransferResponse(tx.getId(), tx.getReference(), tx.getAmount(),
                tx.getFromAccount() == null ? "CASH" : tx.getFromAccount().getAccountNumber(),
                tx.getToAccount() == null ? tx.getExternalAccountNumber() : tx.getToAccount().getAccountNumber(),
                tx.getType(), tx.getStatus().name(), tx.getRiskScore(), tx.getCreatedAt());
    }
}
