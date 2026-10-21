package com.portfolio.erp.security;

import com.portfolio.erp.common.error.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AccountController {
    private final PasswordChangeService passwords;
    private final StaffService staff;
    private final SecurityResponses responses;
    private final UserErrors errors;
    public AccountController(PasswordChangeService passwords, StaffService staff, SecurityResponses responses, UserErrors errors) {
        this.passwords = passwords; this.staff = staff; this.responses = responses; this.errors = errors;
    }
    @GetMapping("/login") public String login(@RequestParam(required=false) String reason, HttpServletRequest request) {
        if ("required".equals(reason)) errors.report(ErrorCode.LOGIN_REQUIRED, null, request);
        if ("expired".equals(reason)) errors.report(ErrorCode.SESSION_EXPIRED, null, request);
        return "auth/login";
    }
    @GetMapping("/employees/{id}") public String profile(@PathVariable long id, Model model) {
        model.addAttribute("profile", staff.find(id)); return "auth/profile";
    }
    @GetMapping("/account/password") public String password() { return "auth/password"; }
    @PostMapping("/account/password") public String change(@RequestParam String currentPassword, @RequestParam String newPassword,
            HttpServletRequest request, HttpServletResponse response) {
        try { passwords.change(currentPassword, newPassword); }
        catch (RuntimeException ex) {
            var code = errors.classify(ex); errors.report(code, ex, request);
            response.setStatus(code.getStatus()); return "auth/password";
        }
        responses.clear(request, response);
        return "redirect:/login?passwordChanged";
    }
}
