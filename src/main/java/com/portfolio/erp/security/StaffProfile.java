package com.portfolio.erp.security;

/** Explicit allowlist: no password hash, authentication version or session identifier. */
public class StaffProfile {
    private long id;
    private String username, displayName, role;
    private boolean enabled;
    public long getId() { return id; } public void setId(long value) { id = value; }
    public String getUsername() { return username; } public void setUsername(String value) { username = value; }
    public String getDisplayName() { return displayName; } public void setDisplayName(String value) { displayName = value; }
    public String getRole() { return role; } public void setRole(String value) { role = value; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean value) { enabled = value; }
}
