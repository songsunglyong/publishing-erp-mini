package com.portfolio.erp.security;

import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;

@Component("staffAccess")
public class StaffAccess {
    public boolean canRead(long id, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof ErpPrincipal principal)) return false;
        return principal.getId() == id || authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
