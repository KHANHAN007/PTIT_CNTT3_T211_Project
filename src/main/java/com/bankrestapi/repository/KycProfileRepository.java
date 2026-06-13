package com.bankrestapi.repository;

import com.bankrestapi.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface KycProfileRepository extends JpaRepository<KycProfile, Long> {
    Optional<KycProfile> findByUserUsername(String username);
    Page<KycProfile> findByStatus(KycStatus status, Pageable pageable);
}
