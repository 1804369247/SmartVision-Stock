package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.service.FileStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * MinIO 对象存储实现（激活 minio profile 时生效）。
 * 适合生产 / 分布式部署：文件存于 MinIO（或兼容 S3 的对象存储），
 * 支持横向扩展、与本地磁盘解耦，且可通过策略做访问管控。
 */
@Service
@Profile("minio")
public class MinioStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageServiceImpl.class);

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Value("${minio.bucket:smartvision-stock}")
    private String bucket;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        log.info("MinIO 客户端已初始化: endpoint={}, bucket={}", endpoint, bucket);
    }

    private void ensureBucket() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("已创建 MinIO 桶: {}", bucket);
            }
        } catch (Exception e) {
            log.error("检查/创建 MinIO 桶失败: {}", bucket, e);
        }
    }

    @Override
    public void store(String imageId, String extension, MultipartFile file) throws IOException {
        ensureBucket();
        String objectName = imageId + "." + extension;
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(is, file.getSize(), -1)
                    .contentType("image/" + extension)
                    .build());
            log.info("图片已存入 MinIO: {}/{}", bucket, objectName);
        } catch (Exception e) {
            throw new IOException("上传到 MinIO 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void store(String imageId, String extension, byte[] content) throws IOException {
        ensureBucket();
        String objectName = imageId + "." + extension;
        try (InputStream is = new ByteArrayInputStream(content)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(is, content.length, -1)
                    .contentType("image/" + extension)
                    .build());
        } catch (Exception e) {
            throw new IOException("上传到 MinIO 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] read(String imageId) {
        try {
            Iterator<Result<Item>> it = minioClient
                    .listObjects(ListObjectsArgs.builder().bucket(bucket).prefix(imageId + ".").build())
                    .iterator();
            if (it.hasNext()) {
                String objectName = it.next().get().objectName();
                try (InputStream is = minioClient.getObject(
                        GetObjectArgs.builder().bucket(bucket).object(objectName).build());
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) > 0) {
                        baos.write(buf, 0, n);
                    }
                    return baos.toByteArray();
                }
            }
        } catch (Exception e) {
            log.error("从 MinIO 读取图片失败: {}", imageId, e);
        }
        log.warn("MinIO 中图片不存在: {}", imageId);
        return new byte[0];
    }

    @Override
    public String resolveExtension(String imageId) {
        try {
            Iterator<Result<Item>> it = minioClient
                    .listObjects(ListObjectsArgs.builder().bucket(bucket).prefix(imageId + ".").build())
                    .iterator();
            if (it.hasNext()) {
                String name = it.next().get().objectName();
                int dot = name.lastIndexOf('.');
                return dot >= 0 ? name.substring(dot + 1) : null;
            }
        } catch (Exception e) {
            log.error("解析 MinIO 图片扩展名失败: {}", imageId, e);
        }
        return null;
    }

    @Override
    public void remove(String imageId) {
        try {
            Iterator<Result<Item>> it = minioClient
                    .listObjects(ListObjectsArgs.builder().bucket(bucket).prefix(imageId + ".").build())
                    .iterator();
            while (it.hasNext()) {
                String objectName = it.next().get().objectName();
                minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
                log.info("已从 MinIO 删除图片: {}", objectName);
            }
        } catch (Exception e) {
            log.warn("删除 MinIO 图片失败: {}", imageId, e);
        }
    }
}
