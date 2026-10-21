package com.portfolio.erp.security;

/** Database-only authentication record. Never put this object in MVC models or logs. */
public class UserAccount {
    private long id, authVersion;
    private String username, passwordHash, displayName, role;
    private boolean enabled;
    public long getId() { return id; } public void setId(long value) { id = value; }
    public long getAuthVersion() { return authVersion; } public void setAuthVersion(long value) { authVersion = value; }
    public String getUsername() { return username; } public void setUsername(String value) { username = value; }
    public String getPasswordHash() { return passwordHash; } public void setPasswordHash(String value) { passwordHash = value; }
    public String getDisplayName() { return displayName; } public void setDisplayName(String value) { displayName = value; }
    public String getRole() { return role; } public void setRole(String value) { role = value; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean value) { enabled = value; }
}
