package com.bankrestapi.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KycReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private KycProfile profile;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User reviewer;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private KycStatus decision;
    private String reason;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }
}
