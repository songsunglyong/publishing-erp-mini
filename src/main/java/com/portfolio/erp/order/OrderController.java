package com.portfolio.erp.order;

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
@RequestMapping("/orders")
public class OrderController {
  private final OrderService service;
  private final UserErrors errors;

  public OrderController(OrderService service, UserErrors errors) {
    this.service = service;
    this.errors = errors;
  }

  @InitBinder("form")
  void formBinder(WebDataBinder binder) {
    binder.setAutoGrowCollectionLimit(20);
    binder.setAllowedFields(
        "partnerId",
        "notes",
        "requestKey",
        "items[*].bookId",
        "items[*].quantity",
        "items[*].unitPrice");
  }

  @InitBinder("returnForm")
  void returnBinder(WebDataBinder binder) {
    binder.setAutoGrowCollectionLimit(20);
    binder.setAllowedFields("reason", "requestKey", "items[*].itemId", "items[*].quantity");
  }

  @InitBinder("query")
  void queryBinder(WebDataBinder binder) {
    binder.setAllowedFields("q", "status", "page");
  }

  @GetMapping
  public String list(@Valid @ModelAttribute("query") OrderQuery query, Model model) {
    model.addAttribute("result", service.list(query));
    return "order/list";
  }

  private String form(Model model) {
    model.addAttribute("books", service.books());
    model.addAttribute("partners", service.partners());
    return "order/form";
  }

  @GetMapping("/new")
  public String newForm(Model model) {
    var form = new OrderForm();
    form.setRequestKey(java.util.UUID.randomUUID().toString());
    model.addAttribute("form", form);
    return form(model);
  }

  @PostMapping
  public String create(
      @Valid @ModelAttribute("form") OrderForm form,
      BindingResult binding,
      Model model,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (binding.hasErrors()) {
      errors.validation(binding, request, response);
      return form(model);
    }
    try {
      return "redirect:/orders/" + service.create(form);
    } catch (RuntimeException ex) {
      errors.formFailure(
          ex, binding, "requestKey", ErrorCode.DUPLICATE_OPERATION, request, response);
      return form(model);
    }
  }

  @GetMapping("/{id}")
  public String detail(@PathVariable long id, Model model) {
    var order = service.find(id);
    model.addAttribute("order", order);
    model.addAttribute("returns", service.returns(id));
    if (!model.containsAttribute("returnForm")) {
      var form = new ReturnForm();
      form.setRequestKey(java.util.UUID.randomUUID().toString());
      for (var item : order.getItems()) {
        var line = new ReturnForm.Line();
        line.setItemId(item.getId());
        form.getItems().add(line);
      }
      model.addAttribute("returnForm", form);
    }
    return "order/detail";
  }

  @PostMapping("/{id}/confirm")
  public String confirm(@PathVariable long id, RedirectAttributes redirect) {
    service.confirm(id);
    redirect.addFlashAttribute("notice", "주문을 확정했습니다. 재고는 출고 시 차감됩니다.");
    return "redirect:/orders/" + id;
  }

  @PostMapping("/{id}/fulfill")
  public String fulfill(@PathVariable long id, RedirectAttributes redirect) {
    service.fulfill(id);
    redirect.addFlashAttribute("notice", "출고를 완료했습니다.");
    return "redirect:/orders/" + id;
  }

  @PostMapping("/{id}/cancel")
  public String cancel(@PathVariable long id, RedirectAttributes redirect) {
    service.cancel(id);
    redirect.addFlashAttribute("notice", "주문을 취소했습니다.");
    return "redirect:/orders/" + id;
  }

  @PostMapping("/{id}/returns")
  public String returnItems(
      @PathVariable long id,
      @Valid @ModelAttribute("returnForm") ReturnForm form,
      BindingResult binding,
      Model model,
      HttpServletRequest request,
      HttpServletResponse response,
      RedirectAttributes redirect) {
    if (binding.hasErrors()) {
      errors.validation(binding, request, response);
      return detail(id, model);
    }
    try {
      service.returnItems(id, form);
    } catch (RuntimeException ex) {
      errors.formFailure(
          ex, binding, "requestKey", ErrorCode.DUPLICATE_OPERATION, request, response);
      return detail(id, model);
    }
    redirect.addFlashAttribute("notice", "반품을 처리하고 재고를 복원했습니다.");
    return "redirect:/orders/" + id;
  }
}
