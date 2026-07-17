package com.example.smartvisionstock.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件存储抽象层。
 * 将图片等文件的存取与具体存储后端解耦：
 *  - 默认（!minio profile）：LocalFileStorageServiceImpl，存本地磁盘（便于演示与开发）。
 *  - minio profile：MinioStorageServiceImpl，存 MinIO 对象存储（生产推荐，支持分布式/云）。
 * 通过依赖该接口，业务层（ImageServiceImpl）无需关心底层是磁盘还是对象存储。
 */
public interface FileStorageService {

    /**
     * 保存文件
     * @param imageId 唯一标识（不含扩展名）
     * @param extension 扩展名（不含点，如 jpg/png）
     * @param file 上传的文件
     */
    void store(String imageId, String extension, MultipartFile file) throws IOException;

    /**
     * 保存文件（字节内容，用于图片预处理后回写）
     */
    void store(String imageId, String extension, byte[] content) throws IOException;

    /**
     * 读取文件字节。imageId 不含扩展名，由实现按常见扩展名探测。
     * @return 文件内容；不存在时返回空字节数组
     */
    byte[] read(String imageId);

    /**
     * 解析 imageId 对应的实际扩展名（不含点），不存在返回 null。
     */
    String resolveExtension(String imageId);

    /**
     * 删除文件
     */
    void remove(String imageId);
}
