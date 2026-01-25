package com.stock.dowjonesindex.controller;

import com.stock.dowjonesindex.model.StockIndexRecord;
import com.stock.dowjonesindex.service.StockIndexServiceInterFace;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        private int updateByIdCalls;
        private int deleteByIdCalls;
        private int bulkDeleteByIdsCalls;

        private boolean throwOnUpload;

        private FileUploadResponse uploadResponse;
        private StockIndexResponse<List<StockIndexRecord>> getAllResponse;
        private StockIndexResponse<StockIndexRecord> updateResponse;
        private StockIndexResponse<Void> deleteResponse;
        private StockIndexResponse<Void> bulkDeleteResponse;

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
        public StockIndexResponse<StockIndexRecord> updateById(Long id, StockIndexRecord updated) {
            updateByIdCalls++;
            return updateResponse;
        }

        @Override
        public StockIndexResponse<Void> deleteById(Long id) {
            deleteByIdCalls++;
            return deleteResponse;
        }

        @Override
        public StockIndexResponse<Void> bulkDeleteByIds(List<Long> ids) {
            bulkDeleteByIdsCalls++;
            return bulkDeleteResponse;
        }
    }

    @Test
    void uploadFile_emptyFile_returns400_andDoesNotCallService() throws Exception {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        MultipartFile empty = new StubMultipartFile("file", "stock.csv", "text/csv", new byte[0]);
        ResponseEntity<FileUploadResponse> response = controller.uploadFile(empty);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        assertEquals(0, service.processUploadCalls);
    }

    @Test
    void uploadFile_unsupportedExtension_returns415_andDoesNotCallService() throws Exception {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        MultipartFile txt = new StubMultipartFile("file", "stock.txt", "text/plain", "hello".getBytes());
        ResponseEntity<FileUploadResponse> response = controller.uploadFile(txt);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertNull(response.getBody());
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

        ResponseEntity<FileUploadResponse> response = controller.uploadFile(csv);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        assertEquals(1, service.processUploadCalls);
    }

    @Test
    void uploadFile_serviceThrows_returns500() throws Exception {
        StubStockIndexService service = new StubStockIndexService();
        service.throwOnUpload = true;
        StockIndexController controller = controllerWithService(service);

        MultipartFile csv = new StubMultipartFile("file", "stock.csv", "text/csv", "a,b,c".getBytes());
        ResponseEntity<FileUploadResponse> response = controller.uploadFile(csv);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
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
    void updateById_returns200_andDelegatesToService() throws Exception {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        StockIndexRecord updatedRecord = new StockIndexRecord();
        updatedRecord.setId(42L);
        updatedRecord.setStock("AA");

        StockIndexResponse<StockIndexRecord> serviceResponse =
                new StockIndexResponse<>("SUCCESS", "updated", 1, updatedRecord);
        service.updateResponse = serviceResponse;

        ResponseEntity<StockIndexResponse<StockIndexRecord>> response = controller.updateById(42L, updatedRecord);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        assertEquals(1, service.updateByIdCalls);
    }

    @Test
    void deleteById_returns200_andDelegatesToService() throws Exception {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        StockIndexResponse<Void> serviceResponse = new StockIndexResponse<>("SUCCESS", "deleted", 1, null);
        service.deleteResponse = serviceResponse;

        ResponseEntity<StockIndexResponse<Void>> response = controller.deleteById(42L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        assertEquals(1, service.deleteByIdCalls);
    }

    @Test
    void bulkDeleteByIds_returns200_andDelegatesToService() {
        StubStockIndexService service = new StubStockIndexService();
        StockIndexController controller = controllerWithService(service);

        StockIndexResponse<Void> serviceResponse = new StockIndexResponse<>("SUCCESS", "bulk deleted", 2, null);
        service.bulkDeleteResponse = serviceResponse;

        ResponseEntity<StockIndexResponse<Void>> response = controller.bulkDeleteByIds(List.of(1L, 2L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        assertEquals(1, service.bulkDeleteByIdsCalls);
    }
}
