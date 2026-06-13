package com.bankrestapi.service;

import com.bankrestapi.model.*;
import com.bankrestapi.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InterbankRetryScheduler {
    private final BankTransactionRepository repository;
    private final BankService bankService;

    @Scheduled(fixedDelayString = "${app.interbank.retry-delay-ms:60000}")
    public void retryPendingTransfers() {
        repository.findTop100ByStatusAndTypeOrderByCreatedAtAsc(TransactionStatus.PROCESSING, TransactionType.INTERBANK)
                .forEach(tx -> bankService.retryInterbank(tx.getId()));
    }
}
