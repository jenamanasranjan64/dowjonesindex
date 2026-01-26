package com.stock.dowjonesindex.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ValidDateValidator implements ConstraintValidator<ValidDate, String> {
    private DateTimeFormatter formatter;
    private Pattern pattern;

    @Override
    public void initialize(ValidDate constraintAnnotation) {
        String configuredPattern = constraintAnnotation.pattern();
        String effectivePattern = configuredPattern;
        if (effectivePattern.contains("yyyy") && !effectivePattern.contains("uuuu")) {
            effectivePattern = effectivePattern.replace("yyyy", "uuuu");
        }
        formatter = DateTimeFormatter.ofPattern(effectivePattern, Locale.US)
                .withResolverStyle(ResolverStyle.STRICT);

        if ("M/dd/yyyy".equals(configuredPattern) || "M/dd/uuuu".equals(configuredPattern)) {
            pattern = Pattern.compile("^\\d{1,2}/\\d{2}/\\d{4}$");
        } else {
            pattern = null;
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true;
        String trimmed = value.trim();
        if (pattern != null && !pattern.matcher(trimmed).matches()) return false;
        try {
            LocalDate.parse(trimmed, formatter);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
