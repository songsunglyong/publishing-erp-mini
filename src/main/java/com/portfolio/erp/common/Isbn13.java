package com.portfolio.erp.common;

import java.lang.annotation.*;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Isbn13Validator.class)
public @interface Isbn13 {
    String message() default "{validation.isbn}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
