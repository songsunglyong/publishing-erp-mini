package com.portfolio.erp.common;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class Isbn13Validator implements ConstraintValidator<Isbn13, String> {
    @Override public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        if (!value.matches("97[89][0-9]{10}")) return false;
        int sum = 0;
        for (int i = 0; i < 13; i++) sum += (value.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        return sum % 10 == 0;
    }
}
