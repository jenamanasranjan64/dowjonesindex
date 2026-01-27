package com.stock.dowjonesindex.validation;

import jakarta.validation.Payload;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidDateValidatorTest {

    private static ValidDate validDateAnnotation(String pattern) {
        return new ValidDate() {
            @Override
            public String pattern() {
                return pattern;
            }

            @Override
            public String message() {
                return "invalid date";
            }

            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ValidDate.class;
            }
        };
    }

    @Test
    void isValid_allowsNullAndBlank() {
        ValidDateValidator validator = new ValidDateValidator();
        validator.initialize(validDateAnnotation("M/dd/yyyy"));

        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("", null));
        assertTrue(validator.isValid("   ", null));
    }

    @Test
    void isValid_enforcesMddyyyyWithTwoDigitDay_andStrictParsing() {
        ValidDateValidator validator = new ValidDateValidator();
        validator.initialize(validDateAnnotation("M/dd/yyyy"));

        assertTrue(validator.isValid("1/05/2015", null));
        assertTrue(validator.isValid(" 1/05/2015 ", null));
        assertFalse(validator.isValid("1/5/2015", null));
        assertFalse(validator.isValid("2/29/2015", null));
        assertFalse(validator.isValid("1/32/2015", null));
    }

    @Test
    void isValid_rewritesYyyyToUuuuForStrictYearParsing() {
        ValidDateValidator validator = new ValidDateValidator();
        validator.initialize(validDateAnnotation("yyyy-MM-dd"));

        assertTrue(validator.isValid("2011-01-14", null));
        assertFalse(validator.isValid("2011-02-29", null));
        assertFalse(validator.isValid("not-a-date", null));
    }
}
