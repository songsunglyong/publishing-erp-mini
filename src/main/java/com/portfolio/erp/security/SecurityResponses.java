package com.portfolio.erp.security;

import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.erp.common.GlobalExceptionHandler;
import com.portfolio.erp.common.error.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;

@Component
public class SecurityResponses {
    private final UserErrors errors;
    private final ObjectMapper json;
    public SecurityResponses(UserErrors errors, ObjectMapper json) { this.errors = errors; this.json = json; }
    public void send(HttpServletRequest request, HttpServletResponse response, ErrorCode code)
            throws IOException, ServletException {
        var notice = errors.report(code, null, request);
        response.setHeader("X-Trace-Id", notice.getTraceId());
        response.setHeader("Cache-Control", "no-store");
        response.setStatus(code.getStatus());
        if (GlobalExceptionHandler.wantsJson(request)) {
            response.setContentType("application/json;charset=UTF-8");
            json.writeValue(response.getOutputStream(), notice);
        } else if (code == ErrorCode.LOGIN_REQUIRED || code == ErrorCode.SESSION_EXPIRED) {
            response.sendRedirect("/login?reason=" + (code == ErrorCode.SESSION_EXPIRED ? "expired" : "required"));
        } else {
            String view = code == ErrorCode.LOGIN_FAILED ? "auth/login" : "common/error";
            request.getRequestDispatcher("/WEB-INF/jsp/" + view + ".jsp").forward(request, response);
        }
    }
    public void clear(HttpServletRequest request, HttpServletResponse response) {
        new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler().logout(request, response, null);
        new org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler("JSESSIONID").logout(request, response, null);
    }
}
