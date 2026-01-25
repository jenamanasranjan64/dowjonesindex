package com.stock.dowjonesindex.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StockIndexControllerMappingsTest {

    @Test
    void controller_hasBaseRequestMapping() {
        RequestMapping mapping = StockIndexController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/api/stock-data"}, mapping.value());
    }

    @Test
    void upload_isMappedToPostUpload_andHasFileRequestParam() throws Exception {
        Method method = StockIndexController.class.getDeclaredMethod("uploadFile", MultipartFile.class);
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/upload"}, mapping.value());

        Parameter param = method.getParameters()[0];
        RequestParam requestParam = param.getAnnotation(RequestParam.class);
        assertNotNull(requestParam);
        assertEquals("file", requestParam.value());
    }

    @Test
    void allStockRecord_isMappedToGetAllStockRecord() throws Exception {
        Method method = StockIndexController.class.getDeclaredMethod("getAll");
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/allStockRecord"}, mapping.value());
    }

    @Test
    void deleteId_isMappedToDeleteDeleteId_andHasPathVariable() throws Exception {
        Method method = StockIndexController.class.getDeclaredMethod("deleteById", Long.class);
        DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/deleteId/{id}"}, mapping.value());

        Parameter param = method.getParameters()[0];
        PathVariable pv = param.getAnnotation(PathVariable.class);
        assertNotNull(pv);
    }

    @Test
    void bulkDelete_isMappedToDeleteBulkDelete_andHasRequestBody() throws Exception {
        Method method = StockIndexController.class.getDeclaredMethod("bulkDeleteByIds", java.util.List.class);
        DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/bulk-delete"}, mapping.value());

        Parameter param = method.getParameters()[0];
        RequestBody rb = param.getAnnotation(RequestBody.class);
        assertNotNull(rb);
    }
}
