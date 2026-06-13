package com.bankrestapi.controller;

import com.bankrestapi.dto.ApiResponse;
import com.bankrestapi.dto.UserDtos.*;
import com.bankrestapi.dto.BankDtos.KycResponse;
import com.bankrestapi.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicRegistrationController {
    private final RegistrationService registrationService;

    @PostMapping("/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody CreateRequest request) {
        return ApiResponse.ok("Registration created", registrationService.register(request));
    }

    @PostMapping(value = "/registrations/with-kyc", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KycResponse> registerWithKyc(@Valid @RequestPart("user") CreateRequest request,
                                                    @RequestPart("document") MultipartFile document,
                                                    @RequestParam String idNumber) {
        return ApiResponse.ok("Registration and KYC created",
                registrationService.registerWithKyc(request, document, idNumber));
    }
}
