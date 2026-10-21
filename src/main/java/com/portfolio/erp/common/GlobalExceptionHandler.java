package com.portfolio.erp.common;

import java.util.*;
import jakarta.servlet.http.*;
import jakarta.validation.ConstraintViolationException;
import com.portfolio.erp.common.error.*;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class GlobalExceptionHandler {
    private final UserErrors errors;
    public GlobalExceptionHandler(UserErrors errors) { this.errors = errors; }

    @ExceptionHandler(Exception.class)
    public Object handle(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        ErrorCode code = errors.classify(ex);
        if (ex instanceof BindException || ex instanceof ConstraintViolationException || ex instanceof org.springframework.web.method.annotation.HandlerMethodValidationException) code = ErrorCode.VALIDATION_FAILED;
        else if (ex instanceof MethodArgumentTypeMismatchException || ex instanceof org.springframework.http.converter.HttpMessageNotReadableException
                || ex instanceof org.springframework.web.bind.MissingServletRequestParameterException) code = ErrorCode.BAD_REQUEST;
        else if (ex instanceof NoResourceFoundException || ex instanceof NoHandlerFoundException) code = ErrorCode.NOT_FOUND;
        else if (ex instanceof HttpRequestMethodNotSupportedException) code = ErrorCode.METHOD_NOT_ALLOWED;
        else if (ex instanceof ResponseStatusException status) code = statusCode(status.getStatusCode().value());
        else if (ex instanceof org.springframework.web.HttpMediaTypeNotSupportedException) code = ErrorCode.UNSUPPORTED_MEDIA_TYPE;
        else if (ex instanceof org.springframework.web.HttpMediaTypeNotAcceptableException) code = ErrorCode.NOT_ACCEPTABLE;
        var notice = errors.report(code, ex, request);
        response.setHeader("Cache-Control", "no-store");
        if (wantsJson(request)) return ResponseEntity.status(code.getStatus()).body(notice);
        var view = new ModelAndView("common/error");
        view.setStatus(org.springframework.http.HttpStatus.valueOf(code.getStatus()));
        view.addObject("errorNotice", notice);
        if (ex instanceof BindException bind) view.addObject("fieldMessages", bind.getFieldErrors().stream().map(errors::fieldMessage).toList());
        // Redisplay the search form without querying the DB when its input is invalid.
        if (ex instanceof BindException bind && bind.getTarget() instanceof PageQuery query
                && ("/books".equals(request.getRequestURI()) || "/partners".equals(request.getRequestURI()))) {
            view.setViewName(request.getRequestURI().equals("/books") ? "book/list" : "partner/list");
            view.addAllObjects(bind.getBindingResult().getModel());
            view.addObject("fieldMessages", bind.getFieldErrors().stream().map(errors::fieldMessage).toList());
            var safePage = new PageQuery();
            view.addObject("result", new PageResult<>(List.of(), 0, safePage));
        }
        return view;
    }
    public static boolean wantsJson(HttpServletRequest request) {
        return "/health".equals(request.getRequestURI()) ||
                Optional.ofNullable(request.getHeader("Accept")).orElse("").contains("application/json");
    }
    public static ErrorCode statusCode(int status) {
        return switch (status) {
            case 400 -> ErrorCode.BAD_REQUEST;
            case 401 -> ErrorCode.LOGIN_REQUIRED;
            case 403 -> ErrorCode.ACCESS_DENIED;
            case 404 -> ErrorCode.NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 406 -> ErrorCode.NOT_ACCEPTABLE;
            case 409 -> ErrorCode.BUSINESS_RULE;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case 503 -> ErrorCode.SERVICE_UNAVAILABLE;
            default -> status >= 400 && status < 500 ? ErrorCode.BAD_REQUEST : ErrorCode.SERVER_ERROR;
        };
    }
}
