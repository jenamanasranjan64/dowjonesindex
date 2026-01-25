package com.stock.dowjonesindex.controller;

import com.stock.dowjonesindex.model.StockIndexRecord;
import com.stock.dowjonesindex.service.StockIndexServiceInterFace;
import com.stock.dowjonesindex.util.FileUploadResponse;
import com.stock.dowjonesindex.util.StockIndexResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
@RestController
@RequestMapping("/api/stock-data")
public class StockIndexController {
    @Autowired
    private StockIndexServiceInterFace stockIndexServiceInterFace;
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().build();
        String fileName = file.getOriginalFilename();
        if (fileName != null && !(fileName.endsWith(".csv") || fileName.endsWith(".xlsx"))) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }
        try {
            FileUploadResponse fileUploadResponse = stockIndexServiceInterFace.processUpload(file);
            return ResponseEntity.ok(fileUploadResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // ========================
    // SELECT ALL
    // ========================
    @GetMapping("/allStockRecord")
    public ResponseEntity<StockIndexResponse<List<StockIndexRecord>>> getAll() {
        StockIndexResponse<List<StockIndexRecord>> response = stockIndexServiceInterFace.getAllStocks();
        return ResponseEntity.ok(response);
    }
    // ========================
    // UPDATE BY ID
    // ========================
    @PutMapping("/{id}")
    public ResponseEntity<StockIndexResponse<StockIndexRecord>> updateById(@PathVariable Long id,
            @RequestBody StockIndexRecord updated) {
        StockIndexResponse<StockIndexRecord> response = stockIndexServiceInterFace.updateById(id, updated);
        return ResponseEntity.ok(response);
    }

    // ========================
    // DELETE BY ID
    // ========================
    @DeleteMapping("/deleteId/{id}")
    public ResponseEntity<StockIndexResponse<Void>> deleteById(@PathVariable Long id) {
        StockIndexResponse<Void> response = stockIndexServiceInterFace.deleteById(id);
        return ResponseEntity.ok(response);
    }

    // ========================
    // BULK DELETE BY IDS
    // ========================
    @DeleteMapping("/bulk-delete")
    public ResponseEntity<StockIndexResponse<Void>> bulkDeleteByIds(@RequestBody List<Long> ids) {
        StockIndexResponse<Void> response = stockIndexServiceInterFace.bulkDeleteByIds(ids);
        return ResponseEntity.ok(response);
    }

}
