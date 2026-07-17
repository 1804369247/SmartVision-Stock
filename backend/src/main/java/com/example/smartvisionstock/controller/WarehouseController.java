package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warehouses")
@Tag(name = "多仓库管理接口", description = "跨仓库调拨、库存共享")
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    @GetMapping
    @Operation(summary = "获取仓库列表", description = "获取所有仓库信息")
    public ApiResponse<Map<String, Object>> getAllWarehouses() {
        List<Map<String, Object>> warehouses = warehouseService.getAllWarehouses();
        Map<String, Object> result = new HashMap<>();
        result.put("data", warehouses);
        result.put("total", warehouses.size());
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取仓库详情", description = "获取单个仓库详情")
    public ApiResponse<Map<String, Object>> getWarehouse(@PathVariable Long id) {
        Map<String, Object> result = warehouseService.getWarehouse(id);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success((Map<String, Object>) result.get("data"));
    }

    @PostMapping
    @Operation(summary = "创建仓库", description = "创建新仓库")
    public ApiResponse<Map<String, Object>> createWarehouse(@RequestBody Map<String, Object> warehouseData) {
        Map<String, Object> result = warehouseService.createWarehouse(warehouseData);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新仓库", description = "更新仓库信息")
    public ApiResponse<Map<String, Object>> updateWarehouse(
            @PathVariable Long id,
            @RequestBody Map<String, Object> warehouseData) {
        Map<String, Object> result = warehouseService.updateWarehouse(id, warehouseData);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除仓库", description = "删除仓库")
    public ApiResponse<Map<String, Object>> deleteWarehouse(@PathVariable Long id) {
        Map<String, Object> result = warehouseService.deleteWarehouse(id);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @PostMapping("/transfer")
    @Operation(summary = "库存调拨", description = "跨仓库库存调拨")
    public ApiResponse<Map<String, Object>> transferStock(
            @RequestParam String sku,
            @RequestParam int quantity,
            @RequestParam Long fromWarehouseId,
            @RequestParam Long toWarehouseId) {
        Map<String, Object> result = warehouseService.transferStock(sku, quantity, fromWarehouseId, toWarehouseId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @GetMapping("/transfer/history/{sku}")
    @Operation(summary = "调拨历史", description = "获取SKU调拨历史")
    public ApiResponse<Map<String, Object>> getTransferHistory(@PathVariable String sku) {
        Map<String, Object> result = warehouseService.getTransferHistory(sku);
        result.remove("code");
        return ApiResponse.success(result);
    }

    @GetMapping("/{warehouseId}/inventory")
    @Operation(summary = "仓库库存", description = "获取仓库库存信息")
    public ApiResponse<Map<String, Object>> getWarehouseInventory(@PathVariable Long warehouseId) {
        Map<String, Object> result = warehouseService.getWarehouseInventory(warehouseId);
        result.remove("code");
        return ApiResponse.success(result);
    }

    @GetMapping("/shared/{sku}")
    @Operation(summary = "共享库存", description = "获取SKU在各仓库的共享库存")
    public ApiResponse<Map<String, Object>> getSharedInventory(@PathVariable String sku) {
        Map<String, Object> result = warehouseService.getSharedInventory(sku);
        result.remove("code");
        return ApiResponse.success(result);
    }

    @GetMapping("/transfers")
    @Operation(summary = "调拨申请列表", description = "获取调拨申请列表")
    public ApiResponse<Map<String, Object>> getTransferRequests(
            @RequestParam(required = false) String status) {
        List<Map<String, Object>> transfers = warehouseService.getTransferRequests(status);
        Map<String, Object> result = new HashMap<>();
        result.put("data", transfers);
        result.put("total", transfers.size());
        return ApiResponse.success(result);
    }

    @PostMapping("/transfer/{transferId}/approve")
    @Operation(summary = "批准调拨", description = "批准库存调拨申请")
    public ApiResponse<Map<String, Object>> approveTransfer(@PathVariable String transferId) {
        Map<String, Object> result = warehouseService.approveTransfer(transferId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @PostMapping("/transfer/{transferId}/execute")
    @Operation(summary = "执行调拨", description = "执行已批准的库存调拨")
    public ApiResponse<Map<String, Object>> executeTransfer(@PathVariable String transferId) {
        Map<String, Object> result = warehouseService.executeTransfer(transferId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @GetMapping("/statistics")
    @Operation(summary = "仓库统计", description = "获取仓库管理统计信息")
    public ApiResponse<Map<String, Object>> getWarehouseStatistics() {
        Map<String, Object> result = warehouseService.getWarehouseStatistics();
        return ApiResponse.success(result);
    }
}