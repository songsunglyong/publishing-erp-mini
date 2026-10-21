package com.portfolio.erp.common.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.*;
import com.portfolio.erp.common.GlobalExceptionHandler;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/** Container failures (including JSP rendering) also bypass Boot's default error details. */
@Controller
public class SafeErrorController implements ErrorController {
    private final UserErrors errors;
    public SafeErrorController(UserErrors errors) { this.errors = errors; }
    @RequestMapping("/error")
    public Object error(HttpServletRequest request, HttpServletResponse response) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        ErrorCode code = GlobalExceptionHandler.statusCode(status instanceof Integer value ? value : 500);
        Throwable ex = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        var notice = errors.report(code, ex, request);
        response.setHeader("Cache-Control", "no-store");
        if (GlobalExceptionHandler.wantsJson(request)) return ResponseEntity.status(code.getStatus()).body(notice);
        var view = new ModelAndView("common/error", "errorNotice", notice);
        view.setStatus(org.springframework.http.HttpStatus.valueOf(code.getStatus()));
        return view;
    }
}
