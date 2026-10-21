package com.portfolio.erp.common.error;

/** Only an approved code crosses the boundary into the user interface. */
public class UserFacingException extends RuntimeException {
    private final ErrorCode code;
    public UserFacingException(ErrorCode code) { super(code.name()); this.code = code; }
    public ErrorCode getCode() { return code; }
}
