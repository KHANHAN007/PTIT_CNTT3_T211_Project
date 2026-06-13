package com.bankrestapi.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KycProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private User user;
    @Column(nullable = false)
    private String documentUrl;
    @Lob
    private String idNumberEncrypted;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private KycStatus status;
    private String rejectionReason;
    private LocalDateTime verifiedAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }
}
