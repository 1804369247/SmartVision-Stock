package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地磁盘文件存储实现（默认，未激活 minio profile 时生效）。
 * 文件路径：<upload-dir>/<imageId>.<extension>
 */
@Service
@Profile("!minio")
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageServiceImpl.class);

    @Value("${file.upload-dir:./uploads/images}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            log.info("本地文件存储目录: {}", uploadPath);
        } catch (IOException e) {
            log.error("无法创建上传目录: {}", uploadPath, e);
        }
    }

    @Override
    public void store(String imageId, String extension, MultipartFile file) throws IOException {
        Path filePath = uploadPath.resolve(imageId + "." + extension);
        file.transferTo(filePath.toFile());
    }

    @Override
    public void store(String imageId, String extension, byte[] content) throws IOException {
        Path filePath = uploadPath.resolve(imageId + "." + extension);
        Files.write(filePath, content);
    }

    @Override
    public byte[] read(String imageId) {
        String[] extensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};
        for (String ext : extensions) {
            Path path = uploadPath.resolve(imageId + ext);
            if (Files.exists(path)) {
                try {
                    return Files.readAllBytes(path);
                } catch (IOException e) {
                    log.error("读取图片文件失败: {}", path, e);
                    return new byte[0];
                }
            }
        }
        log.warn("图片不存在: {}", imageId);
        return new byte[0];
    }

    @Override
    public String resolveExtension(String imageId) {
        String[] extensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};
        for (String ext : extensions) {
            if (Files.exists(uploadPath.resolve(imageId + ext))) {
                return ext.substring(1);
            }
        }
        return null;
    }

    @Override
    public void remove(String imageId) {
        String[] extensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};
        for (String ext : extensions) {
            Path path = uploadPath.resolve(imageId + ext);
            try {
                if (Files.deleteIfExists(path)) {
                    log.info("图片已删除: {}", imageId);
                }
            } catch (IOException e) {
                log.warn("删除图片文件失败: {}", path, e);
            }
        }
    }
}
