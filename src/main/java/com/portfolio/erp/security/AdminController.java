package com.portfolio.erp.security;

import com.portfolio.erp.common.error.*;
import jakarta.servlet.http.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AccountAdminService accounts;
    private final UserErrors errors;
    public AdminController(AccountAdminService accounts, UserErrors errors) { this.accounts = accounts; this.errors = errors; }
    @GetMapping public String list(Model model) { model.addAttribute("accounts", accounts.list()); return "admin/users"; }
    @PostMapping public String create(@RequestParam String username, @RequestParam String displayName,
            @RequestParam String role, @RequestParam String password, Model model,
            HttpServletRequest request, HttpServletResponse response) {
        try { accounts.create(username, displayName, role, password); }
        catch (RuntimeException ex) {
            var code = ex instanceof DuplicateKeyException ? ErrorCode.DUPLICATE_USERNAME : errors.classify(ex);
            errors.report(code, ex, request); response.setStatus(code.getStatus());
            // Whitelist non-secret fields; the password is never added to the model.
            model.addAttribute("inputUsername", username); model.addAttribute("inputDisplayName", displayName);
            return list(model);
        }
        return "redirect:/admin/users";
    }
    @PostMapping("/{id}/block") public String block(@PathVariable long id) {
        accounts.block(id); return "redirect:/admin/users";
    }
}
