package com.portfolio.erp.security;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import com.portfolio.erp.common.error.*;

public class LoginAttemptFilter extends OncePerRequestFilter {
    private final LoginAttempts attempts;
    private final SecurityResponses responses;
    private final UserErrors errors;
    public LoginAttemptFilter(LoginAttempts attempts, SecurityResponses responses, UserErrors errors) {
        this.attempts = attempts; this.responses = responses; this.errors = errors;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equals(request.getMethod()) && (request.getContextPath() + "/login").equals(request.getRequestURI())) {
            String username = request.getParameter("username"), password = request.getParameter("password");
            // Bound untrusted input before hashing or database allocation; never retain credentials.
            if (username == null || username.length() > 40 || password == null || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                responses.send(request, response, ErrorCode.LOGIN_FAILED); return;
            }
            boolean allowed;
            try { allowed = attempts.reserve(username, request.getRemoteAddr()); }
            catch (RuntimeException ex) {
                errors.report(errors.classify(ex), ex, request);
                responses.send(request, response, ErrorCode.SERVICE_UNAVAILABLE); return;
            }
            if (!allowed) { responses.send(request, response, ErrorCode.LOGIN_FAILED); return; }
        }
        chain.doFilter(request, response);
    }
}
