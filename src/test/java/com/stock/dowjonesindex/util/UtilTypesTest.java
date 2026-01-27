package com.stock.dowjonesindex.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UtilTypesTest {

    @Test
    void fileUploadValidationException_exposesMessageAndFields() {
        FileUploadValidationException ex = new FileUploadValidationException(12, "close", "abc", "invalid close");
        assertEquals("invalid close", ex.getMessage());
        assertEquals(12, ex.getRowNumber());
        assertEquals("close", ex.getColumnName());
        assertEquals("abc", ex.getInvalidValue());
    }

    @Test
    void stockIndexResponse_constructorAndSetters() {
        StockIndexResponse<String> response = new StockIndexResponse<>("SUCCESS", "ok", 1, "payload");
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("ok", response.getMessage());
        assertEquals(1, response.getRowsAffected());
        assertEquals("payload", response.getData());

        response.setStatus("ERROR");
        response.setMessage("bad");
        response.setRowsAffected(0);
        response.setData("x");
        assertEquals("ERROR", response.getStatus());
        assertEquals("bad", response.getMessage());
        assertEquals(0, response.getRowsAffected());
        assertEquals("x", response.getData());
    }

    @Test
    void fileUploadResponse_defaultFailuresList_andSetters() {
        FileUploadResponse response = new FileUploadResponse();
        assertNotNull(response.getFailures());
        assertEquals(0, response.getFailures().size());

        response.setTotalRows(10);
        response.setInsertedRows(7);
        response.setFailedRows(3);
        response.setFailedRowRecords(List.of(new FailedRowRecord(2, "open", "x", "invalid")));

        assertEquals(10, response.getTotalRows());
        assertEquals(7, response.getInsertedRows());
        assertEquals(3, response.getFailedRows());
        assertEquals(1, response.getFailedRowRecords().size());
        assertEquals(2, response.getFailedRowRecords().get(0).getRowNumber());
    }
}

