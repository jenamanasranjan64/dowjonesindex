package com.stock.dowjonesindex.util;
import lombok.Data;
@Data
public class StockIndexResponse<T> {
    private String status;
    private String message;
    private int rowsAffected;
    private T data;

    public StockIndexResponse(String status, String message, int rowsAffected, T data) {
        this.status = status;
        this.message = message;
        this.rowsAffected = rowsAffected;
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getRowsAffected() {
        return rowsAffected;
    }

    public void setRowsAffected(int rowsAffected) {
        this.rowsAffected = rowsAffected;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
