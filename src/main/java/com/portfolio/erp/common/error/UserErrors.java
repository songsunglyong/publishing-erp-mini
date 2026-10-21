package com.portfolio.erp.common.error;

import java.util.Locale;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.dao.*;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

@Component
public class UserErrors {
    private static final Logger log = LoggerFactory.getLogger(UserErrors.class);
    private final MessageSource messages;
    public UserErrors(MessageSource messages) { this.messages = messages; }
    public String message(String key) { return messages.getMessage(key, null, Locale.KOREAN); }
    public ErrorCode classify(Throwable ex) {
        if (ex instanceof UserFacingException known) return known.getCode();
        if (ex instanceof DuplicateKeyException) return ErrorCode.DUPLICATE_DATA;
        if (ex instanceof org.springframework.security.access.AccessDeniedException || ex instanceof SecurityException) return ErrorCode.ACCESS_DENIED;
        // MyBatis may wrap the JDBC connection failure; inspect types/SQL state, never message text.
        Throwable cause = ex;
        for (int depth = 0; cause != null && depth < 8; depth++, cause = cause.getCause()) {
            if (cause instanceof DataAccessResourceFailureException || cause instanceof TransientDataAccessException
                    || cause instanceof java.sql.SQLTransientConnectionException
                    || cause instanceof java.sql.SQLNonTransientConnectionException
                    || cause instanceof java.sql.SQLRecoverableException
                    || (cause instanceof java.sql.SQLException sql && sql.getSQLState() != null
                        && sql.getSQLState().startsWith("08"))) return ErrorCode.SERVICE_UNAVAILABLE;
        }
        // Unknown constraints/SQL failures are not guessed to be a business rule.
        return ErrorCode.SERVER_ERROR;
    }
    public ErrorNotice report(ErrorCode code, Throwable ex, HttpServletRequest request) {
        String id = (String) request.getAttribute(TraceIdFilter.ATTRIBUTE);
        if (id == null) { id = UUID.randomUUID().toString(); request.setAttribute(TraceIdFilter.ATTRIBUTE, id); }
        var notice = new ErrorNotice(code, message(code.getMessageKey()), id);
        request.setAttribute("errorNotice", notice);
        // Allowlisted diagnostics only: throwable messages, causes, SQL and submitted values can contain secrets.
        String type = ex == null ? "none" : ex.getClass().getName();
        String location = ex == null || ex.getStackTrace().length == 0 ? "none" : ex.getStackTrace()[0].toString();
        log.warn("user_error code={} traceId={} exceptionType={} location={}", code, id, type, location);
        return notice;
    }
    public String fieldMessage(org.springframework.validation.FieldError field) {
        if (field.isBindingFailure()) {
            return messages.getMessage("typeMismatch." + field.getObjectName() + "." + field.getField(),
                    null, message("typeMismatch"), Locale.KOREAN);
        }
        return messages.getMessage(field, Locale.KOREAN);
    }
    public void validation(BindingResult errors, HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        report(ErrorCode.VALIDATION_FAILED, null, request);
        response.setHeader("Cache-Control", "no-store");
    }
    public void formFailure(RuntimeException ex, BindingResult errors, String duplicateField,
            ErrorCode duplicateCode, HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        ErrorCode code = ex instanceof DuplicateKeyException ? duplicateCode : classify(ex);
        ErrorNotice notice = report(code, ex, request);
        if (ex instanceof DuplicateKeyException) errors.rejectValue(duplicateField, code.name(), notice.getMessage());
        // Retain the same bound form, including rejected raw values. Never redirect or store it in the session.
        // Expected input/duplicate errors keep the existing 200 form contract; service failures are 5xx.
        if (code.getStatus() >= 500 || code.getStatus() == 401 || code.getStatus() == 403) response.setStatus(code.getStatus());
        response.setHeader("Cache-Control", "no-store");
    }
}
