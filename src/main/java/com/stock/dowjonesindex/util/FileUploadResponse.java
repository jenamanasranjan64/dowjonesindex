package com.stock.dowjonesindex.util;
import java.util.ArrayList;
import java.util.List;
public class FileUploadResponse {
    public int totalRows;
    public int insertedRows;
    public int failedRows;
    public List<FailedRowRecord> failedRowRecords;
//    public List<RowFailure> failures = new ArrayList<>();
//
//    public List<RowFailure> getFailures() {
//        return failures;
//    }
//
//    public void setFailures(List<RowFailure> failures) {
//        this.failures = failures == null ? new ArrayList<>() : failures;
//    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getInsertedRows() {
        return insertedRows;
    }

    public void setInsertedRows(int insertedRows) {
        this.insertedRows = insertedRows;
    }

    public int getFailedRows() {
        return failedRows;
    }

    public void setFailedRows(int failedRows) {
        this.failedRows = failedRows;
    }

    public List<FailedRowRecord> getFailedRowRecords() {
        return failedRowRecords;
    }

    public void setFailedRowRecords(List<FailedRowRecord> failedRowRecords) {
        this.failedRowRecords = failedRowRecords;
    }
}
