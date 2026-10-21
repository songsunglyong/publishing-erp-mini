package com.portfolio.erp.security;

import java.io.IOException;
import java.time.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.filter.OncePerRequestFilter;
import com.portfolio.erp.common.error.*;

public class SessionGuardFilter extends OncePerRequestFilter {
    public static final String LOGIN_AT = "security.loginAt", LAST_SEEN = "security.lastSeen";
    private final Clock clock;
    private final SessionRegistry registry;
    private final UserMapper users;
    private final SecurityResponses responses;
    public SessionGuardFilter(Clock clock, SessionRegistry registry, UserMapper users, SecurityResponses responses) {
        this.clock = clock; this.registry = registry; this.users = users; this.responses = responses;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof ErpPrincipal principal) {
            var session = request.getSession(false);
            boolean valid = session != null;
            Instant now = clock.instant();
            if (valid) {
                Object start = session.getAttribute(LOGIN_AT), last = session.getAttribute(LAST_SEEN);
                var info = registry.getSessionInformation(session.getId());
                valid = start instanceof Instant logged && last instanceof Instant seen
                        && now.isBefore(logged.plus(Duration.ofHours(8)))
                        && now.isBefore(seen.plus(Duration.ofMinutes(30))) && info != null && !info.isExpired();
            }
            if (valid) {
                UserAccount account;
                try { account = users.findById(principal.getId()); }
                catch (RuntimeException ex) {
                    // Fail closed if revocation cannot be checked. Do not execute the protected handler.
                    responses.send(request, response, ErrorCode.SERVICE_UNAVAILABLE); return;
                }
                valid = account != null && account.isEnabled() && account.getAuthVersion() == principal.getVersion()
                        && account.getRole().equals(principal.getRole());
            }
            if (!valid) {
                responses.clear(request, response);
                responses.send(request, response, ErrorCode.SESSION_EXPIRED); return;
            }
            session.setAttribute(LAST_SEEN, now);
        } else if (request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid()
                && !request.getRequestURI().equals("/login") && !request.getRequestURI().startsWith("/css/")
                && !request.getRequestURI().startsWith("/webjars/") && !request.getRequestURI().equals("/error")) {
            responses.clear(request, response);
            responses.send(request, response, ErrorCode.SESSION_EXPIRED); return;
        }
        chain.doFilter(request, response);
    }
}
