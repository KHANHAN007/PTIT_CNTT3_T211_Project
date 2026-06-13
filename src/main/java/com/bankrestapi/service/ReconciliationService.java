package com.bankrestapi.service;

import com.bankrestapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerRepository;

    @Scheduled(cron = "${app.reconciliation.cron:0 0 2 * * *}")
    @Transactional(readOnly = true)
    public void reconcile() {
        accountRepository.findAll().forEach(account -> {
            var ledgerBalance = ledgerRepository.calculateLedgerBalance(account.getId());
            if (ledgerBalance.compareTo(account.getBalance()) != 0) {
                log.error("[RECONCILIATION] Account {} mismatch: accountBalance={}, ledgerBalance={}",
                        account.getAccountNumber(), account.getBalance(), ledgerBalance);
            }
        });
    }
}
