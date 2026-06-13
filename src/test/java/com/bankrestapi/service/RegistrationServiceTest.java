package com.bankrestapi.service;

import com.bankrestapi.dto.UserDtos.CreateRequest;
import com.bankrestapi.dto.UserDtos.UserResponse;
import com.bankrestapi.model.Role;
import com.bankrestapi.service.impl.RegistrationServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {
    UserService users = mock(UserService.class);
    KycService kyc = mock(KycService.class);
    RegistrationService service = new RegistrationServiceImpl(users, kyc);

    @Test
    void registrationWithKycPropagatesUploadFailureForTransactionRollback() {
        CreateRequest request = new CreateRequest("customer", "Password1234", "c@test", "Customer", "0901", null);
        UserResponse user = new UserResponse(1L, "customer", "c@test", "Customer", "0901",
                Role.CUSTOMER, true, false, LocalDateTime.now());
        MultipartFile file = mock(MultipartFile.class);
        when(users.registerCustomer(request)).thenReturn(user);
        when(kyc.upload("customer", file, "001")).thenThrow(new IllegalStateException("storage failed"));

        assertThrows(IllegalStateException.class, () -> service.registerWithKyc(request, file, "001"));
    }
}
