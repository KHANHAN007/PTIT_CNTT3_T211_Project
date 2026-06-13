package com.bankrestapi.audit;

import com.bankrestapi.model.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditActionAspect {
    private final AuditLogService auditLogService;

    @Around("@annotation(action)")
    public Object audit(ProceedingJoinPoint point, AuditedAction action) throws Throwable {
        String outcome = "SUCCESS";
        try {
            return point.proceed();
        } catch (Throwable error) {
            outcome = "FAILED";
            throw error;
        } finally {
            HttpServletRequest request = currentRequest();
            var auth = SecurityContextHolder.getContext().getAuthentication();
            try {
                auditLogService.save(AuditLog.builder()
                        .actor(auth == null ? "anonymous" : auth.getName())
                        .action(action.value()).outcome(outcome)
                        .ipAddress(request == null ? null : request.getRemoteAddr())
                        .correlationId(MDC.get("correlationId"))
                        .details(point.getSignature().toShortString()).build());
            } catch (RuntimeException ex) {
                log.error("Could not persist audit action {}", action.value(), ex);
            }
        }
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes servlet ? servlet.getRequest() : null;
    }
}
