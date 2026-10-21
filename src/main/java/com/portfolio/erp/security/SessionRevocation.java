package com.portfolio.erp.security;

import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
public class SessionRevocation {
    public record AccountChanged(long id) { }
    private final SessionRegistry registry;
    private final LiveSessions live;
    public SessionRevocation(SessionRegistry registry, LiveSessions live) { this.registry = registry; this.live = live; }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void revoke(AccountChanged event) {
        for (Object value : registry.getAllPrincipals()) {
            if (value instanceof ErpPrincipal principal && principal.getId() == event.id())
                registry.getAllSessions(principal, false).forEach(session -> {
                    session.expireNow(); live.invalidate(session.getSessionId());
                    registry.removeSessionInformation(session.getSessionId());
                });
        }
    }
}
