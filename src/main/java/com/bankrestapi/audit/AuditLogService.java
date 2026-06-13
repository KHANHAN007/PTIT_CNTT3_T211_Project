package com.bankrestapi.audit;

import com.bankrestapi.model.AuditLog;
import com.bankrestapi.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(AuditLog log) {
        repository.save(log);
    }
}
