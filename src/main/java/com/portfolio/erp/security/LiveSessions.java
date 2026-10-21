package com.portfolio.erp.security;

import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.http.*;

/** Single-server live session handles; never log or expose keys. Lifecycle events bound memory use. */
public class LiveSessions implements HttpSessionListener, HttpSessionIdListener {
    private final ConcurrentHashMap<String, HttpSession> sessions = new ConcurrentHashMap<>();
    public void track(HttpSession session) { sessions.put(session.getId(), session); }
    public void forget(HttpSession session) { if (session != null) sessions.remove(session.getId()); }
    public void invalidate(String id) {
        var session = sessions.remove(id);
        if (session != null) try { session.invalidate(); } catch (IllegalStateException ignored) { }
    }
    @Override public void sessionDestroyed(HttpSessionEvent event) { forget(event.getSession()); }
    @Override public void sessionIdChanged(HttpSessionEvent event, String oldId) {
        if (sessions.remove(oldId) != null) track(event.getSession());
    }
}
