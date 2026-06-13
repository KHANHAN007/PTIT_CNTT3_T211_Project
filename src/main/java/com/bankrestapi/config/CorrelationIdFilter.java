package com.bankrestapi.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = request.getHeader("X-Correlation-ID");
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        MDC.put("correlationId", id);
        response.setHeader("X-Correlation-ID", id);
        try { chain.doFilter(request, response); } finally { MDC.remove("correlationId"); }
    }
}
