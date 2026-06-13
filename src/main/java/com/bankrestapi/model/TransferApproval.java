package com.bankrestapi.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_approvals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransferApproval {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private BankTransaction transaction;
    @ManyToOne(fetch = FetchType.LAZY)
    private User checker;
    @Enumerated(EnumType.STRING)
    private ApprovalDecision decision;
    private String reason;
    private LocalDateTime decidedAt;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }
}
