package com.bankrestapi.repository;

import com.bankrestapi.model.LedgerEntry;
import org.springframework.data.jpa.repository.*;
import java.math.BigDecimal;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    @Query("select coalesce(sum(case when l.direction = com.bankrestapi.model.LedgerDirection.CREDIT then l.amount else -l.amount end), 0) from LedgerEntry l where l.account.id = :accountId")
    BigDecimal calculateLedgerBalance(Long accountId);
}
