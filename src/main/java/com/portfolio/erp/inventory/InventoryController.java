package com.portfolio.erp.inventory;

import com.portfolio.erp.common.error.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory")
public class InventoryController {
  private final InventoryService service;
  private final UserErrors errors;

  public InventoryController(InventoryService service, UserErrors errors) {
    this.service = service;
    this.errors = errors;
  }

  @InitBinder("form")
  void formBinder(WebDataBinder binder) {
    binder.setAllowedFields("bookId", "quantity", "note", "requestKey");
  }

  @InitBinder("query")
  void queryBinder(WebDataBinder binder) {
    binder.setAllowedFields("q", "status", "page", "bookId");
  }

  @GetMapping
  public String list(@Valid @ModelAttribute("query") InventoryQuery query, Model model) {
    model.addAttribute("result", service.list(query));
    return "inventory/list";
  }

  @GetMapping("/movements")
  public String movements(@Valid @ModelAttribute("query") InventoryQuery query, Model model) {
    model.addAttribute("result", service.movements(query));
    return "inventory/movements";
  }

  private String form(boolean adjustment, Model model) {
    model.addAttribute("adjustment", adjustment);
    model.addAttribute("books", service.books());
    return "inventory/form";
  }

  @GetMapping("/receipts")
  public String receiptForm(Model model) {
    var form = new StockForm();
    form.setRequestKey(java.util.UUID.randomUUID().toString());
    model.addAttribute("form", form);
    return form(false, model);
  }

  @GetMapping("/adjustments")
  public String adjustmentForm(Model model) {
    var form = new StockForm();
    form.setRequestKey(java.util.UUID.randomUUID().toString());
    model.addAttribute("form", form);
    return form(true, model);
  }

  private String save(
      boolean adjustment,
      StockForm form,
      BindingResult binding,
      Model model,
      HttpServletRequest request,
      HttpServletResponse response,
      RedirectAttributes redirect) {
    if (binding.hasErrors()) {
      errors.validation(binding, request, response);
      return form(adjustment, model);
    }
    try {
      if (adjustment) service.adjust(form);
      else service.receive(form);
    } catch (RuntimeException ex) {
      errors.formFailure(
          ex, binding, "requestKey", ErrorCode.DUPLICATE_OPERATION, request, response);
      return form(adjustment, model);
    }
    redirect.addFlashAttribute("notice", adjustment ? "재고를 조정했습니다." : "입고를 완료했습니다.");
    return "redirect:/inventory";
  }

  @PostMapping("/receipts")
  public String receive(
      @Valid @ModelAttribute("form") StockForm form,
      BindingResult binding,
      Model model,
      HttpServletRequest request,
      HttpServletResponse response,
      RedirectAttributes redirect) {
    return save(false, form, binding, model, request, response, redirect);
  }

  @PostMapping("/adjustments")
  public String adjust(
      @Valid @ModelAttribute("form") StockForm form,
      BindingResult binding,
      Model model,
      HttpServletRequest request,
      HttpServletResponse response,
      RedirectAttributes redirect) {
    return save(true, form, binding, model, request, response, redirect);
  }
}
