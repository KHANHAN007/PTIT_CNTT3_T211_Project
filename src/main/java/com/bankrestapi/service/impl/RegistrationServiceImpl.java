package com.bankrestapi.service.impl;

import com.bankrestapi.dto.BankDtos.KycResponse;
import com.bankrestapi.dto.UserDtos.CreateRequest;
import com.bankrestapi.dto.UserDtos.UserResponse;
import com.bankrestapi.service.KycService;
import com.bankrestapi.service.RegistrationService;
import com.bankrestapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final UserService userService;
    private final KycService kycService;

    @Override
    @Transactional
    public UserResponse register(CreateRequest request) {
        return userService.registerCustomer(request);
    }

    @Override
    @Transactional
    public KycResponse registerWithKyc(CreateRequest request, MultipartFile document, String idNumber) {
        UserResponse user = userService.registerCustomer(request);
        return kycService.upload(user.username(), document, idNumber);
    }
}
