package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "xls", "xlsx"};
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @PostMapping("/image")
    public ApiResponse<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "image");
    }

    @PostMapping("/document")
    public ApiResponse<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "document");
    }

    @PostMapping("/file")
    public ApiResponse<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "general");
    }

    private ApiResponse<Map<String, Object>> uploadFile(MultipartFile file, String type) {
        if (file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的文件");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ApiResponse.error(400, "文件大小不能超过10MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isAllowedExtension(originalFilename)) {
            return ApiResponse.error(400, "不支持的文件类型");
        }

        try {
            String subDir = type + "/" + LocalDateTime.now().format(DT_FMT);
            Path uploadPath = Paths.get(uploadDir, subDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(newFilename);

            file.transferTo(filePath.toFile());

            String url = "/api/upload/file/" + subDir + "/" + newFilename;

            Map<String, Object> result = new HashMap<>();
            result.put("fileName", originalFilename);
            result.put("filePath", filePath.toString());
            result.put("url", url);
            result.put("size", file.getSize());
            result.put("type", file.getContentType());

            return ApiResponse.success(result);
        } catch (IOException e) {
            return ApiResponse.error(500, "文件上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/file/{path}")
    public void serveFile(@PathVariable String path, javax.servlet.http.HttpServletResponse response) throws IOException {
        if (path.contains("..")) {
            response.setStatus(400);
            return;
        }
        Path filePath = Paths.get(uploadDir, path).normalize();
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!filePath.toAbsolutePath().startsWith(uploadPath)) {
            response.setStatus(400);
            return;
        }
        if (!Files.exists(filePath)) {
            response.setStatus(404);
            return;
        }

        String contentType = Files.probeContentType(filePath);
        response.setContentType(contentType != null ? contentType : "application/octet-stream");
        response.setHeader("Content-Disposition", "inline; filename=\"" + java.net.URLEncoder.encode(filePath.getFileName().toString(), "UTF-8") + "\"");
        Files.copy(filePath, response.getOutputStream());
    }

    @DeleteMapping("/file/{path}")
    public ApiResponse<Object> deleteFile(@PathVariable String path) {
        try {
            if (path.contains("..")) {
                return ApiResponse.error(400, "非法路径");
            }
            Path filePath = Paths.get(uploadDir, path).normalize();
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!filePath.toAbsolutePath().startsWith(uploadPath)) {
                return ApiResponse.error(400, "非法路径");
            }
            if (!Files.exists(filePath)) {
                return ApiResponse.error(404, "文件不存在");
            }
            Files.delete(filePath);
            return ApiResponse.success("文件删除成功");
        } catch (IOException e) {
            return ApiResponse.error(500, "文件删除失败：" + e.getMessage());
        }
    }

    private boolean isAllowedExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equals(extension)) {
                return true;
            }
        }
        return false;
    }
}