package com.portfolio.erp.partner;

import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.portfolio.erp.common.PageQuery;

@Controller
@RequestMapping("/partners")
public class PartnerController {
    private final PartnerService service;
    public PartnerController(PartnerService service) { this.service = service; }
    @InitBinder("form") void formBinder(WebDataBinder binder) { binder.setAllowedFields("code", "name", "type", "contactPerson", "phone", "email", "address", "active", "_csrf"); }
    @InitBinder("query") void queryBinder(WebDataBinder binder) { binder.setAllowedFields("q", "status", "page"); }
    @GetMapping public String list(@Valid @ModelAttribute("query") PageQuery query, Model model) {
        model.addAttribute("result", service.list(query));
        return "partner/list";
    }
    @GetMapping("/new") public String newForm(Model model) {
        model.addAttribute("form", new PartnerForm());
        return "partner/form";
    }
    @GetMapping("/{id}/edit") public String edit(@PathVariable long id, Model model) {
        model.addAttribute("form", service.find(id));
        model.addAttribute("entityId", id);
        return "partner/form";
    }
    @PostMapping public String create(@Valid @ModelAttribute("form") PartnerForm form, BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) return "partner/form";
        try { service.create(form); } catch (DuplicateKeyException ex) {
            errors.rejectValue("code", "duplicate", "이미 등록된 거래처 코드입니다.");
            return "partner/form";
        }
        redirect.addFlashAttribute("notice", "거래처를 등록했습니다.");
        return "redirect:/partners";
    }
    @PostMapping("/{id}") public String update(@PathVariable long id, @Valid @ModelAttribute("form") PartnerForm form,
            BindingResult errors, Model model, RedirectAttributes redirect) {
        service.find(id);
        model.addAttribute("entityId", id);
        if (errors.hasErrors()) return "partner/form";
        try { service.update(id, form); } catch (DuplicateKeyException ex) {
            errors.rejectValue("code", "duplicate", "이미 등록된 거래처 코드입니다.");
            return "partner/form";
        }
        redirect.addFlashAttribute("notice", "거래처를 수정했습니다.");
        return "redirect:/partners";
    }
    @PostMapping("/{id}/deactivate") public String deactivate(@PathVariable long id, RedirectAttributes redirect) {
        service.deactivate(id);
        redirect.addFlashAttribute("notice", "거래처를 비활성화했습니다.");
        return "redirect:/partners";
    }
}
