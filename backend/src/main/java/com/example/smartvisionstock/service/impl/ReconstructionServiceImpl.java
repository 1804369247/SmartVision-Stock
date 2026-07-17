package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.entity.ReconstructionTask;
import com.example.smartvisionstock.repository.ReconstructionTaskRepository;
import com.example.smartvisionstock.service.ReconstructionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 三维重建服务 —— 基于数据库持久化 ReconstructionTask。
 * 重建过程在当前阶段为模拟进度（生产环境应集成 COLMAP/OpenMVS 等重建算法）。
 */
@Service
public class ReconstructionServiceImpl implements ReconstructionService {

    @Autowired
    private ReconstructionTaskRepository taskRepository;

    @Override
    public Map<String, Object> reconstruct(String uploadId) {
        Map<String, Object> result = new HashMap<>();

        String taskId = UUID.randomUUID().toString();

        ReconstructionTask task = new ReconstructionTask();
        task.setTaskId(taskId);
        task.setUploadId(uploadId);
        task.setStatus("PROCESSING");
        task.setProgress(0);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskRepository.save(task);

        // 启动后台模拟重建进程
        simulateReconstruction(taskId);

        result.put("code", 200);
        result.put("taskId", taskId);
        result.put("message", "重建任务已启动");
        return result;
    }

    /**
     * 模拟重建进度（后台线程）。生产环境应调用真实重建算法。
     */
    private void simulateReconstruction(String taskId) {
        new Thread(() -> {
            try {
                for (int i = 0; i <= 100; i += 10) {
                    Optional<ReconstructionTask> optional = taskRepository.findByTaskId(taskId);
                    if (!optional.isPresent()) return;

                    ReconstructionTask task = optional.get();
                    // 检查是否已被取消
                    if ("CANCELLED".equals(task.getStatus())) return;

                    task.setProgress(i);
                    task.setUpdateTime(LocalDateTime.now());
                    taskRepository.save(task);
                    Thread.sleep(500);
                }

                // 重建完成
                Optional<ReconstructionTask> optional = taskRepository.findByTaskId(taskId);
                if (optional.isPresent()) {
                    ReconstructionTask task = optional.get();
                    task.setStatus("COMPLETED");
                    task.setProgress(100);
                    task.setModelId("model_" + taskId.substring(0, 8));
                    task.setUpdateTime(LocalDateTime.now());
                    taskRepository.save(task);
                }
            } catch (InterruptedException e) {
                Optional<ReconstructionTask> optional = taskRepository.findByTaskId(taskId);
                if (optional.isPresent()) {
                    ReconstructionTask task = optional.get();
                    task.setStatus("FAILED");
                    task.setErrorMessage("重建过程被中断");
                    task.setUpdateTime(LocalDateTime.now());
                    taskRepository.save(task);
                }
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    public Map<String, Object> getReconstructionProgress(String taskId) {
        Map<String, Object> result = new HashMap<>();

        Optional<ReconstructionTask> optional = taskRepository.findByTaskId(taskId);
        if (!optional.isPresent()) {
            result.put("code", 400);
            result.put("message", "任务不存在");
            return result;
        }

        ReconstructionTask task = optional.get();
        result.put("code", 200);
        result.put("taskId", taskId);
        result.put("progress", task.getProgress());
        result.put("status", task.getStatus());

        if ("COMPLETED".equals(task.getStatus())) {
            result.put("modelId", task.getModelId());
        }

        return result;
    }

    @Override
    public Map<String, Object> cancelReconstruction(String taskId) {
        Map<String, Object> result = new HashMap<>();

        Optional<ReconstructionTask> optional = taskRepository.findByTaskId(taskId);
        if (!optional.isPresent()) {
            result.put("code", 400);
            result.put("message", "任务不存在");
            return result;
        }

        ReconstructionTask task = optional.get();
        task.setStatus("CANCELLED");
        task.setUpdateTime(LocalDateTime.now());
        taskRepository.save(task);

        result.put("code", 200);
        result.put("message", "任务已取消");
        return result;
    }

    @Override
    public byte[] exportModel(String modelId, String format) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 3D Model Export - SmartVision Stock\n");
        sb.append("# ModelID: ").append(modelId).append("\n");
        sb.append("# Format: ").append(format != null ? format : "obj").append("\n");
        sb.append("# Export Time: ").append(LocalDateTime.now()).append("\n");
        sb.append("# ========================================\n");
        sb.append("# 此处为导出占位内容\n");
        sb.append("# 生产环境应输出真实的3D模型文件（OBJ/GLB/FBX）\n");

        return sb.toString().getBytes();
    }

    @Override
    public Map<String, Object> optimizeModel(String modelId, int quality) {
        Map<String, Object> result = new HashMap<>();

        // 基于质量参数估算顶点减少比例
        double ratio = Math.min(1.0, Math.max(0.1, quality / 100.0));
        int originalVertices = 12500; // 默认值，生产环境从模型文件解析
        int optimizedVertices = (int) (originalVertices * ratio);

        result.put("code", 200);
        result.put("modelId", modelId);
        result.put("quality", quality);
        result.put("message", "模型优化完成");
        result.put("originalVertices", originalVertices);
        result.put("optimizedVertices", optimizedVertices);
        result.put("compressionRatio", String.format("%.1f%%", (1 - ratio) * 100));

        return result;
    }
}
