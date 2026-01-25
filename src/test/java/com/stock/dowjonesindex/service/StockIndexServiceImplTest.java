package com.stock.dowjonesindex.service;

import com.stock.dowjonesindex.model.StockIndexRecord;
import com.stock.dowjonesindex.repository.StockRepository;
import com.stock.dowjonesindex.util.FileUploadResponse;
import com.stock.dowjonesindex.util.StockIndexResponse;
import jakarta.persistence.EntityNotFoundException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockIndexServiceImplTest {

    private static final class StubMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        private StubMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content == null ? new byte[0] : content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), content);
        }
    }

    private static final class RepoHarness {
        private final AtomicLong idSeq = new AtomicLong(100);
        private final Map<Long, StockIndexRecord> store = new LinkedHashMap<>();

        private int saveCalls;
        private int saveAllCalls;
        private int deleteByIdCalls;

        private List<StockIndexRecord> lastSaveAllArgument;

        private final StockRepository repo = (StockRepository) Proxy.newProxyInstance(
                StockRepository.class.getClassLoader(),
                new Class<?>[]{StockRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("toString")) return "StockRepositoryProxy";
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];

                    if (name.equals("findAll")) {
                        return new ArrayList<>(store.values());
                    }
                    if (name.equals("findById")) {
                        Long id = (Long) args[0];
                        return Optional.ofNullable(store.get(id));
                    }
                    if (name.equals("existsById")) {
                        Long id = (Long) args[0];
                        return store.containsKey(id);
                    }
                    if (name.equals("deleteById")) {
                        deleteByIdCalls++;
                        Long id = (Long) args[0];
                        store.remove(id);
                        return null;
                    }
                    if (name.equals("save")) {
                        saveCalls++;
                        StockIndexRecord record = (StockIndexRecord) args[0];
                        if (record.getId() == null) record.setId(idSeq.incrementAndGet());
                        store.put(record.getId(), record);
                        return record;
                    }
                    if (name.equals("saveAll")) {
                        saveAllCalls++;
                        @SuppressWarnings("unchecked")
                        Iterable<StockIndexRecord> input = (Iterable<StockIndexRecord>) args[0];
                        List<StockIndexRecord> list = new ArrayList<>();
                        for (StockIndexRecord r : input) {
                            if (r.getId() == null) r.setId(idSeq.incrementAndGet());
                            store.put(r.getId(), r);
                            list.add(r);
                        }
                        lastSaveAllArgument = list;
                        return list;
                    }
                    if (name.equals("findAllById")) {
                        @SuppressWarnings("unchecked")
                        Iterable<Long> ids = (Iterable<Long>) args[0];
                        List<StockIndexRecord> list = new ArrayList<>();
                        for (Long id : ids) {
                            StockIndexRecord r = store.get(id);
                            if (r != null) list.add(r);
                        }
                        return list;
                    }
                    if (name.equals("deleteAllById")) {
                        @SuppressWarnings("unchecked")
                        Iterable<Long> ids = (Iterable<Long>) args[0];
                        for (Long id : ids) {
                            store.remove(id);
                        }
                        return null;
                    }
                    throw new UnsupportedOperationException("Unstubbed method: " + method);
                }
        );
    }

    @Test
    void safeParseDate_parsesSupportedFormats_andReturnsNullForInvalid() {
        RepoHarness harness = new RepoHarness();
        StockIndexServiceImpl service = new StockIndexServiceImpl(harness.repo);

        assertEquals(LocalDate.of(2011, 1, 14), service.safeParseDate("1/14/2011"));
        assertEquals(LocalDate.of(2011, 1, 14), service.safeParseDate("01/14/2011"));
        assertEquals(LocalDate.of(2011, 1, 14), service.safeParseDate("2011-01-14"));
        assertNull(service.safeParseDate("not-a-date"));
        assertNull(service.safeParseDate(" "));
        assertNull(service.safeParseDate(null));
    }

    @Test
    void getAllStocks_returnsSuccessAndRowCount() {
        RepoHarness harness = new RepoHarness();
        harness.store.put(1L, new StockIndexRecord());
        harness.store.put(2L, new StockIndexRecord());
        StockIndexServiceImpl service = new StockIndexServiceImpl(harness.repo);

        StockIndexResponse<List<StockIndexRecord>> response = service.getAllStocks();

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(2, response.getRowsAffected());
        assertEquals(2, response.getData().size());
    }

    @Test
    void updateById_whenPresent_updatesAndSaves() {
        RepoHarness harness = new RepoHarness();
        StockIndexRecord existing = new StockIndexRecord();
        existing.setId(42L);
        existing.setStock("OLD");
        harness.store.put(42L, existing);

        StockIndexServiceImpl service = new StockIndexServiceImpl(harness.repo);

        StockIndexRecord updated = new StockIndexRecord();
        updated.setQuarter(2);
        updated.setStock("AA");
        updated.setDate(LocalDate.of(2011, 1, 14));
        updated.setOpen(10.0);
        updated.setHigh(11.0);
        updated.setLow(9.0);
        updated.setClose(10.5);
        updated.setVolume(123L);

        StockIndexResponse<StockIndexRecord> response = service.updateById(42L, updated);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(1, response.getRowsAffected());
        assertEquals(1, harness.saveCalls);
        assertEquals("AA", harness.store.get(42L).getStock());
        assertEquals(2, harness.store.get(42L).getQuarter());
    }

    @Test
    void updateById_whenMissing_throwsEntityNotFound() {
        RepoHarness harness = new RepoHarness();
        StockIndexServiceImpl service = new StockIndexServiceImpl(harness.repo);

        assertThrows(EntityNotFoundException.class, () -> service.updateById(999L, new StockIndexRecord()));
    }

    @Test
    void deleteById_whenMissing_throwsEntityNotFound() {
        RepoHarness harness = new RepoHarness();
        StockIndexServiceImpl service = new StockIndexServiceImpl(harness.repo);

        assertThrows(EntityNotFoundException.class, () -> service.deleteById(999L));
    }

    @Test
    void deleteById_whenPresent_deletesAndReturnsSuccess() {
        RepoHarness harness = new RepoHarness();
        StockIndexRecord existing = new StockIndexRecord();
        existing.setId(42L);
        harness.store.put(42L, existing);

        StockIndexServiceImpl service = new StockIndexServiceImpl(harness.repo);

        StockIndexResponse<Void> response = service.deleteById(42L);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(1, response.getRowsAffected());
        assertEquals(1, harness.deleteByIdCalls);
        assertNull(harness.store.get(42L));
    }

    @Test
    void bulkDeleteByIds_whenEmpty_returnsFailed() {
        RepoHarness harness = new RepoHarness();
        StockIndexServiceImpl service = new StockIndexServiceImpl(harness.repo);

        StockIndexResponse<Void> response = service.bulkDeleteByIds(List.of());

        assertEquals("FAILED", response.getStatus());
        assertEquals(0, response.getRowsAffected());
    }

    @Test
    void bulkDeleteByIds_whenAnyMissing_throwsEntityNotFound() {
        RepoHarness harness = new RepoHarness();
        StockIndexRecord existing = new StockIndexRecord();
        existing.setId(1L);
        harness.store.put(1L, existing);
        StockIndexServiceImpl service = new StockIndexServiceImpl(harness.repo);

        assertThrows(EntityNotFoundException.class, () -> service.bulkDeleteByIds(List.of(1L, 2L)));
    }

    @Test
    void bulkDeleteByIds_whenAllExist_deletesAndReturnsCount() {
        RepoHarness harness = new RepoHarness();
        StockIndexRecord a = new StockIndexRecord();
        a.setId(1L);
        StockIndexRecord b = new StockIndexRecord();
        b.setId(2L);
        harness.store.put(1L, a);
        harness.store.put(2L, b);

        StockIndexServiceImpl service = new StockIndexServiceImpl(harness.repo);

        List<Long> ids = new ArrayList<>();
        ids.add(1L);
        ids.add(2L);
        ids.add(2L);
        ids.add(null);
        StockIndexResponse<Void> response = service.bulkDeleteByIds(ids);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(2, response.getRowsAffected());
        assertNull(harness.store.get(1L));
        assertNull(harness.store.get(2L));
    }

    @Test
    void processUpload_csv_parsesRows_savesValidAndReturnsCounts() throws Exception {
        RepoHarness harness = new RepoHarness();
        StockIndexServiceImpl service = new StockIndexServiceImpl(harness.repo);

        String header = "quarter,stock,date,open,high,low,close,volume,percent_change_price," +
                "percent_change_volume_over_last_wk,previous_weeks_volume,next_weeks_open,next_weeks_close," +
                "percent_change_next_weeks_price,days_to_next_dividend,percent_return_next_dividend\n";
        String validRow = "1,AA,1/14/2011,$16.71,$16.71,$15.64,$15.97,242963398,-4.42849," +
                "1.380223,239655616,$15.87,$16.13,1.63803,19,0.187852\n";
        String invalidRow = "-1,,bad,$x,$y,$z,$w,-1,,,,,,,,\n";
        byte[] csvBytes = (header + validRow + invalidRow).getBytes();

        MultipartFile csv = new StubMultipartFile("file", "stock.csv", "text/csv", csvBytes);
        FileUploadResponse response = service.processUpload(csv);

        assertEquals(2, response.getTotalRows());
        assertEquals(1, response.getInsertedRows());
        assertNotNull(response.getFailedRowRecords());
        assertEquals(1, harness.saveAllCalls);
        assertNotNull(harness.lastSaveAllArgument);
        assertEquals(1, harness.lastSaveAllArgument.size());
    }

    @Test
    void processUpload_excel_parsesRows_savesValidAndReturnsCounts() throws Exception {
        RepoHarness harness = new RepoHarness();
        StockIndexServiceImpl service = new StockIndexServiceImpl(harness.repo);

        byte[] xlsx = excelBytesWithTwoRows_oneValid_oneInvalid();
        MultipartFile excel = new StubMultipartFile(
                "file",
                "stock.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsx
        );

        FileUploadResponse response = service.processUpload(excel);

        assertEquals(2, response.getTotalRows());
        assertEquals(1, response.getInsertedRows());
        assertEquals(1, response.getFailedRows());
        assertEquals(1, harness.saveCalls);
        assertNotNull(response.getFailures());
        assertEquals(1, response.getFailures().size());
    }

    @Test
    void processUpload_unsupportedType_throws() {
        RepoHarness harness = new RepoHarness();
        StockIndexServiceImpl service = new StockIndexServiceImpl(harness.repo);

        MultipartFile file = new StubMultipartFile("file", "stock.txt", "text/plain", "x".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.processUpload(file));
    }

    private static byte[] excelBytesWithTwoRows_oneValid_oneInvalid() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");

            Row header = sheet.createRow(0);
            for (int i = 0; i < 16; i++) header.createCell(i, CellType.STRING).setCellValue("h" + i);

            Row valid = sheet.createRow(1);
            valid.createCell(0, CellType.NUMERIC).setCellValue(1);
            valid.createCell(1, CellType.STRING).setCellValue("AA");
            valid.createCell(2, CellType.STRING).setCellValue("1/14/2011");
            valid.createCell(3, CellType.STRING).setCellValue("$16.71");
            valid.createCell(4, CellType.STRING).setCellValue("$16.71");
            valid.createCell(5, CellType.STRING).setCellValue("$15.64");
            valid.createCell(6, CellType.STRING).setCellValue("$15.97");
            valid.createCell(7, CellType.NUMERIC).setCellValue(242963398);
            valid.createCell(8, CellType.STRING).setCellValue("-4.42849");
            valid.createCell(9, CellType.STRING).setCellValue("1.380223");
            valid.createCell(10, CellType.STRING).setCellValue("239655616");
            valid.createCell(11, CellType.STRING).setCellValue("$15.87");
            valid.createCell(12, CellType.STRING).setCellValue("$16.13");
            valid.createCell(13, CellType.STRING).setCellValue("1.63803");
            valid.createCell(14, CellType.NUMERIC).setCellValue(19);
            valid.createCell(15, CellType.STRING).setCellValue("0.187852");

            Row invalid = sheet.createRow(2);
            invalid.createCell(0, CellType.NUMERIC).setCellValue(1);
            invalid.createCell(1, CellType.STRING).setCellValue(""); // invalid stock
            invalid.createCell(2, CellType.STRING).setCellValue("1/14/2011");
            invalid.createCell(3, CellType.STRING).setCellValue("not-a-number"); // triggers decimal error
            invalid.createCell(4, CellType.STRING).setCellValue("$16.71");
            invalid.createCell(5, CellType.STRING).setCellValue("$15.64");
            invalid.createCell(6, CellType.STRING).setCellValue("$15.97");
            invalid.createCell(7, CellType.NUMERIC).setCellValue(242963398);
            invalid.createCell(8, CellType.STRING).setCellValue("-4.42849");
            invalid.createCell(9, CellType.STRING).setCellValue("1.380223");
            invalid.createCell(10, CellType.STRING).setCellValue("239655616");
            invalid.createCell(11, CellType.STRING).setCellValue("$15.87");
            invalid.createCell(12, CellType.STRING).setCellValue("$16.13");
            invalid.createCell(13, CellType.STRING).setCellValue("1.63803");
            invalid.createCell(14, CellType.NUMERIC).setCellValue(19);
            invalid.createCell(15, CellType.STRING).setCellValue("0.187852");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
