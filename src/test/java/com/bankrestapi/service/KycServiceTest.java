package com.bankrestapi.service;

import com.bankrestapi.dto.BankDtos.KycDecision;
import com.bankrestapi.model.*;
import com.bankrestapi.repository.*;
import com.bankrestapi.service.impl.KycServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {
    @Mock KycProfileRepository profiles;
    @Mock UserService users;
    @Mock StorageService storage;
    @Mock SensitiveDataService sensitiveData;
    @Mock KycReviewRepository reviews;
    @Mock AccountRepository accounts;
    @Mock NotificationService notifications;
    @InjectMocks KycServiceImpl service;

    @Test
    void confirmedKycActivatesCustomerAccounts() {
        User customer = User.builder().username("customer").email("c@test").kyc(false).build();
        User reviewer = User.builder().username("staff").build();
        Account account = Account.builder().owner(customer).active(false).build();
        KycProfile profile = KycProfile.builder().id(1L).user(customer).status(KycStatus.PENDING).build();
        when(profiles.findById(1L)).thenReturn(Optional.of(profile));
        when(users.getByUsername("staff")).thenReturn(reviewer);
        when(accounts.findByOwnerUsernameOrderByIdAsc("customer")).thenReturn(List.of(account));

        service.decide(1L, new KycDecision(KycStatus.CONFIRMED, null), "staff");

        assertTrue(customer.isKyc());
        assertTrue(account.isActive());
        verify(reviews).save(any());
    }

    @Test
    void uploadEncryptsIdentityNumber() {
        MultipartFile file = mock(MultipartFile.class);
        User customer = User.builder().id(1L).username("customer").build();
        when(users.getByUsername("customer")).thenReturn(customer);
        when(profiles.findByUserUsername("customer")).thenReturn(Optional.empty());
        when(storage.upload(file)).thenReturn("/uploads/id.png");
        when(sensitiveData.encrypt("001")).thenReturn("encrypted");
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.upload("customer", file, "001");

        assertEquals(KycStatus.PENDING, result.status());
        verify(sensitiveData).encrypt("001");
    }
}
