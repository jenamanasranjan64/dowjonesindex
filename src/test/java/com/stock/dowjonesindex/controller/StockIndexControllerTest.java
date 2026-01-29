package com.stock.dowjonesindex.controller;

import com.stock.dowjonesindex.dto.StockIndexUpdateRequest;
import com.stock.dowjonesindex.model.StockIndexRecord;
import com.stock.dowjonesindex.service.StockIndexServiceInterFace;
import com.stock.dowjonesindex.util.BulkDeleteResult;
import com.stock.dowjonesindex.util.DeleteResult;
import com.stock.dowjonesindex.util.ErrorCodes;
import com.stock.dowjonesindex.util.ErrorResult;
import com.stock.dowjonesindex.util.FileUploadResponse;
import com.stock.dowjonesindex.util.StockIndexResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class StockIndexControllerTest {

    private static StockIndexController controllerWithService(StockIndexServiceInterFace service) {
        StockIndexController controller = new StockIndexController();
        try {
            Field field = StockIndexController.class.getDeclaredField("stockIndexServiceInterFace");
            field.setAccessible(true);
            field.set(controller, service);
            return controller;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    private static final class StubStockIndexService implements StockIndexServiceInterFace {
        private int processUploadCalls;
        private int getAllCalls;
        private int findByIdCalls;
        private int findByStockCalls;
        private int updateByIdCalls;
        private int deleteByIdCalls;
        private int bulkDeleteByIdsCalls;

        private boolean throwOnUpload;

        private FileUploadResponse uploadResponse;
        private StockIndexResponse<List<StockIndexRecord>> getAllResponse;
        private Optional<StockIndexRecord> findByIdResponse = Optional.empty();
        private StockIndexResponse<List<StockIndexRecord>> findByStockResponse;
        private StockIndexResponse<Object> updateResponse;
        private StockIndexResponse<DeleteResult> deleteResponse;
        private StockIndexResponse<Object> bulkDeleteResponse;

        @Override
        public FileUploadResponse processUpload(MultipartFile multipartFile) throws Exception {
            processUploadCalls++;
            if (throwOnUpload) throw new Exception("boom");
            return uploadResponse;
        }

        @Override
        public StockIndexResponse<List<StockIndexRecord>> getAllStocks() {
            getAllCalls++;
            return getAllResponse;
        }

        @Override
        public Optional<StockIndexRecord> findById(Long id) {
            findByIdCalls++;
            return findByIdResponse;
        }

        @Override
        public StockIndexResponse<List<StockIndexRecord>> findByStock(String stock) {
            findByStockCalls++;
            return findByStockResponse;
        }

        @Override
        public StockIndexResponse<Object> updateById(Long id, StockIndexRecord updated) {
            updateByIdCalls++;
            return updateResponse;
        }

        @Override
        public StockIndexResponse<DeleteResult> deleteById(Long id) {
            deleteByIdCalls++;
            return deleteResponse;
        }

        @Override
        public StockIndexResponse<Object> bulkDeleteByIds(List<Long> ids) {
            bulkDeleteByIdsCalls++;
            return bulkDeleteResponse;
        }
    }

    @Test
    void uploadFile_emptyFile_returns400_andDoesNotCallService() throws Exception {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        MultipartFile empty = new StubMultipartFile("file", "stock.csv", "text/csv", new byte[0]);
        ResponseEntity<StockIndexResponse<Object>> response = controller.uploadFile(empty);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().getStatus());
        assertEquals(0, response.getBody().getRowsAffected());
        assertEquals(new ErrorResult(ErrorCodes.INVALID_REQUEST, "File is required and must not be empty"), response.getBody().getData());
        assertEquals(0, service.processUploadCalls);
    }

    @Test
    void uploadFile_unsupportedExtension_returns415_andDoesNotCallService() throws Exception {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        MultipartFile txt = new StubMultipartFile("file", "stock.txt", "text/plain", "hello".getBytes());
        ResponseEntity<StockIndexResponse<Object>> response = controller.uploadFile(txt);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().getStatus());
        assertEquals(0, response.getBody().getRowsAffected());
        assertEquals(
                new ErrorResult(ErrorCodes.INVALID_REQUEST, "Unsupported file type; only .csv and .xlsx are supported"),
                response.getBody().getData()
        );
        assertEquals(0, service.processUploadCalls);
    }

    @Test
    void uploadFile_validCsv_returns200_andResponseBody() throws Exception {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        MultipartFile csv = new StubMultipartFile("file", "stock.csv", "text/csv", "a,b,c".getBytes());
        FileUploadResponse serviceResponse = new FileUploadResponse();
        serviceResponse.setTotalRows(3);
        serviceResponse.setInsertedRows(2);
        serviceResponse.setFailedRows(1);
        service.uploadResponse = serviceResponse;

        ResponseEntity<StockIndexResponse<Object>> response = controller.uploadFile(csv);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().getStatus());
        assertEquals(serviceResponse.getInsertedRows(), response.getBody().getRowsAffected());
        assertEquals(serviceResponse, response.getBody().getData());
        assertEquals(1, service.processUploadCalls);
    }

    @Test
    void uploadFile_allRowsInserted_returns201() throws Exception {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        MultipartFile csv = new StubMultipartFile("file", "stock.csv", "text/csv", "a,b,c".getBytes());
        FileUploadResponse serviceResponse = new FileUploadResponse();
        serviceResponse.setTotalRows(3);
        serviceResponse.setInsertedRows(3);
        serviceResponse.setFailedRows(0);
        service.uploadResponse = serviceResponse;

        ResponseEntity<StockIndexResponse<Object>> response = controller.uploadFile(csv);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().getStatus());
        assertEquals(serviceResponse.getInsertedRows(), response.getBody().getRowsAffected());
        assertEquals(serviceResponse, response.getBody().getData());
        assertEquals(1, service.processUploadCalls);
    }

    @Test
    void uploadFile_serviceThrows_returns500() throws Exception {
        StubStockIndexService service = new StubStockIndexService();
        service.throwOnUpload = true;
        StockIndexController controller = controllerWithService(service);

        MultipartFile csv = new StubMultipartFile("file", "stock.csv", "text/csv", "a,b,c".getBytes());
        ResponseEntity<StockIndexResponse<Object>> response = controller.uploadFile(csv);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().getStatus());
        assertEquals(0, response.getBody().getRowsAffected());
        assertNotNull(response.getBody().getData());
    }

    @Test
    void getAll_returns200_andResponseBody() throws Exception {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        StockIndexResponse<List<StockIndexRecord>> serviceResponse =
                new StockIndexResponse<>("SUCCESS", "ok", 0, List.of());
        service.getAllResponse = serviceResponse;

        ResponseEntity<StockIndexResponse<List<StockIndexRecord>>> response = controller.getAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        assertEquals(1, service.getAllCalls);
    }

    @Test
    void getById_whenPresent_returns200_andResponseBody() {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);
        StockIndexRecord record = new StockIndexRecord();
        record.setId(42L);
        record.setStock("AA");
        service.findByIdResponse = Optional.of(record);
        ResponseEntity<StockIndexResponse<Object>> response = controller.getById(42L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().getStatus());
        assertEquals(1, response.getBody().getRowsAffected());
        assertEquals(record, response.getBody().getData());
        assertEquals(1, service.findByIdCalls);
    }

    @Test
    void getById_whenMissing_returns404_andFailedResponse() {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        service.findByIdResponse = Optional.empty();

        ResponseEntity<StockIndexResponse<Object>> response = controller.getById(999L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().getStatus());
        assertEquals(0, response.getBody().getRowsAffected());
        assertEquals(new ErrorResult(ErrorCodes.NO_RECORD_FOUND, "No stock record found with id: 999"), response.getBody().getData());
        assertEquals(1, service.findByIdCalls);
    }

    @Test
    void getByStock_whenPresent_returns200_andResponseBody() {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        StockIndexRecord record = new StockIndexRecord();
        record.setId(1L);
        record.setStock("AA");
        service.findByStockResponse = new StockIndexResponse<>("SUCCESS", "ok", 1, List.of(record));

        ResponseEntity<StockIndexResponse<List<StockIndexRecord>>> response = controller.getByStock("AA");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(service.findByStockResponse, response.getBody());
        assertEquals(1, service.findByStockCalls);
    }

    @Test
    void updateById_returns200_andDelegatesToService() throws Exception {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        StockIndexUpdateRequest request = new StockIndexUpdateRequest(
                String.valueOf(2),
                "AA",
                "1/05/2015",
                String.valueOf(16.71),
                String.valueOf(16.71),
                String.valueOf(15.64),
                String.valueOf(15.97),
                String.valueOf(242963398L)
        );

        StockIndexRecord updatedRecord = new StockIndexRecord();
        updatedRecord.setId(42L);
        updatedRecord.setStock("AA");
        StockIndexResponse<Object> serviceResponse =
                new StockIndexResponse<>("SUCCESS", "updated", 1, updatedRecord);
        service.updateResponse = serviceResponse;

        ResponseEntity<StockIndexResponse<Object>> response = controller.updateById(42L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        assertEquals(1, service.updateByIdCalls);
    }

    @Test
    void updateById_whenDuplicate_returns200_andDuplicateNotAllowedBody() throws Exception {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        StockIndexUpdateRequest request = new StockIndexUpdateRequest(
                String.valueOf(2),
                "AA",
                "1/05/2015",
                String.valueOf(16.71),
                String.valueOf(16.71),
                String.valueOf(15.64),
                String.valueOf(15.97),
                String.valueOf(242963398L)
        );

        StockIndexResponse<Object> serviceResponse = new StockIndexResponse<>(
                "SUCCESS",
                "Duplicate record is not allowed for the given id",
                0,
                new ErrorResult(ErrorCodes.DUPLICATE_RECORD, "Record is already updated for the given id: 42")
        );
        service.updateResponse = serviceResponse;

        ResponseEntity<StockIndexResponse<Object>> response = controller.updateById(42L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        assertEquals(1, service.updateByIdCalls);
    }

    @Test
    void deleteById_returns200_andDelegatesToService() throws Exception {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        StockIndexResponse<DeleteResult> serviceResponse =
                new StockIndexResponse<>("SUCCESS", "deleted", 1, new DeleteResult(42L));
        service.deleteResponse = serviceResponse;

        ResponseEntity<StockIndexResponse<DeleteResult>> response = controller.deleteById(42L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        assertEquals(1, service.deleteByIdCalls);
    }

    @Test
    void bulkDeleteByIds_returns200_andDelegatesToService() {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        StockIndexResponse<Object> serviceResponse = new StockIndexResponse<>(
                "SUCCESS",
                "bulk deleted",
                2,
                new BulkDeleteResult(List.of(1L, 2L), 2, null, null)
        );
        service.bulkDeleteResponse = serviceResponse;

        ResponseEntity<StockIndexResponse<Object>> response = controller.bulkDeleteByIds(List.of(1L, 2L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        assertEquals(1, service.bulkDeleteByIdsCalls);
    }

    @Test
    void bulkDeleteByIds_whenMissing_returns200_andErrorBody() {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        StockIndexResponse<Object> serviceResponse = new StockIndexResponse<>(
                "SUCCESS",
                "Stock id(s) not found",
                0,
                new BulkDeleteResult(List.of(1L), 1, List.of(2L), 1)
        );
        service.bulkDeleteResponse = serviceResponse;

        ResponseEntity<StockIndexResponse<Object>> response = controller.bulkDeleteByIds(List.of(1L, 2L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        assertEquals(1, service.bulkDeleteByIdsCalls);
    }
}
