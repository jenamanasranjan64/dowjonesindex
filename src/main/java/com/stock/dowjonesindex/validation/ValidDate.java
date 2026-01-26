package com.stock.dowjonesindex.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = ValidDateValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
public @interface ValidDate {
    String message() default "Invalid date";
    String pattern();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

