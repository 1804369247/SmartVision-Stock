package com.example.smartvisionstock.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 3D 模型 —— 存储模型元数据，模型文件存储在磁盘。
 * 数据持久化到数据库，不再使用内存 Map。
 */
@Entity
@Table(name = "model_3d")
public class Model3D {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 模型编号 */
    @Column(unique = true, nullable = false)
    private String modelId;

    /** 模型名称 */
    @Column(nullable = false)
    private String name;

    /** 模型描述 */
    @Column(length = 500)
    private String description;

    /** 文件格式（obj / glb / fbx / stl 等） */
    private String format;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 磁盘上的文件路径 */
    private String filePath;

    /** 关联的上传ID */
    private String uploadId;

    /** 版本号 */
    private String version;

    /** 顶点数 */
    private Integer vertexCount;

    /** 面数 */
    private Integer faceCount;

    /** 创建时间 */
    @Column(nullable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    // ---- Getters / Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Integer getVertexCount() { return vertexCount; }
    public void setVertexCount(Integer vertexCount) { this.vertexCount = vertexCount; }

    public Integer getFaceCount() { return faceCount; }
    public void setFaceCount(Integer faceCount) { this.faceCount = faceCount; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
