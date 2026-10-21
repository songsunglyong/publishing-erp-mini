package com.portfolio.erp.security;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** User erases its password after authentication; inherited equality uses the canonical username. */
public class ErpPrincipal extends User {
    private final long id, version;
    private final String displayName, role;
    public ErpPrincipal(UserAccount account) {
        super(account.getUsername(), account.getPasswordHash(), account.isEnabled(), true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole())));
        id = account.getId(); version = account.getAuthVersion();
        displayName = account.getDisplayName(); role = account.getRole();
    }
    public long getId() { return id; }
    public long getVersion() { return version; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
}
