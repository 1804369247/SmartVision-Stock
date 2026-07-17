package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
@Tag(name = "批次管理接口", description = "批次追踪、FIFO/LIFO策略执行")
public class BatchController {

    @Autowired
    private BatchService batchService;

    @PostMapping("/create")
    @Operation(summary = "创建批次", description = "创建新的商品批次")
    public ApiResponse<Map<String, Object>> createBatch(
            @RequestParam String sku,
            @RequestParam String batchNo,
            @RequestParam int quantity,
            @RequestParam String expiryDate) {
        Map<String, Object> result = batchService.createBatch(sku, batchNo, quantity, expiryDate);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "获取批次", description = "获取批次详情")
    public ApiResponse<Map<String, Object>> getBatch(@PathVariable String batchId) {
        Map<String, Object> result = batchService.getBatch(batchId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "获取SKU批次", description = "获取指定SKU的所有批次")
    public ApiResponse<List<Map<String, Object>>> getBatchesBySku(@PathVariable String sku) {
        List<Map<String, Object>> result = batchService.getBatchesBySku(sku);
        return ApiResponse.success(result);
    }

    @PostMapping("/select/fifo")
    @Operation(summary = "FIFO选择", description = "根据先进先出策略选择批次")
    public ApiResponse<Map<String, Object>> selectBatchByFIFO(
            @RequestParam String sku,
            @RequestParam int quantity) {
        Map<String, Object> result = batchService.selectBatchByFIFO(sku, quantity);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/select/lifo")
    @Operation(summary = "LIFO选择", description = "根据后进先出策略选择批次")
    public ApiResponse<Map<String, Object>> selectBatchByLIFO(
            @RequestParam String sku,
            @RequestParam int quantity) {
        Map<String, Object> result = batchService.selectBatchByLIFO(sku, quantity);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/select/fefo")
    @Operation(summary = "FEFO选择", description = "根据先到期先出策略选择批次")
    public ApiResponse<Map<String, Object>> selectBatchByFEFO(
            @RequestParam String sku,
            @RequestParam int quantity) {
        Map<String, Object> result = batchService.selectBatchByFEFO(sku, quantity);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/history/{sku}")
    @Operation(summary = "批次历史", description = "获取SKU批次历史记录")
    public ApiResponse<List<Map<String, Object>>> getBatchHistory(@PathVariable String sku) {
        List<Map<String, Object>> result = batchService.getBatchHistory(sku);
        return ApiResponse.success(result);
    }

    @PutMapping("/{batchId}/quantity")
    @Operation(summary = "更新批次数量", description = "更新批次剩余数量")
    public ApiResponse<Map<String, Object>> updateBatchQuantity(
            @PathVariable String batchId,
            @RequestParam int quantity) {
        Map<String, Object> result = batchService.updateBatchQuantity(batchId, quantity);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/{batchId}/traceability")
    @Operation(summary = "批次追溯", description = "获取批次追溯信息")
    public ApiResponse<Map<String, Object>> getBatchTraceability(@PathVariable String batchId) {
        Map<String, Object> result = batchService.getBatchTraceability(batchId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/expiring")
    @Operation(summary = "临期批次", description = "获取即将过期的批次")
    public ApiResponse<List<Map<String, Object>>> getExpiringBatches(
            @RequestParam(defaultValue = "30") int daysThreshold) {
        List<Map<String, Object>> result = batchService.getExpiringBatches(daysThreshold);
        return ApiResponse.success(result);
    }

    @GetMapping("/statistics")
    @Operation(summary = "批次统计", description = "获取批次管理统计信息")
    public ApiResponse<Map<String, Object>> getBatchStatistics() {
        Map<String, Object> result = batchService.getBatchStatistics();
        return ApiResponse.success(result);
    }
}