package com.stock.dowjonesindex.controller;

import com.stock.dowjonesindex.util.StockIndexResponse;
import com.stock.dowjonesindex.util.ValidationErrorResult;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final List<String> STOCK_UPDATE_FIELDS = List.of(
            "quarter", "stock", "date", "open", "high", "low", "close", "volume"
    );
    private static final String STOCK_UPDATE_TYPE_DETAIL =
            "open/high/low/close must be numbers; quarter must be integer; volume must be integer; date must match M/dd/yyyy";

    /**
     * Converts bean-validation errors (e.g. {@code @NotBlank}, {@code @Pattern}, custom validators) into a consistent
     * JSON payload. This project returns HTTP 200 for validation errors and exposes per-field messages.
     *
     * @param ex validation exception thrown by Spring MVC during request binding/validation
     * @return JSON response containing {@link ValidationErrorResult}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StockIndexResponse<ValidationErrorResult>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String field = error.getField();
            String message = error.getDefaultMessage();
            if (message == null || message.isBlank()) message = "Invalid value";
            fieldErrors.computeIfAbsent(field, k -> new ArrayList<>()).add(message);
        }

        List<String> errorFields = new ArrayList<>(fieldErrors.keySet());
        String detail = errorFields.isEmpty()
                ? "One or more fields are invalid"
                : ("Invalid field(s): " + errorFields);

        return ResponseEntity.ok().body(
                new StockIndexResponse<>(
                        "SUCCESS",
                        "Invalid request body",
                        0,
                        new ValidationErrorResult(errorFields, fieldErrors, detail)
                )
        );
    }

    /**
     * Handles malformed JSON or type mismatches while parsing the request body and returns a consistent JSON payload.
     *
     * @param ex thrown when JSON cannot be parsed into the expected request type
     * @return JSON response containing {@link ValidationErrorResult}
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StockIndexResponse<ValidationErrorResult>> handleNotReadable(HttpMessageNotReadableException ex) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put("_error", List.of("Invalid request body (JSON parse/type mismatch)"));
        fieldErrors.put("_fields", STOCK_UPDATE_FIELDS);

        return ResponseEntity.ok().body(
                new StockIndexResponse<>(
                        "SUCCESS",
                        "Invalid request body",
                        0,
                        new ValidationErrorResult(List.of("_error"), fieldErrors, STOCK_UPDATE_TYPE_DETAIL)
                )
        );
    }

    /**
     * Handles numeric parsing errors when mapping request strings into numeric fields.
     *
     * @param ex number format exception
     * @return JSON response containing {@link ValidationErrorResult}
     */
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<StockIndexResponse<ValidationErrorResult>> handleNumberFormat(NumberFormatException ex) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put("_error", List.of("Invalid request body (JSON parse/type mismatch)"));
        fieldErrors.put("_fields", STOCK_UPDATE_FIELDS);
        return ResponseEntity.ok().body(
                new StockIndexResponse<>(
                        "SUCCESS",
                        "Invalid request body",
                        0,
                        new ValidationErrorResult(List.of("_error"), fieldErrors, STOCK_UPDATE_TYPE_DETAIL)
                )
        );
    }
}
