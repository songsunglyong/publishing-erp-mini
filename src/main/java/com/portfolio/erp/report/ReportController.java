package com.portfolio.erp.report;

import com.portfolio.erp.common.error.UserErrors;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
public class ReportController {
  private final ReportService service;
  private final UserErrors errors;

  public ReportController(ReportService service, UserErrors errors) {
    this.service = service;
    this.errors = errors;
  }

  @InitBinder("query")
  void binder(WebDataBinder binder) {
    binder.setAllowedFields("from", "to", "lowStock");
  }

  @GetMapping("/reports/sales")
  public String sales(
      @Valid @ModelAttribute("query") ReportQuery query,
      BindingResult binding,
      Model model,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (binding.hasErrors()) {
      errors.validation(binding, request, response);
      response.setStatus(400);
      return "report/sales";
    }
    var report = service.sales(query);
    model.addAttribute("summary", report.summary());
    model.addAttribute("monthly", report.monthly());
    model.addAttribute("topBooks", report.books());
    model.addAttribute("partnerSales", report.partners());
    model.addAttribute("lowStock", report.lowStock());
    return "report/sales";
  }

  @GetMapping("/dashboard")
  public String dashboard() {
    return "redirect:/reports/sales";
  }
}
