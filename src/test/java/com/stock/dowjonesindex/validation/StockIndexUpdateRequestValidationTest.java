package com.stock.dowjonesindex.validation;

import com.stock.dowjonesindex.dto.StockIndexUpdateRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StockIndexUpdateRequestValidationTest {

    @Test
    void invalidDateFormat_returnsFieldViolation() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            StockIndexUpdateRequest request = new StockIndexUpdateRequest(
                    String.valueOf(2),
                    "AA",
                    "1/5/2015",
                    String.valueOf(16.71),
                    String.valueOf(16.71),
                    String.valueOf(15.64),
                    String.valueOf(15.97),
                    String.valueOf(242963398L)
            );

            String violations = validator.validate(request).stream()
                    .map(v -> v.getPropertyPath() + ":" + v.getMessage())
                    .collect(Collectors.joining(", "));

            assertTrue(violations.contains("date"), violations);
        }
    }
}
