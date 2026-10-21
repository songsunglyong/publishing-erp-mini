package com.portfolio.erp.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class CurrentUserAdvice {
    @ModelAttribute("currentUser") public ErpPrincipal currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof ErpPrincipal principal ? principal : null;
    }
}
