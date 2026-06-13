package com.bankrestapi.service;

import com.bankrestapi.model.TransactionType;
import com.bankrestapi.service.impl.FraudDetectionServiceImpl;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class FraudDetectionServiceTest {
    private final FraudDetectionService service = new FraudDetectionServiceImpl();

    @Test
    void highValueInterbankTransferProducesElevatedRisk() {
        int score = service.score(new BigDecimal("25000000"), new BigDecimal("120000000"), TransactionType.INTERBANK);
        assertEquals(90, score);
    }
}
