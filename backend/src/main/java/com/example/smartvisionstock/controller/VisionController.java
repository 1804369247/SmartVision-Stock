package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.service.ImageService;
import com.example.smartvisionstock.service.ReconstructionService;
import com.example.smartvisionstock.service.VisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vision")
@Tag(name = "机器视觉接口", description = "图像上传、识别、三维重建等机器视觉功能")
public class VisionController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private VisionService visionService;

    @Autowired
    private ReconstructionService reconstructionService;

    @PostMapping("/upload")
    @Operation(summary = "上传图片", description = "上传单张图片进行识别或重建")
    public ApiResponse<Map<String, Object>> uploadImage(
            @Parameter(description = "图片文件") @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = imageService.uploadImage(file, "vision");
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/upload/multiple")
    @Operation(summary = "批量上传图片", description = "上传多张图片用于三维重建")
    public ApiResponse<Map<String, Object>> uploadMultipleImages(
            @Parameter(description = "图片文件列表") @RequestParam("files") MultipartFile[] files) {
        Map<String, Object> result = imageService.uploadMultipleImages(files);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/image/{imageId}")
    @Operation(summary = "获取图片", description = "根据图片ID获取图片内容")
    public ResponseEntity<byte[]> getImage(
            @Parameter(description = "图片ID") @PathVariable String imageId) {
        byte[] imageData = imageService.getImage(imageId);
        if (imageData.length == 0) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        return ResponseEntity.ok().headers(headers).body(imageData);
    }

    @PostMapping("/recognize")
    @Operation(summary = "仓库识别", description = "识别仓库布局、货架、货物等元素")
    public ApiResponse<Map<String, Object>> recognize(
            @Parameter(description = "上传批次ID") @RequestParam String uploadId,
            @Parameter(description = "拍摄角度数量") @RequestParam(defaultValue = "4") int angleCount) {
        Map<String, Object> result = visionService.recognize(uploadId, angleCount);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/detect")
    @Operation(summary = "物体检测", description = "检测图片中的物体（货架、货物、叉车等）")
    public ApiResponse<Map<String, Object>> detectObjects(
            @Parameter(description = "图片ID") @RequestParam String imageId) {
        try {
            List<Map<String, Object>> objects = visionService.detectObjects(imageId);
            Map<String, Object> result = new HashMap<>();
            result.put("objects", objects);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/analyze/shelf")
    @Operation(summary = "货架分析", description = "分析货架容量、利用率等信息")
    public ApiResponse<Map<String, Object>> analyzeShelf(
            @Parameter(description = "上传批次ID") @RequestParam String uploadId) {
        Map<String, Object> result = visionService.analyzeShelf(uploadId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/measure")
    @Operation(summary = "距离测量", description = "测量图片中两点之间的实际距离")
    public ApiResponse<Map<String, Object>> measureDistance(
            @Parameter(description = "图片ID") @RequestParam String imageId,
            @Parameter(description = "测量点坐标") @RequestBody Map<String, Integer> points) {
        Map<String, Object> result = visionService.measureDistance(imageId, points);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/reconstruct")
    @Operation(summary = "三维重建", description = "从多角度图片生成3D模型")
    public ApiResponse<Map<String, Object>> reconstruct(
            @Parameter(description = "上传批次ID") @RequestParam String uploadId) {
        Map<String, Object> result = reconstructionService.reconstruct(uploadId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/reconstruct/progress/{taskId}")
    @Operation(summary = "重建进度", description = "查询三维重建任务进度")
    public ApiResponse<Map<String, Object>> getProgress(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        Map<String, Object> result = reconstructionService.getReconstructionProgress(taskId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/reconstruct/cancel/{taskId}")
    @Operation(summary = "取消重建", description = "取消正在进行的三维重建任务")
    public ApiResponse<Map<String, Object>> cancelReconstruction(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        Map<String, Object> result = reconstructionService.cancelReconstruction(taskId);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/model/export/{modelId}")
    @Operation(summary = "导出模型", description = "导出3D模型文件（支持OBJ/GLB格式）")
    public ResponseEntity<byte[]> exportModel(
            @Parameter(description = "模型ID") @PathVariable String modelId,
            @Parameter(description = "导出格式") @RequestParam(defaultValue = "obj") String format) {
        byte[] modelData = reconstructionService.exportModel(modelId, format);
        if (modelData.length == 0) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", modelId + "." + format);
        return ResponseEntity.ok().headers(headers).body(modelData);
    }

    @PostMapping("/model/optimize/{modelId}")
    @Operation(summary = "优化模型", description = "优化3D模型质量和大小")
    public ApiResponse<Map<String, Object>> optimizeModel(
            @Parameter(description = "模型ID") @PathVariable String modelId,
            @Parameter(description = "质量百分比") @RequestParam(defaultValue = "80") int quality) {
        Map<String, Object> result = reconstructionService.optimizeModel(modelId, quality);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }
}