package com.stock.dowjonesindex.controller;
import com.stock.dowjonesindex.dto.StockIndexUpdateRequest;
import com.stock.dowjonesindex.model.StockIndexRecord;
import com.stock.dowjonesindex.service.StockIndexServiceInterFace;
import com.stock.dowjonesindex.util.DeleteResult;
import com.stock.dowjonesindex.util.ErrorCodes;
import com.stock.dowjonesindex.util.ErrorResult;
import com.stock.dowjonesindex.util.FileUploadResponse;
import com.stock.dowjonesindex.util.StockIndexResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
@RestController
@RequestMapping("/api/stock-data")
public class StockIndexController {
    @Autowired
    private StockIndexServiceInterFace stockIndexServiceInterFace;
    private static final DateTimeFormatter UPDATE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("M/dd/uuuu", Locale.US).withResolverStyle(ResolverStyle.STRICT);

    /**
     * Uploads stock index data as a CSV or XLSX file and processes it row-by-row.
     * <p>
     * Returns a consistent JSON wrapper ({@link StockIndexResponse}) for both success and validation failures.
     * When every data row is inserted successfully, the endpoint responds with HTTP 201.
     *
     * @param file multipart file containing CSV/XLSX data
     * @return JSON response containing {@link FileUploadResponse} or an {@link ErrorResult}
     */
    @PostMapping("/upload")
    public ResponseEntity<StockIndexResponse<Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.ok(new StockIndexResponse<>(
                    "SUCCESS",
                    "Invalid file upload",
                    0,
                    new ErrorResult(ErrorCodes.INVALID_REQUEST, "File is required and must not be empty")
            ));
        }

        String fileName = file.getOriginalFilename();
        String extension = fileExtension(fileName);
        if (!"csv".equals(extension) && !"xlsx".equals(extension)) {
            return ResponseEntity.ok(new StockIndexResponse<>(
                    "SUCCESS",
                    "Invalid file upload",
                    0,
                    new ErrorResult(
                            ErrorCodes.INVALID_REQUEST,
                            "Unsupported file type; only .csv and .xlsx are supported"
                    )
            ));
        }

        try {
            FileUploadResponse fileUploadResponse = stockIndexServiceInterFace.processUpload(file);
            int rowsAffected = fileUploadResponse == null ? 0 : fileUploadResponse.getInsertedRows();
            StockIndexResponse<Object> body = new StockIndexResponse<>(
                    "SUCCESS",
                    "File processed successfully",
                    rowsAffected,
                    fileUploadResponse
            );

            if (fileUploadResponse != null
                    && fileUploadResponse.getTotalRows() > 0
                    && fileUploadResponse.getFailedRows() == 0
                    && fileUploadResponse.getInsertedRows() == fileUploadResponse.getTotalRows()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(body);
            }
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(new StockIndexResponse<>(
                    "SUCCESS",
                    "Invalid file upload",
                    0,
                    new ErrorResult(ErrorCodes.INVALID_REQUEST, e.getMessage())
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(new StockIndexResponse<>(
                    "SUCCESS",
                    "File processing failed",
                    0,
                    new ErrorResult(ErrorCodes.INVALID_REQUEST, e.getMessage() == null ? "Unknown error" : e.getMessage())
            ));
        }
    }

    private static String fileExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
    }
    // ========================
    // SELECT ALL
    // ========================
    /**
     * Fetches all stock records.
     *
     * @return JSON response containing a list of {@link StockIndexRecord}
     */
    @GetMapping("/allStockRecord")
    public ResponseEntity<StockIndexResponse<List<StockIndexRecord>>> getAll() {
        try {
            StockIndexResponse<List<StockIndexRecord>> response = stockIndexServiceInterFace.getAllStocks();
            if (response == null) {
                return ResponseEntity.ok(new StockIndexResponse<>("SUCCESS", "No Stock records found", 0, List.of()));
            }
            if (response.getData() == null) {
                response.setData(List.of());
            }
            if (response.getRowsAffected() == 0 && "SUCCESS".equalsIgnoreCase(response.getStatus())) {
                response.setMessage("No Stock records found");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(new StockIndexResponse<>("SUCCESS", "No Stock records found", 0, List.of()));
        }
    }

    // ========================
    // SELECT BY ID
    // ========================
    /**
     * Fetches a single stock record by numeric id.
     *
     * @param id record id
     * @return JSON response containing a {@link StockIndexRecord} or an {@link ErrorResult} when not found
     */
    @GetMapping("/id/{id:\\d+}")
    public ResponseEntity<StockIndexResponse<Object>> getById(@PathVariable Long id) {
        Optional<StockIndexRecord> record = stockIndexServiceInterFace.findById(id);
        if (record.isEmpty()) {
            return ResponseEntity.ok(
                    new StockIndexResponse<>(
                            "SUCCESS",
                            "No record found",
                            0,
                            new ErrorResult(ErrorCodes.NO_RECORD_FOUND, "No stock record found with id: " + id)
                    )
            );
        }
        return ResponseEntity.ok(
                new StockIndexResponse<>("SUCCESS", "Stock Record fetched successfully", 1, record.get())
        );
    }

    // ========================
    // SELECT BY STOCK
    // ========================
    /**
     * Fetches records by stock symbol/ticker (e.g. AA, AAPL, BRK.B).
     *
     * @param stock stock symbol (route only matches non-numeric identifiers)
     * @return JSON response containing a list of matching {@link StockIndexRecord}
     */
    @GetMapping("/stock/{stock:[A-Za-z][A-Za-z0-9._-]*}")
    public ResponseEntity<StockIndexResponse<List<StockIndexRecord>>> getByStock(@PathVariable String stock) {
        StockIndexResponse<List<StockIndexRecord>> response = stockIndexServiceInterFace.findByStock(stock);
        return ResponseEntity.ok(response);
    }
    // ========================
    // UPDATE BY ID
    // ========================
    /**
     * Updates a stock record by id.
     * <p>
     * Request payload values are validated and parsed into domain types. The endpoint returns HTTP 200 even when
     * the id is not found, using a non-null {@link ErrorResult} payload.
     *
     * @param id      record id
     * @param updated validated request body
     * @return JSON response containing updated record or an {@link ErrorResult} when not found/invalid
     */
    @PutMapping("/id/{id}")
    public ResponseEntity<StockIndexResponse<Object>> updateById(@PathVariable Long id,
            @Valid @RequestBody StockIndexUpdateRequest updated) {
        StockIndexRecord mapped = new StockIndexRecord();
        mapped.setQuarter(Integer.parseInt(updated.quarter()));
        mapped.setStock(updated.stock());
        mapped.setDate(LocalDate.parse(updated.date(), UPDATE_DATE_FORMAT));
        mapped.setOpen(Double.parseDouble(updated.open()));
        mapped.setHigh(Double.parseDouble(updated.high()));
        mapped.setLow(Double.parseDouble(updated.low()));
        mapped.setClose(Double.parseDouble(updated.close()));
        mapped.setVolume(Long.parseLong(updated.volume()));
        StockIndexResponse<Object> response = stockIndexServiceInterFace.updateById(id, mapped);
        return ResponseEntity.ok(response);
    }

    // ========================
    // DELETE BY ID
    // ========================
    /**
     * Deletes a stock record by id.
     * <p>
     * Always returns HTTP 200 and returns a non-null {@link DeleteResult} in the response data.
     *
     * @param id record id
     * @return JSON response containing {@link DeleteResult}
     */
    @DeleteMapping("/deleteId/{id}")
    public ResponseEntity<StockIndexResponse<DeleteResult>> deleteById(@PathVariable Long id) {
        StockIndexResponse<DeleteResult> response = stockIndexServiceInterFace.deleteById(id);
        return ResponseEntity.ok(response);
    }
    // ========================
    // BULK DELETE BY IDS
    // ========================
    /**
     * Deletes multiple records by ids.
     * <p>
     * Always returns HTTP 200. For invalid requests or missing ids, the response includes an {@link ErrorResult}
     * payload describing the issue.
     *
     * @param ids list of ids to delete (may be null/empty)
     * @return JSON response containing bulk delete result or an {@link ErrorResult}
     */
    @DeleteMapping("/bulk-delete")
    public ResponseEntity<StockIndexResponse<Object>> bulkDeleteByIds(@RequestBody(required = false) List<Long> ids) {
        StockIndexResponse<Object> response = stockIndexServiceInterFace.bulkDeleteByIds(ids);
        if (response == null) {
            return ResponseEntity.ok(new StockIndexResponse<>(
                    "SUCCESS",
                    "No ids provided",
                    0,
                    new ErrorResult(ErrorCodes.INVALID_REQUEST, "No ids provided")
            ));
        }
        return ResponseEntity.ok(response);
    }

}
