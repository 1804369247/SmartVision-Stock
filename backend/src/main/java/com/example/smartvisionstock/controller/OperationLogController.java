package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.entity.OperationLog;
import com.example.smartvisionstock.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping("/logs")
    public ApiResponse<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            Page<OperationLog> logPage = operationLogService.getLogs(page, size, userId, username, module, startTime, endTime);
            Map<String, Object> result = new HashMap<>();
            result.put("data", logPage.getContent());
            result.put("total", logPage.getTotalElements());
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/logs/recent")
    public ApiResponse<List<OperationLog>> getRecentLogs() {
        try {
            List<OperationLog> logs = operationLogService.getRecentLogs();
            return ApiResponse.success(logs);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}