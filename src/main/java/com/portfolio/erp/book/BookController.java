package com.portfolio.erp.book;

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
@RequestMapping("/books")
public class BookController {
    private final BookService service;
    public BookController(BookService service) { this.service = service; }
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
    @PostMapping public String create(@Valid @ModelAttribute("form") BookForm form, BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) return "book/form";
        try { service.create(form); } catch (DuplicateKeyException ex) {
            errors.rejectValue("isbn", "duplicate", "이미 등록된 ISBN입니다.");
            return "book/form";
        }
        redirect.addFlashAttribute("notice", "도서를 등록했습니다.");
        return "redirect:/books";
    }
    @PostMapping("/{id}") public String update(@PathVariable long id, @Valid @ModelAttribute("form") BookForm form,
            BindingResult errors, Model model, RedirectAttributes redirect) {
        service.find(id);
        model.addAttribute("entityId", id);
        if (errors.hasErrors()) return "book/form";
        try { service.update(id, form); } catch (DuplicateKeyException ex) {
            errors.rejectValue("isbn", "duplicate", "이미 등록된 ISBN입니다.");
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
