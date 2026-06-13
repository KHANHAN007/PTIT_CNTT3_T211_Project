package com.bankrestapi.controller;

import com.bankrestapi.dto.ApiResponse;
import com.bankrestapi.model.AuditLog;
import com.bankrestapi.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditController {
    private final AuditLogRepository repository;

    @GetMapping
    public ApiResponse<Page<AuditLog>> list(Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok("Audit logs retrieved", repository.findAll(sorted));
    }
}
