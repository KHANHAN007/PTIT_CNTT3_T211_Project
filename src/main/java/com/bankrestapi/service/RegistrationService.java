package com.bankrestapi.service;

import com.bankrestapi.dto.BankDtos.KycResponse;
import com.bankrestapi.dto.UserDtos.CreateRequest;
import com.bankrestapi.dto.UserDtos.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface RegistrationService {
    UserResponse register(CreateRequest request);
    KycResponse registerWithKyc(CreateRequest request, MultipartFile document, String idNumber);
}
