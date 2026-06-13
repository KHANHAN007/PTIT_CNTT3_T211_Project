package com.bankrestapi.audit;

import com.bankrestapi.dto.BankDtos.TransferRequest;
import com.bankrestapi.dto.BankDtos.TransferResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class TransferAuditAspect {
    @AfterReturning(pointcut = "@annotation(com.bankrestapi.audit.AuditedTransfer)", returning = "result")
    public void success(TransferResponse result) {
        log.info("[AUDIT] Account {} transferred {} to account {}, reference={}",
                result.fromAccount(), result.amount(), result.toAccount(), result.reference());
    }

    @AfterThrowing(pointcut = "@annotation(com.bankrestapi.audit.AuditedTransfer)", throwing = "error")
    public void failure(JoinPoint point, Throwable error) {
        TransferRequest request = (TransferRequest) point.getArgs()[0];
        log.warn("[AUDIT] Failed transfer from account {} to account {}, amount={}, reason={}",
                request.sourceAccountId(), request.targetAccountId(), request.amount(), error.getMessage());
    }
}
