package com.bankrestapi.repository;

import com.bankrestapi.model.KycReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycReviewRepository extends JpaRepository<KycReview, Long> {}
