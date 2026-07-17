package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.annotation.LogOperation;
import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.service.PrintService;
import com.example.smartvisionstock.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/print")
@Tag(name = "打印接口", description = "打印机管理和打印任务")
public class PrintController {

    @Autowired
    private PrintService printService;

    @GetMapping("/printers")
    @Operation(summary = "获取打印机列表", description = "获取所有可用打印机信息")
    public ApiResponse<Map<String, Object>> getPrinters() {
        List<Map<String, Object>> printers = printService.getPrinters();
        Map<String, Object> result = new HashMap<>();
        result.put("content", printers);
        result.put("totalElements", printers.size());
        return ApiResponse.success(result);
    }

    @PostMapping("/order")
    @Operation(summary = "打印订单", description = "根据订单ID打印出库单")
    @LogOperation(type = "PRINT", module = "打印管理", description = "打印订单")
    public ApiResponse<Map<String, Object>> printOrder(@RequestBody Map<String, Object> request) {
        Long currentUserId = UserContext.getCurrentUserIdOrDefault(1L);
        String currentUsername = UserContext.getCurrentUsernameOrDefault("admin");

        Long orderId = null;
        Object orderIdObj = request.get("orderId");
        if (orderIdObj instanceof Integer) {
            orderId = ((Integer) orderIdObj).longValue();
        } else if (orderIdObj instanceof Long) {
            orderId = (Long) orderIdObj;
        } else if (orderIdObj instanceof String) {
            orderId = Long.parseLong((String) orderIdObj);
        }
        String type = (String) request.getOrDefault("type", "normal");

        Map<String, Object> result = printService.printOrder(orderId, type);
        result.put("operatorId", currentUserId);
        result.put("operatorName", currentUsername);
        
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/label")
    @Operation(summary = "打印标签", description = "打印商品标签或货架标签")
    @LogOperation(type = "PRINT", module = "打印管理", description = "打印标签")
    public ApiResponse<Map<String, Object>> printLabel(@RequestBody Map<String, Object> labelData) {
        Long currentUserId = UserContext.getCurrentUserIdOrDefault(1L);
        String currentUsername = UserContext.getCurrentUsernameOrDefault("admin");
        labelData.put("operatorId", currentUserId);
        labelData.put("operatorName", currentUsername);

        Map<String, Object> result = printService.printLabel(labelData);
        result.put("operatorId", currentUserId);
        result.put("operatorName", currentUsername);
        
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }
}
