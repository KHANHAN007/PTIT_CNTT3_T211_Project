package com.bankrestapi.repository;

import com.bankrestapi.model.TransferApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransferApprovalRepository extends JpaRepository<TransferApproval, Long> {
    Optional<TransferApproval> findByTransactionId(Long transactionId);
}
