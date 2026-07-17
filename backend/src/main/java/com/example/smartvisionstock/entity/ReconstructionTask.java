package com.example.smartvisionstock.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 三维重建任务 —— 将上传的图片/视频重建为3D模型。
 * 数据持久化到数据库，不再使用内存 Map。
 */
@Entity
@Table(name = "reconstruction_task")
public class ReconstructionTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任务编号 */
    @Column(unique = true, nullable = false)
    private String taskId;

    /** 关联的上传ID（图片/视频来源） */
    private String uploadId;

    /** 状态：PROCESSING / COMPLETED / FAILED / CANCELLED */
    @Column(nullable = false)
    private String status;

    /** 重建进度（0-100） */
    private Integer progress;

    /** 重建完成后的模型ID */
    private String modelId;

    /** 输出格式（obj / glb / fbx 等） */
    private String outputFormat;

    /** 创建时间 */
    @Column(nullable = false)
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;

    /** 错误信息 */
    @Column(length = 1000)
    private String errorMessage;

    // ---- Getters / Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }

    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
