package com.stock.dowjonesindex.service;
import com.opencsv.bean.CsvToBeanBuilder;
import com.stock.dowjonesindex.model.StockIndexCsvDto;
import com.stock.dowjonesindex.model.StockIndexRecord;
import com.stock.dowjonesindex.repository.StockRepository;
import com.stock.dowjonesindex.util.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
/**
 * Service implementation for stock index operations.
 * <p>
 * Responsibilities:
 * - CRUD-style operations used by the REST controller.
 * - CSV/XLSX upload parsing, row-level validation, and persistence.
 * - Duplicate (stock,date) handling so valid rows are inserted while duplicates are reported as row errors.
 */
public class StockIndexServiceImpl implements StockIndexServiceInterFace {
    private final StockRepository stockRepository;
    private static final DataFormatter EXCEL_FORMATTER = new DataFormatter(Locale.US);
    public StockIndexServiceImpl(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    /**
     * Processes a user-uploaded file and persists valid records.
     * <p>
     * Supported formats: {@code .csv} and {@code .xlsx}. The method prefers file extension routing (from
     * {@link MultipartFile#getOriginalFilename()}) and falls back to content-type when needed.
     *
     * @param multipartFile upload file
     * @return upload summary including inserted/failed counts and per-row error details
     */
    @Override
    public FileUploadResponse processUpload(MultipartFile multipartFile) throws Exception {
        String fileName = multipartFile.getOriginalFilename();
        String extension = fileExtension(fileName);
        if ("csv".equals(extension)) {
            return parseCSV(multipartFile);
        }
        if ("xlsx".equals(extension)) {
            try (InputStream inputStream = multipartFile.getInputStream()) {
                return parseExcel(inputStream);
            }
        }

        String contentType = multipartFile.getContentType();
        if ("text/csv".equalsIgnoreCase(contentType)) {
            return parseCSV(multipartFile);
        }
        if ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equalsIgnoreCase(contentType)) {
            try (InputStream inputStream = multipartFile.getInputStream()) {
                return parseExcel(inputStream);
            }
        }
        throw new IllegalArgumentException("Unsupported file type; only .csv and .xlsx are supported");
    }

