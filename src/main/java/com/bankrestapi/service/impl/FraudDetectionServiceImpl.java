package com.bankrestapi.service.impl;

import com.bankrestapi.service.*;

import com.bankrestapi.model.TransactionType;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class FraudDetectionServiceImpl implements FraudDetectionService {
    
    @Override
    public int score(BigDecimal amount, BigDecimal spentToday, TransactionType type) {
        int score = 0;
        if (amount.compareTo(new BigDecimal("20000000")) >= 0) score += 35;
        if (spentToday.compareTo(new BigDecimal("100000000")) >= 0) score += 35;
        if (type == TransactionType.INTERBANK) score += 20;
        return Math.min(score, 100);
    }
}
