package com.portfolio.erp.security;

import java.nio.charset.StandardCharsets;
import com.portfolio.erp.common.error.*;

public final class PasswordPolicy {
    private PasswordPolicy() { }
    public static void verify(String password) {
        if (password == null || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72
                || password.isBlank()) throw new UserFacingException(ErrorCode.PASSWORD_POLICY);
    }
}
