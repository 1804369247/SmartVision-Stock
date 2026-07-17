package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/return")
@Tag(name = "退货管理接口", description = "退货验收、质检、入库完整流程")
public class ReturnController {

    @Autowired
    private ReturnService returnService;

    @PostMapping("/create")
    @Operation(summary = "创建退货申请", description = "创建退货入库申请")
    public ApiResponse<Map<String, Object>> createReturnRequest(@RequestBody Map<String, Object> returnData) {
        Map<String, Object> result = returnService.createReturnRequest(returnData);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @GetMapping("/{returnId}")
    @Operation(summary = "获取退货详情", description = "获取退货申请详情")
    public ApiResponse<Map<String, Object>> getReturnRequest(@PathVariable String returnId) {
        Map<String, Object> result = returnService.getReturnRequest(returnId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success((Map<String, Object>) result.get("data"));
    }

    @GetMapping("/list")
    @Operation(summary = "获取退货列表", description = "获取退货申请列表")
    public ApiResponse<Map<String, Object>> getReturnRequests(
            @RequestParam(required = false) String status) {
        List<Map<String, Object>> returns = returnService.getReturnRequests(status);
        Map<String, Object> result = new HashMap<>();
        result.put("data", returns);
        result.put("total", returns.size());
        return ApiResponse.success(result);
    }

    @PutMapping("/{returnId}/inspect")
    @Operation(summary = "质检退货", description = "对退货商品进行质检")
    public ApiResponse<Map<String, Object>> inspectReturn(
            @PathVariable String returnId,
            @RequestBody Map<String, Object> inspectData) {
        Map<String, Object> result = returnService.inspectReturn(returnId, inspectData);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @PutMapping("/{returnId}/confirm")
    @Operation(summary = "确认退货", description = "确认退货入库")
    public ApiResponse<Map<String, Object>> confirmReturn(@PathVariable String returnId) {
        Map<String, Object> result = returnService.confirmReturn(returnId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @PutMapping("/{returnId}/reject")
    @Operation(summary = "拒绝退货", description = "拒绝退货申请")
    public ApiResponse<Map<String, Object>> rejectReturn(
            @PathVariable String returnId,
            @RequestParam String reason) {
        Map<String, Object> result = returnService.rejectReturn(returnId, reason);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @PostMapping("/{returnId}/refund")
    @Operation(summary = "处理退款", description = "处理退货退款")
    public ApiResponse<Map<String, Object>> processRefund(@PathVariable String returnId) {
        Map<String, Object> result = returnService.processRefund(returnId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @GetMapping("/statistics")
    @Operation(summary = "退货统计", description = "获取退货管理统计信息")
    public ApiResponse<Map<String, Object>> getReturnStatistics() {
        Map<String, Object> result = returnService.getReturnStatistics();
        result.remove("code");
        return ApiResponse.success(result);
    }

    @GetMapping("/history/{customerId}")
    @Operation(summary = "退货历史", description = "获取客户退货历史")
    public ApiResponse<Map<String, Object>> getReturnHistory(@PathVariable String customerId) {
        List<Map<String, Object>> history = returnService.getReturnHistory(customerId);
        Map<String, Object> result = new HashMap<>();
        result.put("data", history);
        result.put("total", history.size());
        return ApiResponse.success(result);
    }
}