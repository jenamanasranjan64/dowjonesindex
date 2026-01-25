package com.stock.dowjonesindex.service;
import com.opencsv.bean.CsvToBeanBuilder;
import com.stock.dowjonesindex.model.StockIndexCsvDto;
import com.stock.dowjonesindex.model.StockIndexRecord;
import com.stock.dowjonesindex.repository.StockRepository;
import com.stock.dowjonesindex.util.*;
import jakarta.persistence.EntityNotFoundException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import java.util.Set;

@Service
public class StockIndexServiceImpl implements StockIndexServiceInterFace {
    private final StockRepository stockRepository;
    public StockIndexServiceImpl(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public FileUploadResponse processUpload(MultipartFile multipartFile) throws Exception {
        String contentType = multipartFile.getContentType();
        FileUploadResponse fileUploadResponse = null;
        if (contentType.equals("text/csv")) {
            fileUploadResponse = parseCSV(multipartFile);
        }
        else if (contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            fileUploadResponse = parseExcel(multipartFile.getInputStream());
        } else {
            throw new IllegalArgumentException("Unsupported file type");
        }
        return fileUploadResponse;
    }
    FileUploadResponse parseExcel(InputStream  inputStream) throws IOException {
        FileUploadResponse fileUploadResponse=new FileUploadResponse();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        int rowNum = 0;
        for (Row row : sheet) {
            rowNum++;
            if (rowNum == 1) continue; // skip header
            RowFailure failure = new RowFailure();
            failure.rowNumber = rowNum;
            try {
                StockIndexRecord stockIndexRecord = parseAndValidate(row, failure);
                if (failure.columnErrors.isEmpty()) {
                    stockRepository.save(stockIndexRecord);
                    fileUploadResponse.insertedRows++;
                } else {
                    fileUploadResponse.failedRows++;
                    fileUploadResponse.failures.add(failure);
                }
            } catch (Exception e) {
                failure.columnErrors.put("row", e.getMessage());
                fileUploadResponse.failedRows++;
                fileUploadResponse.failures.add(failure);
            }
        }
        fileUploadResponse.totalRows = fileUploadResponse.insertedRows + fileUploadResponse.failedRows;
        workbook.close();
        return fileUploadResponse;
    }
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
    private StockIndexRecord parseAndValidate(Row row, RowFailure failure) {
        StockIndexRecord stockIndexRecord=new StockIndexRecord();
        // quarter
        Integer quarter = getInt(row, 0, "quarter", failure);
        System.out.println("quarter...."+quarter);
        if (quarter == null) {
            failure.columnErrors.put("quarter", "Quarter must be 1 to 4");
        }else {
            stockIndexRecord.setQuarter(quarter.intValue());
        }
        // stock
        String stock = getString(row, 1,"stock",failure);
        if (stock == null || stock.isBlank()) {
            failure.columnErrors.put("stock", "Stock is mandatory");
        }else {
            stockIndexRecord.setStock(stock);
        }
// date
        try {
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
//            String date =row.getCell(2).toString();
//            System.out.println("date..."+date);
//            LocalDate dateObjfor = LocalDate.parse(date, formatter);
//            System.out.println("date objectfor"+dateObjfor);
            LocalDate localDate= safeParseDate(row.getCell(2).toString());
            System.out.println(localDate);
            stockIndexRecord.setDate(localDate);
        } catch (Exception ex) {
            failure.columnErrors.put("date", "Invalid date format");
        }
        stockIndexRecord.setOpen(getDecimal(row, 3, "open", failure));
        stockIndexRecord.setHigh(getDecimal(row, 4, "high", failure));
        stockIndexRecord.setLow(getDecimal(row, 5, "low", failure));
        stockIndexRecord.setClose(getDecimal(row, 6, "close", failure));
        stockIndexRecord.setVolume(getLong(row, 7, "volume", failure).longValue());
        stockIndexRecord.setPercentChangePrice(getDecimal(row, 8, "percent_change_price", failure));
        stockIndexRecord.setPercentChangeVolumeOverLastWk(getDecimal(row, 9, "percent_change_volume_over_last_wk", failure));
        stockIndexRecord.setPreviousWeeksVolume(getDecimal(row, 10, "previous_weeks_volume", failure));
        stockIndexRecord.setNextWeeksOpen(getDecimal(row, 11, "next_weeks_open", failure));
        stockIndexRecord.setNextWeeksClose(getDecimal(row, 12, "next_weeks_close", failure));
        stockIndexRecord.setPercentChangeNextWeeksPrice(getDecimal(row, 13, "percent_change_next_weeks_price", failure));
        stockIndexRecord.setDaysToNextDividend(getInt(row, 14, "days_to_next_dividend", failure).intValue());
        stockIndexRecord.setPercentReturnNextDividend(getDecimal(row, 15, "percent_return_next_dividend", failure));
        return stockIndexRecord;
    }

    private String getString(Row row, int i,String col, RowFailure f) {
        try { return row.getCell(i).getStringCellValue(); }
        catch (Exception e) {
            f.columnErrors.put(col, "Invalid String "+col);
            return null;
        }
    }

    private Integer getInt(Row row, int i, String col, RowFailure f) {
        try { return (int) row.getCell(i).getNumericCellValue(); }
        catch (Exception e) {
            f.columnErrors.put(col, "Invalid integer "+col);
            return null;
        }
    }


    private Long getLong(Row row, int i, String col, RowFailure f) {
        try { return (long) row.getCell(i).getNumericCellValue(); }
        catch (Exception e) {
            f.columnErrors.put(col, "Invalid long "+col);
            return null;
        }
    }


    private Double getDecimal(Row row, int i, String col, RowFailure f) {
        try {
            return Double.parseDouble(row.getCell(i).toString().replace("$", "")); }
        catch (Exception e) {
            f.columnErrors.put(col, "Invalid decimal "+col);
            return null;
        }
    }

    @Override
    public StockIndexResponse<List<StockIndexRecord>> getAllStocks() {
        List<StockIndexRecord> stockIndexRecordList = stockRepository.findAll();
        return new StockIndexResponse<>(
                "SUCCESS",
                "Records fetched successfully",
                stockIndexRecordList.size(),stockIndexRecordList);
    }

    @Override
    public StockIndexResponse<StockIndexRecord> updateById(Long id, StockIndexRecord updated) {
        // check existence
        StockIndexRecord existing = stockRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Stock not found with id: " + id));
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
    @Override
    public StockIndexResponse<Void> deleteById(Long id) {
        if (!stockRepository.existsById(id)) {
            throw new EntityNotFoundException("Stock not found with id: " + id);
        }
        stockRepository.deleteById(id);

        return new StockIndexResponse<>(
                "SUCCESS",
                "Record deleted successfully",
                1,
                null
        );
    }

    @Override
    public StockIndexResponse<Void> bulkDeleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new StockIndexResponse<>(
                    "FAILED",
                    "No ids provided",
                    0,
                    null
            );
        }

        Set<Long> requested = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) requested.add(id);
        }
        if (requested.isEmpty()) {
            return new StockIndexResponse<>(
                    "FAILED",
                    "No ids provided",
                    0,
                    null
            );
        }

        List<StockIndexRecord> existing = stockRepository.findAllById(requested);
        Set<Long> existingIds = new LinkedHashSet<>();
        for (StockIndexRecord r : existing) {
            if (r != null && r.getId() != null) existingIds.add(r.getId());
        }

        if (existingIds.size() != requested.size()) {
            Set<Long> missing = new LinkedHashSet<>(requested);
            missing.removeAll(existingIds);
            throw new EntityNotFoundException("Stock not found with id(s): " + missing);
        }

        stockRepository.deleteAllById(existingIds);

        return new StockIndexResponse<>(
                "SUCCESS",
                "Records deleted successfully",
                existingIds.size(),
                null
        );
    }

    public FileUploadResponse parseCSV(MultipartFile multipartFile) throws IOException {
        StockIndexRecord stockIndexRecord = null;
        List<FailedRowRecord> failedRows = new ArrayList<>();
        List<StockIndexRecord> stockIndexRecordList = new ArrayList<>();
        int total = 0;
        int rowNum = 1;
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
                total++;
                try {
                    List<FailedRowRecord> rowErrors = validateRow(stockIndexCsvDto, rowNum);
                    if (!rowErrors.isEmpty()) {
                        // collect ALL column errors of this row
                        failedRows.addAll(rowErrors);
                        continue; // skip insert for this row
                    }
                    stockIndexRecord = mapToEntity(stockIndexCsvDto);
                    stockIndexRecordList.add(stockIndexRecord);
                } catch (FileUploadValidationException fileUploadValidationException) {
                    failedRows.add(new FailedRowRecord(
                            fileUploadValidationException.getRowNumber(),
                            fileUploadValidationException.getColumnName(),
                            fileUploadValidationException.getInvalidValue(),
                            fileUploadValidationException.getMessage()
                    ));
                }
            }
            stockRepository.saveAll(stockIndexRecordList);
////            if (!failedRows.isEmpty()) {
////                writeFailedRowsResponse(uploadId, failedRows);
//            }
        } catch (Exception e) {
            throw new RuntimeException("CSV File upload failed: " + e.getMessage());
        }
        FileUploadResponse fileUploadResponse = new FileUploadResponse();
        fileUploadResponse.setTotalRows(total);
        fileUploadResponse.setInsertedRows(stockIndexRecordList.size());
        fileUploadResponse.setFailedRows(failedRows.size());
        fileUploadResponse.setFailedRowRecords(failedRows);
        return fileUploadResponse;
    }

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

    private double parseDouble(String value) {
        return Double.parseDouble(value.replace("$", "").trim());
    }

    private Double parseNullableDouble(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return Double.parseDouble(value.replace("$", "").trim());
    }

    private Long parseNullableLong(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return Long.parseLong(value.trim());
    }

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
    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }
    private FailedRowRecord err(int row, String col, String val, String msg) {
        return new FailedRowRecord(row, col, val, msg);
    }

}
