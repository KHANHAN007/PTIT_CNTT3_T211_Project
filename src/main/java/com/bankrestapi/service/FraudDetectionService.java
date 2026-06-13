package com.bankrestapi.service;

import com.bankrestapi.model.TransactionType;

import java.math.BigDecimal;

public interface FraudDetectionService {
    int score(BigDecimal amount, BigDecimal spentToday, TransactionType type);
}
