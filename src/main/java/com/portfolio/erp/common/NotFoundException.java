package com.portfolio.erp.common;

import com.portfolio.erp.common.error.*;

public class NotFoundException extends UserFacingException {
    public NotFoundException() { super(ErrorCode.NOT_FOUND); }
}
