package com.stock.dowjonesindex.service;

import com.stock.dowjonesindex.model.StockIndexRecord;
import com.stock.dowjonesindex.util.FileUploadResponse;
import com.stock.dowjonesindex.util.StockIndexResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StockIndexServiceInterFace {

    FileUploadResponse processUpload(MultipartFile multipartFile)  throws Exception;

    StockIndexResponse<List<StockIndexRecord>> getAllStocks();

    StockIndexResponse<StockIndexRecord> updateById(Long id, StockIndexRecord updated);

    StockIndexResponse<Void> deleteById(Long id);

    StockIndexResponse<Void> bulkDeleteByIds(List<Long> ids);
}
