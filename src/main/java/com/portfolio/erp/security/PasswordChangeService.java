package com.portfolio.erp.security;

import com.portfolio.erp.common.error.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordChangeService {
    private final UserMapper users;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher events;
    public PasswordChangeService(UserMapper users, PasswordEncoder encoder, ApplicationEventPublisher events) {
        this.users = users; this.encoder = encoder; this.events = events;
    }
    @PreAuthorize("isAuthenticated()")
    @Transactional public void change(String currentPassword, String newPassword) {
        var value = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(value instanceof ErpPrincipal principal)) throw new UserFacingException(ErrorCode.LOGIN_REQUIRED);
        var account = users.findById(principal.getId());
        if (account == null || !account.isEnabled() || account.getAuthVersion() != principal.getVersion())
            throw new UserFacingException(ErrorCode.SESSION_EXPIRED);
        if (currentPassword == null || currentPassword.length() > 200 || !encoder.matches(currentPassword, account.getPasswordHash()))
            throw new UserFacingException(ErrorCode.PASSWORD_CHANGE_FAILED);
        PasswordPolicy.verify(newPassword);
        if (users.changePassword(account.getId(), encoder.encode(newPassword), principal.getVersion()) != 1)
            throw new UserFacingException(ErrorCode.SESSION_EXPIRED);
        events.publishEvent(new SessionRevocation.AccountChanged(account.getId()));
    }
}
