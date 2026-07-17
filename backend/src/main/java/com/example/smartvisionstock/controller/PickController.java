package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.service.PickService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pick")
@Tag(name = "波次拣货接口", description = "批量拣货、路径优化等波次拣货功能")
public class PickController {

    @Autowired
    private PickService pickService;

    @PostMapping("/wave")
    @Operation(summary = "创建波次拣货", description = "根据订单列表创建波次拣货任务")
    public ApiResponse<Map<String, Object>> createWavePick(@RequestBody List<Long> orderIds) {
        Map<String, Object> result = pickService.createWavePick(orderIds);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @GetMapping("/wave/{waveId}")
    @Operation(summary = "获取波次详情", description = "获取波次拣货任务详情")
    public ApiResponse<Map<String, Object>> getWavePick(@PathVariable String waveId) {
        Map<String, Object> result = pickService.getWavePick(waveId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @GetMapping("/tasks")
    @Operation(summary = "获取拣货任务列表", description = "获取波次下的所有拣货任务")
    public ApiResponse<Map<String, Object>> getPickTasks(@RequestParam String waveId) {
        List<Map<String, Object>> tasks = pickService.getPickTasks(waveId);
        Map<String, Object> result = new HashMap<>();
        result.put("data", tasks);
        result.put("total", tasks.size());
        return ApiResponse.success(result);
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "获取任务详情", description = "获取单个拣货任务详情")
    public ApiResponse<Map<String, Object>> getPickTaskDetail(@PathVariable String taskId) {
        Map<String, Object> result = pickService.getPickTaskDetail(taskId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @PutMapping("/tasks/{taskId}/complete")
    @Operation(summary = "完成拣货任务", description = "标记拣货任务为已完成")
    public ApiResponse<Map<String, Object>> completePickTask(
            @PathVariable String taskId,
            @RequestBody Map<String, Object> completionData) {
        Map<String, Object> result = pickService.completePickTask(taskId, completionData);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @PostMapping("/path/optimize")
    @Operation(summary = "优化拣货路径", description = "计算最优拣货路径")
    public ApiResponse<Map<String, Object>> optimizePickPath(@RequestParam String waveId) {
        Map<String, Object> result = pickService.optimizePickPath(waveId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @GetMapping("/waves/active")
    @Operation(summary = "获取活动波次", description = "获取所有进行中的波次")
    public ApiResponse<Map<String, Object>> getActiveWaves() {
        List<Map<String, Object>> waves = pickService.getActiveWaves();
        Map<String, Object> result = new HashMap<>();
        result.put("data", waves);
        result.put("total", waves.size());
        return ApiResponse.success(result);
    }

    @PostMapping("/wave/{waveId}/cancel")
    @Operation(summary = "取消波次", description = "取消波次拣货任务")
    public ApiResponse<Map<String, Object>> cancelWavePick(@PathVariable String waveId) {
        Map<String, Object> result = pickService.cancelWavePick(waveId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @GetMapping("/statistics/{waveId}")
    @Operation(summary = "拣货统计", description = "获取波次拣货统计信息")
    public ApiResponse<Map<String, Object>> getPickStatistics(@PathVariable String waveId) {
        Map<String, Object> result = pickService.getPickStatistics(waveId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }
}