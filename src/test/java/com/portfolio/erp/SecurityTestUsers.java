package com.portfolio.erp;

import com.portfolio.erp.security.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Synthetic test-only credentials; never loaded by production component scanning. */
@TestConfiguration
public class SecurityTestUsers {
    public static final String PASSWORD = "Synthetic-test-Only!42";
    @Bean ApplicationRunner testUsers(UserMapper users, PasswordEncoder encoder) {
        return args -> {
            for (String role : new String[]{"ADMIN", "MANAGER", "USER"}) {
                String username = "tst_" + role.toLowerCase(java.util.Locale.ROOT);
                if (users.findByUsername(username) != null) continue;
                var account = new UserAccount(); account.setUsername(username); account.setDisplayName("가상 " + role);
                account.setRole(role); account.setEnabled(true); account.setPasswordHash(encoder.encode(PASSWORD));
                users.insert(account);
            }
        };
    }
}
