package com.portfolio.erp.book;

import jakarta.validation.Valid;
import com.portfolio.erp.common.error.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.portfolio.erp.common.PageQuery;

@Controller
@RequestMapping("/books")
public class BookController {
    private final BookService service;
    private final UserErrors userErrors;
    public BookController(BookService service, UserErrors userErrors) { this.service = service; this.userErrors = userErrors; }
    @InitBinder("form") void formBinder(WebDataBinder binder) { binder.setAllowedFields("isbn", "title", "author", "publisher", "publicationDate", "listPrice", "active", "_csrf"); }
    @InitBinder("query") void queryBinder(WebDataBinder binder) { binder.setAllowedFields("q", "status", "page"); }
    @GetMapping public String list(@Valid @ModelAttribute("query") PageQuery query, Model model) {
        model.addAttribute("result", service.list(query));
        return "book/list";
    }
    @GetMapping("/new") public String newForm(Model model) {
        model.addAttribute("form", new BookForm());
        return "book/form";
    }
    @GetMapping("/{id}/edit") public String edit(@PathVariable long id, Model model) {
        model.addAttribute("form", service.find(id));
        model.addAttribute("entityId", id);
        return "book/form";
    }
    @PostMapping public String create(@Valid @ModelAttribute("form") BookForm form, BindingResult errors, RedirectAttributes redirect, HttpServletRequest request, HttpServletResponse response) {
        if (errors.hasErrors()) { userErrors.validation(errors, request, response); return "book/form"; }
        try { service.create(form); } catch (RuntimeException ex) {
            userErrors.formFailure(ex, errors, "isbn", ErrorCode.DUPLICATE_ISBN, request, response);
            return "book/form";
        }
        redirect.addFlashAttribute("notice", "도서를 등록했습니다.");
        return "redirect:/books";
    }
    @PostMapping("/{id}") public String update(@PathVariable long id, @Valid @ModelAttribute("form") BookForm form,
            BindingResult errors, Model model, RedirectAttributes redirect, HttpServletRequest request, HttpServletResponse response) {
        model.addAttribute("entityId", id);
        if (errors.hasErrors()) { userErrors.validation(errors, request, response); return "book/form"; }
        try { service.find(id); service.update(id, form); } catch (RuntimeException ex) {
            if (ex instanceof com.portfolio.erp.common.NotFoundException) throw ex;
            userErrors.formFailure(ex, errors, "isbn", ErrorCode.DUPLICATE_ISBN, request, response);
            return "book/form";
        }
        redirect.addFlashAttribute("notice", "도서를 수정했습니다.");
        return "redirect:/books";
    }
    @PostMapping("/{id}/deactivate") public String deactivate(@PathVariable long id, RedirectAttributes redirect) {
        service.deactivate(id);
        redirect.addFlashAttribute("notice", "도서를 비활성화했습니다.");
        return "redirect:/books";
    }
}
