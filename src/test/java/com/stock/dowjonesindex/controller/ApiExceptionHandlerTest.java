package com.stock.dowjonesindex.controller;

import com.stock.dowjonesindex.dto.StockIndexUpdateRequest;
import com.stock.dowjonesindex.util.StockIndexResponse;
import com.stock.dowjonesindex.util.ValidationErrorResult;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiExceptionHandlerTest {

    private static final class DummyController {
        @SuppressWarnings("unused")
        void update(StockIndexUpdateRequest request) {}
    }

    @Test
    void handleValidation_mapsFieldErrors_andUsesFallbackMessage() throws Exception {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        StockIndexUpdateRequest request = new StockIndexUpdateRequest("", "", "", "", "", "", "", "");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "request");
        bindingResult.addError(new FieldError("request", "quarter", "quarter is required"));
        bindingResult.addError(new FieldError("request", "date", null, false, null, null, null));

        Method method = DummyController.class.getDeclaredMethod("update", StockIndexUpdateRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<StockIndexResponse<ValidationErrorResult>> response = handler.handleValidation(ex);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().getStatus());
        assertEquals("Invalid request body", response.getBody().getMessage());
        assertEquals(0, response.getBody().getRowsAffected());

        ValidationErrorResult data = response.getBody().getData();
        assertEquals(List.of("quarter", "date"), data.errorFields());
        assertEquals(Map.of("quarter", List.of("quarter is required"), "date", List.of("Invalid value")), data.fieldErrors());
        assertEquals("Invalid field(s): [quarter, date]", data.detail());
    }

    @Test
    void handleNotReadable_returnsConsistentPayload() {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        HttpInputMessage inputMessage = new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };
        ResponseEntity<StockIndexResponse<ValidationErrorResult>> response =
                handler.handleNotReadable(new HttpMessageNotReadableException("bad json", inputMessage));

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().getStatus());
        assertEquals("Invalid request body", response.getBody().getMessage());
        assertEquals(0, response.getBody().getRowsAffected());

        ValidationErrorResult data = response.getBody().getData();
        assertEquals(List.of("_error"), data.errorFields());
        assertEquals(List.of("Invalid request body (JSON parse/type mismatch)"), data.fieldErrors().get("_error"));
        assertEquals(
                List.of("quarter", "stock", "date", "open", "high", "low", "close", "volume"),
                data.fieldErrors().get("_fields")
        );
        assertEquals(
                "open/high/low/close must be numbers; quarter must be integer; volume must be integer; date must match M/dd/yyyy",
                data.detail()
        );
    }

    @Test
    void handleNumberFormat_returnsSamePayloadAsNotReadable() {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        ResponseEntity<StockIndexResponse<ValidationErrorResult>> response =
                handler.handleNumberFormat(new NumberFormatException("NaN"));

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().getStatus());
        assertEquals("Invalid request body", response.getBody().getMessage());

        ValidationErrorResult data = response.getBody().getData();
        assertEquals(List.of("_error"), data.errorFields());
        assertEquals(List.of("Invalid request body (JSON parse/type mismatch)"), data.fieldErrors().get("_error"));
        assertEquals(
                List.of("quarter", "stock", "date", "open", "high", "low", "close", "volume"),
                data.fieldErrors().get("_fields")
        );
    }
}
