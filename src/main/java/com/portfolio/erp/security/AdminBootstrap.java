package com.portfolio.erp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Explicit one-time bootstrap only; never ship a shared default password. */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserMapper users;
    private final PasswordEncoder encoder;
    private final boolean enabled;
    private final String username, password;
    public AdminBootstrap(UserMapper users, PasswordEncoder encoder,
            @Value("${security.bootstrap.enabled:false}") boolean enabled,
            @Value("${security.bootstrap.username:admin}") String username,
            @Value("${security.bootstrap.password:}") String password) {
        this.users = users; this.encoder = encoder; this.enabled = enabled; this.username = username; this.password = password;
    }
    @Override public void run(ApplicationArguments args) {
        if (!enabled || users.count() != 0) return;
        PasswordPolicy.verify(password);
        if (!username.matches("[a-z][a-z0-9_]{2,39}")) throw new IllegalStateException("bootstrap_configuration_invalid");
        var account = new UserAccount(); account.setUsername(username); account.setPasswordHash(encoder.encode(password));
        account.setDisplayName("시연 관리자"); account.setRole("ADMIN"); account.setEnabled(true);
        users.insert(account);
    }
}
