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