    /**
     * Extracts and normalizes the file extension.
     *
     * @param fileName original filename
     * @return extension without dot (lower-cased) or empty string when missing
     */
    private static String fileExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Parses an XLSX stream, validates each row, and persists non-duplicate rows.
     * <p>
     * - Skips the header row (row 1).
     * - Skips fully-empty rows (common with formatted but empty Excel sheets).
     * - Collects validation issues into both {@code failures} and {@code failedRowRecords}.
     *
     * @param inputStream XLSX input stream
     * @return upload summary
     */
    FileUploadResponse parseExcel(InputStream inputStream) throws IOException {
        FileUploadResponse fileUploadResponse = new FileUploadResponse();
        List<FailedRowRecord> failedRowRecords = new ArrayList<>();
        Set<String> seenStockDateKeys = new LinkedHashSet<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                int rowNumber = row.getRowNum() + 1; // 1-based, matches Excel display
                if (rowNumber == 1) continue; // skip header
                if (isExcelRowEmpty(row)) continue; // skip trailing/empty rows
                RowFailure failure = new RowFailure();
                failure.rowNumber = rowNumber;
                try {
                    StockIndexRecord stockIndexRecord = parseAndValidate(row, failure);
                    if (failure.columnErrors.isEmpty()) {
                        String duplicateKey = stockDateKey(stockIndexRecord);
                        if (duplicateKey != null && !seenStockDateKeys.add(duplicateKey)) {
                            failure.columnErrors.put("stock", "Duplicate stock/date in upload: " + duplicateKey);
                            fileUploadResponse.failedRows++;
                            fileUploadResponse.failures.add(failure);
                            failedRowRecords.add(new FailedRowRecord(rowNumber, "stock", duplicateKey,
                                    "Duplicate stock/date in upload"));
                            continue;
                        }
                        stockRepository.save(stockIndexRecord);
                        fileUploadResponse.insertedRows++;
                    } else {
                        fileUploadResponse.failedRows++;
                        fileUploadResponse.failures.add(failure);
                        for (var entry : failure.columnErrors.entrySet()) {
                            String col = entry.getKey();
                            String msg = entry.getValue();
                            String invalidValue = valueForExcelColumn(row, col);
                            failedRowRecords.add(new FailedRowRecord(rowNumber, col, invalidValue, msg));
                        }
                    }
                } catch (Exception e) {
                    if (isDuplicateStockDateException(e)) {
                        String duplicateKey = stockDateKeyFromRow(row);
                        failure.columnErrors.put("stock", "Duplicate stock/date already exists: " + duplicateKey);
                        fileUploadResponse.failedRows++;
                        fileUploadResponse.failures.add(failure);
                        failedRowRecords.add(new FailedRowRecord(rowNumber, "stock", duplicateKey,
                                "Duplicate stock/date already exists"));
                    } else {
                        failure.columnErrors.put("row", e.getMessage());
                        fileUploadResponse.failedRows++;
                        fileUploadResponse.failures.add(failure);
                        failedRowRecords.add(new FailedRowRecord(rowNumber, "row", null, e.getMessage()));
                    }
                }
            }
            fileUploadResponse.totalRows = fileUploadResponse.insertedRows + fileUploadResponse.failedRows;
            fileUploadResponse.setFailedRowRecords(failedRowRecords);
            return fileUploadResponse;
        }
    }
    /**
     * Attempts to parse a date value using supported formats.
     *
     * @param value date string (may be null/blank)
     * @return parsed date or {@code null} when invalid/blank
     */
    public  LocalDate safeParseDate(String value) {

        if (value == null || value.isBlank()) return null;

        DateTimeFormatter[] formats = {
                DateTimeFormatter.ofPattern("M/d/yyyy"),DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ISO_LOCAL_DATE
        };

        for (DateTimeFormatter f : formats) {
            try {
                return LocalDate.parse(value.trim(), f);
            } catch (Exception e) {
                // try next format
            }
        }

        return null; // invalid date
    }
    /**
     * Validates and maps an Excel row into a {@link StockIndexRecord}.
     * <p>
     * Validation errors are recorded in {@link RowFailure#columnErrors}. Callers should check
     * {@code columnErrors.isEmpty()} before persisting.
     */
    private StockIndexRecord parseAndValidate(Row row, RowFailure failure) {
        StockIndexRecord stockIndexRecord = new StockIndexRecord();
        // quarter
        Integer quarter = getRequiredInt(row, 0, "quarter", failure);
        if (quarter == null || quarter < 1 || quarter > 4) {
            failure.columnErrors.put("quarter", "Quarter must be 1 to 4");
        } else {
            stockIndexRecord.setQuarter(quarter);
        }
        // stock
        String stock = getRequiredString(row, 1, "stock", failure);
        if (stock != null) {
            stockIndexRecord.setStock(stock);
        }

        // date
        String rawDate = getRequiredString(row, 2, "date", failure);
        if (rawDate != null) {
            LocalDate localDate = safeParseDate(rawDate);
            if (localDate == null) {
                failure.columnErrors.put("date", "Invalid date format");
            } else {
                stockIndexRecord.setDate(localDate);
            }
        }

        Double open = getRequiredDecimal(row, 3, "open", failure);
        if (open != null && open < 0) failure.columnErrors.put("open", "open must be a valid number >= 0");
        stockIndexRecord.setOpen(open);

        Double high = getRequiredDecimal(row, 4, "high", failure);
        if (high != null && high < 0) failure.columnErrors.put("high", "high must be a valid number >= 0");
        stockIndexRecord.setHigh(high);

        Double low = getRequiredDecimal(row, 5, "low", failure);
        if (low != null && low < 0) failure.columnErrors.put("low", "low must be a valid number >= 0");
        stockIndexRecord.setLow(low);

        Double close = getRequiredDecimal(row, 6, "close", failure);
        if (close != null && close < 0) failure.columnErrors.put("close", "close must be a valid number >= 0");
        stockIndexRecord.setClose(close);

        Long volume = getRequiredLong(row, 7, "volume", failure);
        if (volume != null && volume <= 0) failure.columnErrors.put("volume", "volume must be a valid integer > 0");
        if (volume != null) stockIndexRecord.setVolume(volume);

        stockIndexRecord.setPercentChangePrice(getOptionalDecimal(row, 8, "percent_change_price", failure));
        stockIndexRecord.setPercentChangeVolumeOverLastWk(getOptionalDecimal(row, 9, "percent_change_volume_over_last_wk", failure));
        stockIndexRecord.setPreviousWeeksVolume(getOptionalDecimal(row, 10, "previous_weeks_volume", failure));
        stockIndexRecord.setNextWeeksOpen(getOptionalDecimal(row, 11, "next_weeks_open", failure));
        stockIndexRecord.setNextWeeksClose(getOptionalDecimal(row, 12, "next_weeks_close", failure));
        stockIndexRecord.setPercentChangeNextWeeksPrice(getOptionalDecimal(row, 13, "percent_change_next_weeks_price", failure));

        Integer daysToNextDividend = getRequiredInt(row, 14, "days_to_next_dividend", failure);
        if (daysToNextDividend != null && daysToNextDividend < 0) {
            failure.columnErrors.put("days_to_next_dividend", "days_to_next_dividend must be a valid integer >= 0");
        }
        if (daysToNextDividend != null) stockIndexRecord.setDaysToNextDividend(daysToNextDividend);

        Double percentReturnNextDividend = getRequiredDecimal(row, 15, "percent_return_next_dividend", failure);
        if (percentReturnNextDividend != null) stockIndexRecord.setPercentReturnNextDividend(percentReturnNextDividend);
        return stockIndexRecord;
    }

    /**
     * Reads the raw cell value for a given domain column name, used for error reporting.
     */
    private static String valueForExcelColumn(Row row, String column) {
        int index = switch (column) {
            case "quarter" -> 0;
            case "stock" -> 1;
            case "date" -> 2;
            case "open" -> 3;
            case "high" -> 4;
            case "low" -> 5;
            case "close" -> 6;
            case "volume" -> 7;
            case "percent_change_price" -> 8;
            case "percent_change_volume_over_last_wk" -> 9;
            case "previous_weeks_volume" -> 10;
            case "next_weeks_open" -> 11;
            case "next_weeks_close" -> 12;
            case "percent_change_next_weeks_price" -> 13;
            case "days_to_next_dividend" -> 14;
            case "percent_return_next_dividend" -> 15;
            default -> -1;
        };
        if (index < 0) return null;
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        String value = EXCEL_FORMATTER.formatCellValue(cell);
        if (value == null) return null;
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Normalizes numeric strings (e.g. removes {@code $} and {@code ,}) before parsing.
     */
    private static String sanitizeNumeric(String raw) {
        if (raw == null) return null;
        return raw.replace("$", "").replace(",", "").trim();
    }

    /**
     * Returns true when a value is null/blank after trimming.
     */
    private static boolean isBlankCell(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Reads an Excel cell as a trimmed string using {@link DataFormatter}; returns null for empty cells.
     */
    private static String getCellString(Row row, int index) {
        if (row == null) return null;
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        String value = EXCEL_FORMATTER.formatCellValue(cell);
        if (value == null) return null;
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Reads a required String cell; records an error when missing/blank.
     */
    private String getRequiredString(Row row, int index, String col, RowFailure f) {
        String value = getCellString(row, index);
        if (isBlankCell(value)) {
            f.columnErrors.put(col, col + " cannot be null or blank");
            return null;
        }
        return value;
    }

    /**
     * Reads a required integer cell; records an error when missing/blank/invalid.
     */
    private Integer getRequiredInt(Row row, int index, String col, RowFailure f) {
        String raw = getCellString(row, index);
        if (isBlankCell(raw)) {
            f.columnErrors.put(col, col + " cannot be null or blank");
            return null;
        }
        try {
            double parsed = Double.parseDouble(sanitizeNumeric(raw));
            if (Math.floor(parsed) != parsed) throw new NumberFormatException("non-integer");
            return (int) parsed;
        } catch (Exception e) {
            f.columnErrors.put(col, "Invalid integer " + col);
            return null;
        }
    }

    /**
     * Reads a required long cell; records an error when missing/blank/invalid.
     */
    private Long getRequiredLong(Row row, int index, String col, RowFailure f) {
        String raw = getCellString(row, index);
        if (isBlankCell(raw)) {
            f.columnErrors.put(col, col + " cannot be null or blank");
            return null;
        }
        try {
            double parsed = Double.parseDouble(sanitizeNumeric(raw));
            if (Math.floor(parsed) != parsed) throw new NumberFormatException("non-integer");
            return (long) parsed;
        } catch (Exception e) {
            f.columnErrors.put(col, "Invalid long " + col);
            return null;
        }
    }

    /**
     * Reads a required decimal cell; records an error when missing/blank/invalid.
     */
    private Double getRequiredDecimal(Row row, int index, String col, RowFailure f) {
        String raw = getCellString(row, index);
        if (isBlankCell(raw)) {
            f.columnErrors.put(col, col + " cannot be null or blank");
            return null;
        }
        try {
            return Double.parseDouble(sanitizeNumeric(raw));
        } catch (Exception e) {
            f.columnErrors.put(col, "Invalid decimal " + col);
            return null;
        }
    }

    /**
     * Reads an optional decimal cell; returns null when blank, otherwise records an error when invalid.
     */
    private Double getOptionalDecimal(Row row, int index, String col, RowFailure f) {
        String raw = getCellString(row, index);
        if (isBlankCell(raw)) return null;
        try {
            return Double.parseDouble(sanitizeNumeric(raw));
        } catch (Exception e) {
            f.columnErrors.put(col, "Invalid decimal " + col);
            return null;
        }
    }

    /**
     * Fetches all stored stock records.
     *
     * @return response containing all records (or empty list) and a row count
     */
    @Override
    public StockIndexResponse<List<StockIndexRecord>> getAllStocks() {
        List<StockIndexRecord> stockIndexRecordList = stockRepository.findAll();
        if (stockIndexRecordList == null || stockIndexRecordList.isEmpty()) {
            return new StockIndexResponse<>(
                    "SUCCESS",
                    "No records found",
                    0,
                    stockIndexRecordList == null ? List.of() : stockIndexRecordList
            );
        }
        return new StockIndexResponse<>(
                "SUCCESS",
                "Records fetched successfully",
                stockIndexRecordList.size(),stockIndexRecordList);
    }

    /**
     * Finds a single record by id.
     *
     * @param id record id
     * @return optional record
     */
    @Override
    public Optional<StockIndexRecord> findById(Long id) {
        return stockRepository.findById(id);
    }

    /**
     * Finds all records matching a stock ticker.
     *
     * @param stock stock ticker
     * @return response containing matching records (or empty list)
     */
    @Override
    public StockIndexResponse<List<StockIndexRecord>> findByStock(String stock) {
        if (stock == null || stock.isBlank()) {
            return new StockIndexResponse<>("SUCCESS", "No Stock records found", 0, List.of());
        }
        List<StockIndexRecord> records = stockRepository.findByStock(stock.trim());
        if (records == null || records.isEmpty()) {
            return new StockIndexResponse<>("SUCCESS", "No Stock records found", 0, List.of());
        }
        return new StockIndexResponse<>("SUCCESS", "Stock Record fetched successfully", records.size(), records);
    }

    /**
     * Updates a record by id.
     * <p>
     * When no record exists for the id, returns a success response with {@link ErrorResult} in {@code data}.
     *
     * @param id      record id
     * @param updated updated values
     * @return response containing the updated record or an {@link ErrorResult}
     */
    @Override
    public StockIndexResponse<Object> updateById(Long id, StockIndexRecord updated) {
        Optional<StockIndexRecord> existingOpt = stockRepository.findById(id);
        if (existingOpt.isEmpty()) {
            String detail = "No stock record found with id: " + id;
            return new StockIndexResponse<>(
                    "SUCCESS",
                    detail,
                    0,
                    new ErrorResult(ErrorCodes.NO_RECORD_FOUND, detail)
            );
        }
        StockIndexRecord existing = existingOpt.get();
        if (isAlreadyUpdated(existing, updated)) {
            return new StockIndexResponse<>(
                    "SUCCESS",
                    "Duplicate record is not allowed for the given id",
                    0,
                    new ErrorResult(ErrorCodes.DUPLICATE_RECORD, "Record is already updated for the given id: " + id)
            );
        }
        if (updated != null
                && updated.getStock() != null
                && updated.getDate() != null
                && stockRepository.existsByStockAndDateAndIdNot(updated.getStock(), updated.getDate(), id)) {
            return new StockIndexResponse<>(
                    "SUCCESS",
                    "Duplicate record is not allowed",
                    0,
                    new ErrorResult(
                            ErrorCodes.DUPLICATE_RECORD,
                            "Duplicate stock/date already exists: " + updated.getStock() + "/" + updated.getDate()
                    )
            );
        }
        existing.setQuarter(updated.getQuarter());
        existing.setStock(updated.getStock());
        existing.setDate(updated.getDate());
        existing.setOpen(updated.getOpen());
        existing.setHigh(updated.getHigh());
        existing.setLow(updated.getLow());
        existing.setClose(updated.getClose());
        existing.setVolume(updated.getVolume());
        StockIndexRecord stockIndexRecordUpdated = stockRepository.save(existing);
        return new StockIndexResponse<>(
                "SUCCESS",
                "Record updated successfully",
                1,
                stockIndexRecordUpdated
        );
    }

    private static boolean isAlreadyUpdated(StockIndexRecord existing, StockIndexRecord updated) {
        if (existing == null || updated == null) return false;
        return Objects.equals(existing.getQuarter(), updated.getQuarter())
                && Objects.equals(existing.getStock(), updated.getStock())
                && Objects.equals(existing.getDate(), updated.getDate())
                && Objects.equals(existing.getOpen(), updated.getOpen())
                && Objects.equals(existing.getHigh(), updated.getHigh())
                && Objects.equals(existing.getLow(), updated.getLow())
                && Objects.equals(existing.getClose(), updated.getClose())
                && Objects.equals(existing.getVolume(), updated.getVolume());
    }
    /**
     * Deletes a record by id.
     * <p>
     * Always returns a success response. When id does not exist, rowsAffected is 0 and data includes the requested id.
     *
     * @param id record id
     * @return response containing {@link DeleteResult}
     */
    @Override
    public StockIndexResponse<DeleteResult> deleteById(Long id) {
        if (!stockRepository.existsById(id)) {
            return new StockIndexResponse<>(
                    "SUCCESS",
                    "No stock record found with id: " + id,
                    0,
                    new DeleteResult(id)
            );
        }
        stockRepository.deleteById(id);
        return new StockIndexResponse<>(
                "SUCCESS",
                "Record deleted successfully",
                1,
                new DeleteResult(id)
        );
    }
    /**
     * Deletes multiple records by ids.
     * <p>
     * Always returns a success response. Missing/invalid ids are returned as an {@link ErrorResult} in {@code data}.
     *
     * @param ids list of ids (may be null/empty)
     * @return response containing {@link BulkDeleteResult} or {@link ErrorResult}
     */
    @Override
    public StockIndexResponse<Object> bulkDeleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new StockIndexResponse<>(
                    "SUCCESS",
                    "No ids provided",
                    0,
                    new ErrorResult(ErrorCodes.INVALID_REQUEST, "No ids provided")
            );
        }

        Set<Long> requested = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) requested.add(id);
        }
        if (requested.isEmpty()) {
            return new StockIndexResponse<>(
                    "SUCCESS",
                    "No ids provided",
                    0,
                    new ErrorResult(ErrorCodes.INVALID_REQUEST, "No ids provided")
            );
        }
        List<StockIndexRecord> existing = stockRepository.findAllById(requested);
        Set<Long> existingIds = new LinkedHashSet<>();
        for (StockIndexRecord r : existing) {
            if (r != null && r.getId() != null) existingIds.add(r.getId());
        }
        Set<Long> missing = new LinkedHashSet<>(requested);
        missing.removeAll(existingIds);
        if (!missing.isEmpty()) {
            return new StockIndexResponse<>(
                    "SUCCESS",
                    "Stock id(s) not found",
                    0,
                    new ErrorResult(ErrorCodes.STOCK_IDS_NOT_FOUND, "Stock record(s) not found for id(s): " + missing)
            );
        }
        stockRepository.deleteAllById(existingIds);
        return new StockIndexResponse<>(
                "SUCCESS",
                "Records deleted successfully",
                existingIds.size(),
                new BulkDeleteResult(new ArrayList<>(existingIds), existingIds.size())
        );
    }
    /**
     * Parses a CSV upload, validates each row, and inserts non-duplicate rows.
     * <p>
     * - Skips the header (skipLines=1)
     * - Skips fully-empty trailing lines
     * - Collects per-row/per-column validation failures in {@link FailedRowRecord}
     */
    public FileUploadResponse parseCSV(MultipartFile multipartFile) throws IOException {
        StockIndexRecord stockIndexRecord = null;
        List<FailedRowRecord> failedRows = new ArrayList<>();
        int total = 0;
        int rowNum = 1;
        int insertedRows = 0;
        Set<String> seenStockDateKeys = new LinkedHashSet<>();
        try {
            List<StockIndexCsvDto> stockIndexCsvDtoList =
                    new CsvToBeanBuilder<StockIndexCsvDto>(
                            new InputStreamReader(multipartFile.getInputStream()))
                            .withType(StockIndexCsvDto.class)
                            .withSkipLines(1) // skip header
                            .build()
                            .parse();
            for (StockIndexCsvDto stockIndexCsvDto : stockIndexCsvDtoList) {
                rowNum++;
                if (isCsvRowEmpty(stockIndexCsvDto)) {
                    continue; // skip blank/trailing lines
                }
                total++;
                try {
                    List<FailedRowRecord> rowErrors = validateRow(stockIndexCsvDto, rowNum);
                    if (!rowErrors.isEmpty()) {
                        // collect ALL column errors of this row
                        failedRows.addAll(rowErrors);
                        continue; // skip insert for this row
                    }
                    stockIndexRecord = mapToEntity(stockIndexCsvDto);
                    String duplicateKey = stockDateKey(stockIndexRecord);
                    if (duplicateKey != null && !seenStockDateKeys.add(duplicateKey)) {
                        failedRows.add(err(rowNum, "stock", duplicateKey, "Duplicate stock/date in upload"));
                        continue;
                    }
                    try {
                        stockRepository.save(stockIndexRecord);
                        insertedRows++;
                    } catch (Exception e) {
                        if (isDuplicateStockDateException(e)) {
                            failedRows.add(err(rowNum, "stock", duplicateKey, "Duplicate stock/date already exists"));
                        } else {
                            failedRows.add(err(rowNum, "row", null, e.getMessage()));
                        }
                    }
                } catch (FileUploadValidationException fileUploadValidationException) {
                    failedRows.add(new FailedRowRecord(
                            fileUploadValidationException.getRowNumber(),
                            fileUploadValidationException.getColumnName(),
                            fileUploadValidationException.getInvalidValue(),
                            fileUploadValidationException.getMessage()
                    ));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("CSV File upload failed: " + e.getMessage());
        }
        FileUploadResponse fileUploadResponse = new FileUploadResponse();
        fileUploadResponse.setTotalRows(total);
        fileUploadResponse.setInsertedRows(insertedRows);
        int failedRowCount = (int) failedRows.stream()
                .map(FailedRowRecord::getRowNumber)
                .distinct()
                .count();
        fileUploadResponse.setFailedRows(failedRowCount);
        fileUploadResponse.setFailedRowRecords(failedRows);
        return fileUploadResponse;
    }
    /**
     * Maps a validated CSV DTO into a {@link StockIndexRecord}.
     */
    private StockIndexRecord mapToEntity(StockIndexCsvDto stockIndexCsvDto) {
        LocalDate stockIndexDate = LocalDate.parse(stockIndexCsvDto.getDate(),DateTimeFormatter.ofPattern("M/d/yyyy"));
        StockIndexRecord stockIndexRecord = new StockIndexRecord();
        stockIndexRecord.setQuarter(stockIndexCsvDto.getQuarter());
        stockIndexRecord.setStock(stockIndexCsvDto.getStock());
        stockIndexRecord.setDate(stockIndexDate);
        stockIndexRecord.setOpen(parseDouble(stockIndexCsvDto.getOpen()));
        stockIndexRecord.setHigh(parseDouble(stockIndexCsvDto.getHigh()));
        stockIndexRecord.setLow(parseDouble(stockIndexCsvDto.getLow()));
        stockIndexRecord.setClose(parseDouble(stockIndexCsvDto.getClose()));
        stockIndexRecord.setVolume(Long.parseLong(stockIndexCsvDto.getVolume()));
        stockIndexRecord.setPercentChangePrice(parseNullableDouble(stockIndexCsvDto.getPercent_change_price()));
        stockIndexRecord.setPercentChangeVolumeOverLastWk(parseNullableDouble(stockIndexCsvDto.getPercent_change_volume_over_last_wk()));
        stockIndexRecord.setPreviousWeeksVolume(parseNullableDouble(stockIndexCsvDto.getPrevious_weeks_volume()));
        stockIndexRecord.setNextWeeksOpen(parseNullableDouble(stockIndexCsvDto.getNext_weeks_open()));
        stockIndexRecord.setNextWeeksClose(parseNullableDouble(stockIndexCsvDto.getNext_weeks_close()));
        stockIndexRecord.setPercentChangeNextWeeksPrice(parseNullableDouble(stockIndexCsvDto.getPercent_change_next_weeks_price()));
        stockIndexRecord.setDaysToNextDividend(Integer.parseInt(stockIndexCsvDto.getDays_to_next_dividend()));
        stockIndexRecord.setPercentReturnNextDividend(Double.parseDouble(stockIndexCsvDto.getPercent_return_next_dividend()));
        return stockIndexRecord;
    }

    /**
     * Parses a required money/decimal string (e.g. {@code $16.71}).
     */
    private double parseDouble(String value) {
        return Double.parseDouble(value.replace("$", "").trim());
    }

    /**
     * Parses an optional money/decimal string; returns null when blank.
     */
    private Double parseNullableDouble(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return Double.parseDouble(value.replace("$", "").trim());
    }

    /**
     * Parses an optional long string; returns null when blank.
     */
    private Long parseNullableLong(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return Long.parseLong(value.trim());
    }

    /**
     * Validates a CSV row and returns per-field failures with row/column context.
     */
    private List<FailedRowRecord> validateRow(StockIndexCsvDto stockIndexCsvDto, int rowNum) {

        List<FailedRowRecord> errors = new ArrayList<>();
// 1. quarter
        if (stockIndexCsvDto.getQuarter() <= 0) {
            errors.add(err(rowNum, "quarter",
                    String.valueOf(stockIndexCsvDto.getQuarter()),
                    "Quarter must be positive integer"));
        }
        // 2. stock
        if (isBlank(stockIndexCsvDto.getStock())) {
            errors.add(err(rowNum, "stock",
                    stockIndexCsvDto.getStock(),
                    "Stock cannot be null or blank"));
        }
        // 3. date
        if (isBlank(stockIndexCsvDto.getDate())) {
            errors.add(err(rowNum, "date",
                    stockIndexCsvDto.getDate(),
                    "Date cannot be null or blank"));
        } else {
            try {
                LocalDate.parse(stockIndexCsvDto.getDate(),DateTimeFormatter.ofPattern("M/d/yyyy"));
            } catch (Exception e) {
                errors.add(err(rowNum, "date",
                        stockIndexCsvDto.getDate(),
                        "Invalid date format. Expected M/d/yyyy"));
            }
        }
            // 4. open
            validateMoneyColumn(stockIndexCsvDto.getOpen(), rowNum, "open", errors);

            // 5. high
            validateMoneyColumn(stockIndexCsvDto.getHigh(), rowNum, "high", errors);

            // 6. low
            validateMoneyColumn(stockIndexCsvDto.getLow(), rowNum, "low", errors);

            // 7. close
            validateMoneyColumn(stockIndexCsvDto.getClose(), rowNum, "close", errors);
// 8. volume
            if (isBlank(stockIndexCsvDto.getVolume())) {
                errors.add(err(rowNum, "volume",
                        stockIndexCsvDto.getVolume(),
                        "Volume cannot be null or blank"));
            } else {
                try {
                    long v = Long.parseLong(stockIndexCsvDto.getVolume());
                    if (v < 0) throw new Exception();
                } catch (Exception e) {
                    errors.add(err(rowNum, "volume",
                            stockIndexCsvDto.getVolume(),
                            "Invalid volume. Must be positive number"));
                }
            }
            // 9. percent_change_price (optional)
            validateOptionalDecimal(stockIndexCsvDto.getPercent_change_price(), rowNum,
                    "percent_change_price", errors);

            // 10. percent_change_volume_over_last_wk (optional)
            validateOptionalDecimal(stockIndexCsvDto.getPercent_change_volume_over_last_wk(), rowNum,
                    "percent_change_volume_over_last_wk", errors);

            // 11. previous_weeks_volume (optional)
            validateOptionalLong(stockIndexCsvDto.getPrevious_weeks_volume(), rowNum,
                    "previous_weeks_volume", errors);

            // 12. next_weeks_open (optional money)
            validateOptionalMoney(stockIndexCsvDto.getNext_weeks_open(), rowNum,
                    "next_weeks_open", errors);

            // 13. next_weeks_close (optional money)
            validateOptionalMoney(stockIndexCsvDto.getNext_weeks_close(), rowNum,
                    "next_weeks_close", errors);

            // 14. percent_change_next_weeks_price (optional)
            validateOptionalDecimal(stockIndexCsvDto.getPercent_change_next_weeks_price(), rowNum,
                    "percent_change_next_weeks_price", errors);

            // 15. days_to_next_dividend
            if (isBlank(stockIndexCsvDto.getDays_to_next_dividend())) {
                errors.add(err(rowNum, "days_to_next_dividend",
                        stockIndexCsvDto.getDays_to_next_dividend(),
                        "Days_to_next_dividend cannot be null or blank"));
            } else {
                try {
                    int i = Integer.parseInt(stockIndexCsvDto.getDays_to_next_dividend());
                    if (i < 0) throw new Exception();
                } catch (Exception e) {
                    errors.add(err(rowNum, "days_to_next_dividend",
                            stockIndexCsvDto.getDays_to_next_dividend(),
                            "Invalid integer value"));
                }
            }

            // 16. percent_return_next_dividend
            if (isBlank(stockIndexCsvDto.getPercent_return_next_dividend())) {
                errors.add(err(rowNum, "percent_return_next_dividend",
                        stockIndexCsvDto.getPercent_return_next_dividend(),
                        "Percent_return_next_dividend cannot be null or blank"));
            } else {
                try {
                    Double.parseDouble(stockIndexCsvDto.getPercent_return_next_dividend());
                } catch (Exception e) {
                    errors.add(err(rowNum, "percent_return_next_dividend",
                            stockIndexCsvDto.getPercent_return_next_dividend(),
                            "Invalid decimal value"));
                }
            }
            return errors;
        }

    private void validateOptionalLong(String v, int row, String col,List<FailedRowRecord> errors) {
        if (isBlank(v)) {
            errors.add(err(row, col, v, col + " cannot be null or blank"));
            return;
        }
        try {
            Double.parseDouble(v.replace("$", "").trim());
        } catch (Exception e) {
            errors.add(err(row, col, v, "Invalid long number"));
        }
    }
    private void validateMoneyColumn(String v, int row, String col,List<FailedRowRecord> errors) {
        if (isBlank(v)) {
            errors.add(err(row, col, v, col + " cannot be null or blank"));
            return;
        }
        try {
            Double.parseDouble(v.replace("$", "").trim());
        } catch (Exception e) {
            errors.add(err(row, col, v, "Invalid money format. Expected $12.34"));
        }
    }
    private void validateOptionalMoney(String v, int row, String col, List<FailedRowRecord> errors) {
        if (isBlank(v)) {
            errors.add(err(row, col, v, col + " cannot be null or blank"));
            return;
        }
        try {
            Double.parseDouble(v.replace("$", "").trim());
        } catch (Exception e) {
            errors.add(err(row, col, v,
                    "Invalid money format. Expected $12.34"));
        }
    }
    private void validateOptionalDecimal(String v, int row, String col, List<FailedRowRecord> errors) {
        if (isBlank(v)) {
            errors.add(err(row, col, v, col + " cannot be null or blank"));
            return;
        }
        try {
            Double.parseDouble(v.replace("$", "").trim());
        } catch (Exception e) {
            errors.add(err(row, col, v,
                    "Invalid decimal number"));
        }
    }

    /**
     * Null/blank/space check for CSV values.
     */
    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    /**
     * Builds a {@link FailedRowRecord} for a specific row/column.
     */
    private FailedRowRecord err(int row, String col, String val, String msg) {
        return new FailedRowRecord(row, col, val, msg);
    }

    /**
     * Builds a unique key for the (stock,date) uniqueness constraint.
     */
    private static String stockDateKey(StockIndexRecord record) {
        if (record == null || record.getStock() == null || record.getDate() == null) return null;
        return record.getStock().trim() + "-" + record.getDate();
    }

    /**
     * Detects whether an exception indicates a duplicate key (stock,date) violation.
     */
    private static boolean isDuplicateStockDateException(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof DataIntegrityViolationException) return true;
            String msg = cur.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase(Locale.ROOT);
                if (lower.contains("duplicate entry") || lower.contains("uk_stock_date")) return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    /**
     * Attempts to reconstruct a user-friendly (stock,date) key from an Excel row for error reporting.
     */
    private static String stockDateKeyFromRow(Row row) {
        String stock = getCellString(row, 1);
        String date = getCellString(row, 2);
        if (stock == null || date == null) return null;
        LocalDate parsed = null;
        try {
            DateTimeFormatter[] formats = {
                    DateTimeFormatter.ofPattern("M/d/yyyy"), DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                    DateTimeFormatter.ISO_LOCAL_DATE
            };
            for (DateTimeFormatter f : formats) {
                try {
                    parsed = LocalDate.parse(date.trim(), f);
                    break;
                } catch (Exception ignore) {
                }
            }
        } catch (Exception ignore) {
        }
        return parsed == null ? (stock.trim() + "-" + date.trim()) : (stock.trim() + "-" + parsed);
    }

    /**
     * Returns true when all relevant columns are empty in an Excel row.
     */
    private static boolean isExcelRowEmpty(Row row) {
        for (int i = 0; i <= 15; i++) {
            if (!isBlankCell(getCellString(row, i))) return false;
        }
        return true;
    }

    /**
     * Returns true when a parsed CSV DTO represents a blank/trailing line.
     */
    private boolean isCsvRowEmpty(StockIndexCsvDto dto) {
        if (dto == null) return true;
        if (dto.getQuarter() != 0) return false;
        return isBlank(dto.getStock())
                && isBlank(dto.getDate())
                && isBlank(dto.getOpen())
                && isBlank(dto.getHigh())
                && isBlank(dto.getLow())
                && isBlank(dto.getClose())
                && isBlank(dto.getVolume())
                && isBlank(dto.getPercent_change_price())
                && isBlank(dto.getPercent_change_volume_over_last_wk())
                && isBlank(dto.getPrevious_weeks_volume())
                && isBlank(dto.getNext_weeks_open())
                && isBlank(dto.getNext_weeks_close())
                && isBlank(dto.getPercent_change_next_weeks_price())
                && isBlank(dto.getDays_to_next_dividend())
                && isBlank(dto.getPercent_return_next_dividend());
    }

}
