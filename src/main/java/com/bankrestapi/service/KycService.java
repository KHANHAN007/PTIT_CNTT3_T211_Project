package com.bankrestapi.service;

import com.bankrestapi.dto.BankDtos.KycDecision;
import com.bankrestapi.dto.BankDtos.KycResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface KycService {
    KycResponse upload(String username, MultipartFile file, String idNumber);
    Page<KycResponse> pending(Pageable pageable);
    KycResponse decide(Long id, KycDecision decision, String reviewerUsername);
}
