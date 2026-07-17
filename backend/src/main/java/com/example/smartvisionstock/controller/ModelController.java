package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.service.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/models")
public class ModelController {

    @Autowired
    private ModelService modelService;

    @GetMapping("/{modelId}")
    public ApiResponse<Map<String, Object>> getModel(@PathVariable String modelId) {
        Map<String, Object> result = modelService.getModel(modelId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listModels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name) {
        try {
            List<Map<String, Object>> models = modelService.listModels(page, size, name);
            Map<String, Object> result = new HashMap<>();
            result.put("data", models);
            result.put("total", models.size());
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createModel(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String format = (String) request.getOrDefault("format", "glb");
            String uploadId = (String) request.get("uploadId");
            
            byte[] modelData = new byte[1024];
            Map<String, Object> result = modelService.saveModel(name, modelData, format, uploadId);
            Integer code = (Integer) result.get("code");
            if (code != null && code != 200) {
                return ApiResponse.error(code, (String) result.get("message"));
            }
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{modelId}")
    public ApiResponse<Map<String, Object>> updateModel(
            @PathVariable String modelId,
            @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Map<String, Object> result = modelService.updateModel(modelId, name, description);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{modelId}")
    public ApiResponse<Object> deleteModel(@PathVariable String modelId) {
        try {
            modelService.deleteModel(modelId);
            return ApiResponse.success("模型删除成功");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{modelId}/merge")
    public ApiResponse<Map<String, Object>> mergeModel(
            @PathVariable String modelId,
            @RequestParam String sceneId) {
        Map<String, Object> result = modelService.mergeModel(modelId, sceneId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/{modelId}/metadata")
    public ApiResponse<Map<String, Object>> getMetadata(@PathVariable String modelId) {
        Map<String, Object> result = modelService.getModelMetadata(modelId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }
}