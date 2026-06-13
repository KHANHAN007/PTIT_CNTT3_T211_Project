package com.bankrestapi.repository;

import com.bankrestapi.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
    @Query("""
        select t from BankTransaction t
        where (t.fromAccount is not null and t.fromAccount.id = :accountId)
           or (t.toAccount is not null and t.toAccount.id = :accountId)
        """)
    Page<BankTransaction> findStatement(Long accountId, Pageable pageable);

    Optional<BankTransaction> findByIdempotencyKeyAndFromAccountOwnerUsername(String key, String username);

    @Query("select coalesce(sum(t.amount), 0) from BankTransaction t where t.fromAccount.id = :accountId and t.status = com.bankrestapi.model.TransactionStatus.COMPLETED and t.createdAt >= :since")
    BigDecimal sumCompletedTransfersSince(Long accountId, LocalDateTime since);
    List<BankTransaction> findTop100ByStatusAndTypeOrderByCreatedAtAsc(TransactionStatus status, TransactionType type);
}
