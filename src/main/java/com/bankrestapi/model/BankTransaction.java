package com.bankrestapi.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BankTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String reference;
    @ManyToOne(fetch = FetchType.LAZY)
    private Account fromAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    private Account toAccount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TransactionType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TransactionStatus status;
    private String description;
    @Column
    private String idempotencyKey;
    private String externalBankCode;
    private String externalAccountNumber;
    private Integer riskScore;
    private String failureReason;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null) status = TransactionStatus.PROCESSING;
    }
}
