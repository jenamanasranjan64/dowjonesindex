package com.stock.dowjonesindex.util;

public class FailedRowRecord {
    private int rowNumber;
    private String column;
    private String invalidValue;
    private String message;

    public FailedRowRecord(int rowNumber, String column, String invalidValue, String message) {
        this.rowNumber = rowNumber;
        this.column = column;
        this.invalidValue = invalidValue;
        this.message = message;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getInvalidValue() {
        return invalidValue;
    }

    public void setInvalidValue(String invalidValue) {
        this.invalidValue = invalidValue;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
