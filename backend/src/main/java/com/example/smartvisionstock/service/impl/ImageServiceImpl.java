package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.service.FileStorageService;
import com.example.smartvisionstock.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageServiceImpl implements ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageServiceImpl.class);

    @Autowired
    private FileStorageService fileStorageService;

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;

    @Override
    public Map<String, Object> uploadImage(MultipartFile file, String type) {
        Map<String, Object> result = new HashMap<>();
        if (file == null || file.isEmpty()) {
            result.put("code", 400);
            result.put("message", "请选择要上传的文件");
            return result;
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            result.put("code", 400);
            result.put("message", "文件大小不能超过10MB");
            return result;
        }
        String originalFilename = file.getOriginalFilename();
        String extension = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase() : "";
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            result.put("code", 400);
            result.put("message", "不支持的图片类型");
            return result;
        }
        try {
            String imageId = UUID.randomUUID().toString();
            fileStorageService.store(imageId, extension, file);

            log.info("图片上传成功: id={}, name={}, size={}", imageId, originalFilename, file.getSize());

            result.put("code", 200);
            result.put("imageId", imageId);
            result.put("filename", originalFilename);
            result.put("size", file.getSize());
            result.put("type", type);

        } catch (IOException e) {
            log.error("图片上传失败", e);
            result.put("code", 400);
            result.put("message", "上传失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> uploadMultipleImages(MultipartFile[] files) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> uploaded = new ArrayList<>();

        for (MultipartFile file : files) {
            Map<String, Object> uploadResult = uploadImage(file, "multiple");
            uploaded.add(uploadResult);
        }

        result.put("code", 200);
        result.put("count", uploaded.size());
        result.put("images", uploaded);
        return result;
    }

    @Override
    public byte[] getImage(String imageId) {
        return fileStorageService.read(imageId);
    }

    @Override
    public Map<String, Object> preprocessImage(String imageId, String operation) {
        Map<String, Object> result = new HashMap<>();

        String extension = fileStorageService.resolveExtension(imageId);
        if (extension == null) {
            result.put("code", 400);
            result.put("message", "图片不存在: " + imageId);
            return result;
        }

        try {
            byte[] raw = fileStorageService.read(imageId);
            if (raw.length == 0) {
                result.put("code", 400);
                result.put("message", "图片不存在: " + imageId);
                return result;
            }

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(raw));
            if (image == null) {
                result.put("code", 400);
                result.put("message", "无法读取图片数据");
                return result;
            }

            int originalWidth = image.getWidth();
            int originalHeight = image.getHeight();

            switch (operation != null ? operation.toLowerCase() : "") {
                case "resize":
                case "thumbnail":
                    int maxDim = 800;
                    if (originalWidth > maxDim || originalHeight > maxDim) {
                        double ratio = Math.min((double) maxDim / originalWidth, (double) maxDim / originalHeight);
                        int newW = (int) (originalWidth * ratio);
                        int newH = (int) (originalHeight * ratio);
                        Image scaled = image.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
                        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = resized.createGraphics();
                        g.drawImage(scaled, 0, 0, null);
                        g.dispose();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(resized, "jpg", baos);
                        fileStorageService.store(imageId, extension, baos.toByteArray());
                    }
                    result.put("message", "图片已缩放");
                    break;

                case "grayscale":
                    BufferedImage gray = new BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_BYTE_GRAY);
                    Graphics2D g2 = gray.createGraphics();
                    g2.drawImage(image, 0, 0, null);
                    g2.dispose();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(gray, "jpg", baos);
                    fileStorageService.store(imageId, extension, baos.toByteArray());
                    result.put("message", "图片已转为灰度");
                    break;

                case "info":
                default:
                    result.put("message", "图片信息获取完成");
                    break;
            }

            result.put("code", 200);
            result.put("imageId", imageId);
            result.put("operation", operation);
            result.put("width", originalWidth);
            result.put("height", originalHeight);

        } catch (IOException e) {
            log.error("图片预处理失败: id={}, op={}", imageId, operation, e);
            result.put("code", 400);
            result.put("message", "预处理失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public void deleteImage(String imageId) {
        fileStorageService.remove(imageId);
    }
}
