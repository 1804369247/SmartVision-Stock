package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.entity.OperationLog;
import com.example.smartvisionstock.repository.OperationLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@Tag(name = "日志接口", description = "操作日志查询和导出")
public class LogController {

    @Autowired
    private OperationLogRepository operationLogRepository;

    @GetMapping("/operation")
    @Operation(summary = "查询操作日志", description = "分页查询操作日志，支持按模块、操作员筛选")
    public ApiResponse<Map<String, Object>> getOperationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Long operatorId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "operateTime"));
        Page<OperationLog> logPage;

        if (module != null && !module.isEmpty()) {
            logPage = operationLogRepository.findByModule(module, pageable);
        } else if (operatorId != null) {
            logPage = operationLogRepository.findByOperatorId(operatorId, pageable);
        } else {
            logPage = operationLogRepository.findAll(pageable);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", logPage.getContent());
        result.put("totalElements", logPage.getTotalElements());
        result.put("totalPages", logPage.getTotalPages());
        result.put("currentPage", page);

        return ApiResponse.success(result);
    }

    @GetMapping("/operation/{id}")
    @Operation(summary = "获取日志详情", description = "根据ID获取操作日志详情")
    public ApiResponse<OperationLog> getOperationLogById(@PathVariable Long id) {
        OperationLog log = operationLogRepository.findById(id).orElse(null);

        if (log == null) {
            return ApiResponse.notFound("日志不存在");
        }

        return ApiResponse.success(log);
    }

    @PostMapping("/operation/export")
    @Operation(summary = "导出日志", description = "导出操作日志为CSV格式")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Long operatorId) {

        List<OperationLog> logs;
        if (module != null && !module.isEmpty()) {
            logs = operationLogRepository.findByModuleOrderByOperateTimeDesc(module);
        } else if (operatorId != null) {
            logs = operationLogRepository.findByOperatorIdOrderByOperateTimeDesc(operatorId);
        } else {
            logs = operationLogRepository.findAll(Sort.by(Sort.Direction.DESC, "operateTime"));
        }

        StringBuilder csv = new StringBuilder();
        csv.append("ID,操作员ID,操作员姓名,操作类型,模块,目标ID,详情,IP地址,操作时间\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (OperationLog log : logs) {
            csv.append(log.getId()).append(",");
            csv.append(log.getOperatorId() != null ? log.getOperatorId() : "").append(",");
            csv.append(escapeCsv(log.getOperatorName())).append(",");
            csv.append(escapeCsv(log.getOperationType())).append(",");
            csv.append(escapeCsv(log.getModule())).append(",");
            csv.append(log.getTargetId() != null ? log.getTargetId() : "").append(",");
            csv.append(escapeCsv(log.getDetail())).append(",");
            csv.append(log.getIpAddress()).append(",");
            csv.append(log.getOperateTime() != null ? log.getOperateTime().format(formatter) : "").append("\n");
        }

        byte[] csvBytes = csv.toString().getBytes();
        String filename = "operation_logs_" + System.currentTimeMillis() + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(csvBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}