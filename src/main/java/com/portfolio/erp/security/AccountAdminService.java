package com.portfolio.erp.security;

import java.util.List;
import com.portfolio.erp.common.NotFoundException;
import com.portfolio.erp.common.error.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AccountAdminService {
    private final UserMapper users;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher events;
    public AccountAdminService(UserMapper users, PasswordEncoder encoder, ApplicationEventPublisher events) {
        this.users = users; this.encoder = encoder; this.events = events;
    }
    public List<StaffProfile> list() { return users.profiles(); }
    @Transactional public void create(String username, String displayName, String role, String password) {
        if (username == null || !username.matches("[a-z][a-z0-9_]{2,39}") || displayName == null
                || displayName.isBlank() || displayName.length() > 100 || role == null
                || !List.of("ADMIN", "MANAGER", "USER").contains(role)) throw new UserFacingException(ErrorCode.VALIDATION_FAILED);
        PasswordPolicy.verify(password);
        var account = new UserAccount(); account.setUsername(username); account.setDisplayName(displayName.trim());
        account.setRole(role); account.setPasswordHash(encoder.encode(password)); account.setEnabled(true);
        users.insert(account);
    }
    @Transactional public void block(long id) {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof ErpPrincipal current && current.getId() == id) throw new UserFacingException(ErrorCode.BUSINESS_RULE);
        if (users.block(id) == 0) throw new NotFoundException();
        events.publishEvent(new SessionRevocation.AccountChanged(id));
    }
}
