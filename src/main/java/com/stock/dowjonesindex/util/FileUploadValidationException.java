package com.stock.dowjonesindex.util;

public class FileUploadValidationException extends RuntimeException{
    private final int rowNumber;
    private final String columnName;
    private final String invalidValue;

    public FileUploadValidationException(int rowNumber, String columnName, String invalidValue,String message) {
        super(message);
        this.rowNumber = rowNumber;
        this.columnName = columnName;
        this.invalidValue = invalidValue;
    }
    public int getRowNumber() {
        return rowNumber;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getInvalidValue() {
        return invalidValue;
    }
}
