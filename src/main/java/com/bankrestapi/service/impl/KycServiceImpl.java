package com.bankrestapi.service.impl;

import com.bankrestapi.service.*;

import com.bankrestapi.dto.BankDtos.*;
import com.bankrestapi.exception.BusinessException;
import com.bankrestapi.model.*;
import com.bankrestapi.repository.KycProfileRepository;
import com.bankrestapi.repository.KycReviewRepository;
import com.bankrestapi.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class KycServiceImpl implements KycService {
    private final KycProfileRepository kycRepository;
    private final UserService userService;
    private final StorageService storageService;
    private final SensitiveDataService sensitiveDataService;
    private final KycReviewRepository reviewRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;

    @Transactional
    
    @Override
    public KycResponse upload(String username, MultipartFile file, String idNumber) {
        User user = userService.getByUsername(username);
        KycProfile profile = kycRepository.findByUserUsername(username).orElseGet(() ->
                KycProfile.builder().user(user).build());
        profile.setDocumentUrl(storageService.upload(file));
        profile.setIdNumberEncrypted(sensitiveDataService.encrypt(idNumber));
        profile.setStatus(KycStatus.PENDING);
        profile.setRejectionReason(null);
        return map(kycRepository.save(profile));
    }

    @Transactional(readOnly = true)
    
    @Override
    public Page<KycResponse> pending(Pageable pageable) {
        return kycRepository.findByStatus(KycStatus.PENDING, pageable).map(this::map);
    }

    @Transactional
    
    @Override
    public KycResponse decide(Long id, KycDecision decision, String reviewerUsername) {
        if (decision.status() == KycStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Decision must be CONFIRMED or REJECTED");
        }
        KycProfile profile = kycRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "KYC profile not found"));
        profile.setStatus(decision.status());
        profile.setRejectionReason(decision.status() == KycStatus.REJECTED ? decision.reason() : null);
        profile.getUser().setKyc(decision.status() == KycStatus.CONFIRMED);
        accountRepository.findByOwnerUsernameOrderByIdAsc(profile.getUser().getUsername())
                .forEach(account -> account.setActive(decision.status() == KycStatus.CONFIRMED));
        profile.setVerifiedAt(LocalDateTime.now());
        reviewRepository.save(KycReview.builder().profile(profile).reviewer(userService.getByUsername(reviewerUsername))
                .decision(decision.status()).reason(decision.reason()).build());
        notificationService.send(profile.getUser().getEmail(), "Rikkei Bank eKYC decision",
                "Your eKYC profile is now " + decision.status());
        return map(profile);
    }

    private KycResponse map(KycProfile k) {
        return new KycResponse(k.getId(), k.getUser().getId(), k.getUser().getUsername(), k.getDocumentUrl(),
                k.getStatus(), k.getRejectionReason(), k.getUpdatedAt());
    }
}
