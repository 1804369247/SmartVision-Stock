package com.example.smartvisionstock.service;

import java.util.List;
import java.util.Map;

public interface BatchService {

    Map<String, Object> createBatch(String sku, String batchNo, int quantity, String expiryDate);

    Map<String, Object> getBatch(String batchId);

    List<Map<String, Object>> getBatchesBySku(String sku);

    Map<String, Object> selectBatchByFIFO(String sku, int quantity);

    Map<String, Object> selectBatchByLIFO(String sku, int quantity);

    Map<String, Object> selectBatchByFEFO(String sku, int quantity);

    List<Map<String, Object>> getBatchHistory(String sku);

    Map<String, Object> updateBatchQuantity(String batchId, int quantity);

    Map<String, Object> getBatchTraceability(String batchId);

    List<Map<String, Object>> getExpiringBatches(int daysThreshold);

    Map<String, Object> getBatchStatistics();
}