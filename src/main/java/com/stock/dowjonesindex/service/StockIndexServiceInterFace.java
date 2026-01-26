package com.stock.dowjonesindex.service;

import com.stock.dowjonesindex.model.StockIndexRecord;
import com.stock.dowjonesindex.util.DeleteResult;
import com.stock.dowjonesindex.util.FileUploadResponse;
import com.stock.dowjonesindex.util.StockIndexResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface StockIndexServiceInterFace {

    FileUploadResponse processUpload(MultipartFile multipartFile)  throws Exception;

    StockIndexResponse<List<StockIndexRecord>> getAllStocks();

    Optional<StockIndexRecord> findById(Long id);

    StockIndexResponse<List<StockIndexRecord>> findByStock(String stock);

    StockIndexResponse<Object> updateById(Long id, StockIndexRecord updated);

    StockIndexResponse<DeleteResult> deleteById(Long id);

    StockIndexResponse<Object> bulkDeleteByIds(List<Long> ids);
}
