package com.bankrestapi.audit;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ExecutionTimeAspect {
    @Around("execution(public * com.bankrestapi.controller..*(..)) || execution(public * com.bankrestapi.service..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint point) throws Throwable {
        long started = System.nanoTime();
        try {
            return point.proceed();
        } finally {
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;
            log.info("[PERFORMANCE] {} completed in {} ms", point.getSignature().toShortString(), elapsedMs);
        }
    }
}
