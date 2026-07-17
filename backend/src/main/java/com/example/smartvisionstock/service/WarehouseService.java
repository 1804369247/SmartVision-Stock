package com.example.smartvisionstock.service;

import java.util.List;
import java.util.Map;

public interface WarehouseService {

    List<Map<String, Object>> getAllWarehouses();

    Map<String, Object> getWarehouse(Long id);

    Map<String, Object> createWarehouse(Map<String, Object> warehouseData);

    Map<String, Object> updateWarehouse(Long id, Map<String, Object> warehouseData);

    Map<String, Object> deleteWarehouse(Long id);

    Map<String, Object> transferStock(String sku, int quantity, Long fromWarehouseId, Long toWarehouseId);

    Map<String, Object> getTransferHistory(String sku);

    Map<String, Object> getWarehouseInventory(Long warehouseId);

    Map<String, Object> getSharedInventory(String sku);

    List<Map<String, Object>> getTransferRequests(String status);

    Map<String, Object> approveTransfer(String transferId);

    Map<String, Object> executeTransfer(String transferId);

    Map<String, Object> getWarehouseStatistics();
}