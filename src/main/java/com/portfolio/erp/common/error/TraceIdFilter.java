package com.portfolio.erp.common.error;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String ATTRIBUTE = "errorTraceId";
    @Override protected boolean shouldNotFilterErrorDispatch() { return false; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        // Never trust a client-supplied ID; keep the same ID on container error dispatch.
        if (request.getAttribute(ATTRIBUTE) == null) request.setAttribute(ATTRIBUTE, UUID.randomUUID().toString());
        String id = (String) request.getAttribute(ATTRIBUTE);
        response.setHeader("X-Trace-Id", id);
        response.setHeader("X-Content-Type-Options", "nosniff");
        try (var ignored = MDC.putCloseable("traceId", id)) { chain.doFilter(request, response); }
    }
}
