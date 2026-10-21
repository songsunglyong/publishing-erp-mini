package com.portfolio.erp.common.error;

public class ErrorNotice {
    private final String code, message, traceId;
    public ErrorNotice(ErrorCode code, String message, String traceId) {
        this.code = code.name(); this.message = message; this.traceId = traceId;
    }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public String getTraceId() { return traceId; }
}
