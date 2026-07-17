package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.service.ExpiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expiry")
@Tag(name = "效期预警接口", description = "自动检测临期商品、发送预警通知")
public class ExpiryController {

    @Autowired
    private ExpiryService expiryService;

    @GetMapping("/check")
    @Operation(summary = "检查临期商品", description = "检查指定天数内即将过期的商品")
    public ApiResponse<List<Map<String, Object>>> checkExpiringProducts(
            @RequestParam(defaultValue = "30") int daysThreshold) {
        List<Map<String, Object>> result = expiryService.checkExpiringProducts(daysThreshold);
        return ApiResponse.success(result);
    }

    @GetMapping("/alert/{alertId}")
    @Operation(summary = "获取预警详情", description = "获取单个预警详情")
    public ApiResponse<Map<String, Object>> getExpiryAlert(@PathVariable String alertId) {
        Map<String, Object> result = expiryService.getExpiryAlert(alertId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/alerts/active")
    @Operation(summary = "活动预警", description = "获取所有活动预警")
    public ApiResponse<List<Map<String, Object>>> getActiveAlerts() {
        List<Map<String, Object>> result = expiryService.getActiveAlerts();
        return ApiResponse.success(result);
    }

    @PostMapping("/alert/{alertId}/process")
    @Operation(summary = "处理预警", description = "处理效期预警")
    public ApiResponse<Map<String, Object>> processAlert(
            @PathVariable String alertId,
            @RequestParam String action) {
        Map<String, Object> result = expiryService.processAlert(alertId, action);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/notification/send")
    @Operation(summary = "发送预警通知", description = "发送效期预警通知")
    public ApiResponse<Map<String, Object>> sendExpiryNotification(@RequestBody List<String> alertIds) {
        Map<String, Object> result = expiryService.sendExpiryNotification(alertIds);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/statistics")
    @Operation(summary = "效期统计", description = "获取效期预警统计信息")
    public ApiResponse<Map<String, Object>> getExpiryStatistics() {
        Map<String, Object> result = expiryService.getExpiryStatistics();
        return ApiResponse.success(result);
    }

    @GetMapping("/expired")
    @Operation(summary = "过期商品", description = "获取已过期商品列表")
    public ApiResponse<List<Map<String, Object>>> getExpiredProducts() {
        List<Map<String, Object>> result = expiryService.getExpiredProducts();
        return ApiResponse.success(result);
    }

    @PostMapping("/{batchId}/mark-expired")
    @Operation(summary = "标记过期", description = "标记批次为过期状态")
    public ApiResponse<Map<String, Object>> markAsExpired(@PathVariable String batchId) {
        Map<String, Object> result = expiryService.markAsExpired(batchId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }
}