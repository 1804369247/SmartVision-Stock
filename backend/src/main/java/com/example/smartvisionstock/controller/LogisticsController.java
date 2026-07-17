package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.service.LogisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logistics")
@Tag(name = "物流对接接口", description = "快递单号追踪、电子面单打印")
public class LogisticsController {

    @Autowired
    private LogisticsService logisticsService;

    @PostMapping("/track")
    @Operation(summary = "物流追踪", description = "根据快递单号查询物流状态")
    public ApiResponse<Map<String, Object>> trackShipment(@RequestParam String trackingNo) {
        Map<String, Object> result = logisticsService.trackShipment(trackingNo);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/shipment")
    @Operation(summary = "创建运单", description = "创建物流运单")
    public ApiResponse<Map<String, Object>> createShipment(@RequestBody Map<String, Object> shipmentData) {
        Map<String, Object> result = logisticsService.createShipment(shipmentData);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/label")
    @Operation(summary = "生成电子面单", description = "生成订单电子面单")
    public ApiResponse<Map<String, Object>> generateLabel(@RequestParam String orderId) {
        Map<String, Object> result = logisticsService.generateLabel(orderId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/companies")
    @Operation(summary = "快递公司列表", description = "获取可用快递公司列表")
    public ApiResponse<Map<String, Object>> getShippingCompanies() {
        Map<String, Object> result = logisticsService.getShippingCompanies();
        return ApiResponse.success(result);
    }

    @PostMapping("/estimate")
    @Operation(summary = "运费估算", description = "估算运费")
    public ApiResponse<Map<String, Object>> estimateShippingCost(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam double weight) {
        Map<String, Object> result = logisticsService.estimateShippingCost(from, to, weight);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "订单运单", description = "获取订单关联的运单")
    public ApiResponse<List<Map<String, Object>>> getShipmentsByOrder(@PathVariable Long orderId) {
        List<Map<String, Object>> result = logisticsService.getShipmentsByOrder(orderId);
        return ApiResponse.success(result);
    }

    @PutMapping("/shipment/{trackingNo}/status")
    @Operation(summary = "更新运单状态", description = "更新运单状态")
    public ApiResponse<Map<String, Object>> updateShipmentStatus(
            @PathVariable String trackingNo,
            @RequestParam String status) {
        Map<String, Object> result = logisticsService.updateShipmentStatus(trackingNo, status);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/statistics")
    @Operation(summary = "物流统计", description = "获取物流统计信息")
    public ApiResponse<Map<String, Object>> getLogisticsStatistics() {
        Map<String, Object> result = logisticsService.getLogisticsStatistics();
        return ApiResponse.success(result);
    }
}