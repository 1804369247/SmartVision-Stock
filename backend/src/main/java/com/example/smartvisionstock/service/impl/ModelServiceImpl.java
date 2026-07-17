package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.entity.Model3D;
import com.example.smartvisionstock.repository.Model3DRepository;
import com.example.smartvisionstock.service.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 3D 模型管理服务 —— 基于数据库持久化 Model3D。
 * 模型元数据存数据库，模型文件存磁盘。
 */
@Service
public class ModelServiceImpl implements ModelService {

    private static final Logger log = LoggerFactory.getLogger(ModelServiceImpl.class);
    private static final String MODEL_DIR = "./uploads/models/";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private Model3DRepository modelRepository;

    @Override
    public Map<String, Object> saveModel(String name, byte[] modelData, String format, String uploadId) {
        Map<String, Object> result = new HashMap<>();

        String modelId = UUID.randomUUID().toString();
        String fileName = modelId + "." + (format != null ? format.toLowerCase() : "obj");
        String filePath = MODEL_DIR + fileName;

        // 确保目录存在
        try {
            Files.createDirectories(Paths.get(MODEL_DIR));
            Files.write(Paths.get(filePath), modelData);
        } catch (IOException e) {
            result.put("code", 400);
            result.put("message", "模型文件写入失败：" + e.getMessage());
            return result;
        }

        Model3D model = new Model3D();
        model.setModelId(modelId);
        model.setName(name);
        model.setFormat(format != null ? format.toLowerCase() : "obj");
        model.setFileSize((long) modelData.length);
        model.setFilePath(filePath);
        model.setUploadId(uploadId);
        model.setVersion("1.0");
        model.setCreateTime(LocalDateTime.now());
        model.setUpdateTime(LocalDateTime.now());

        modelRepository.save(model);

        result.put("code", 200);
        result.put("modelId", modelId);
        result.put("message", "模型保存成功");
        return result;
    }

    @Override
    public Map<String, Object> getModel(String modelId) {
        Optional<Model3D> optional = modelRepository.findByModelId(modelId);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "模型不存在");
            return result;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", modelToMap(optional.get()));
        return result;
    }

    @Override
    public List<Map<String, Object>> listModels(int page, int size, String name) {
        List<Model3D> models = modelRepository.findByNameContaining(
                name != null && !name.isEmpty() ? name : null,
                PageRequest.of(page, size));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Model3D model : models) {
            result.add(modelToMap(model));
        }
        return result;
    }

    @Override
    public Map<String, Object> updateModel(String modelId, String name, String description) {
        Optional<Model3D> optional = modelRepository.findByModelId(modelId);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "模型不存在");
            return result;
        }

        Model3D model = optional.get();
        if (name != null && !name.isEmpty()) {
            model.setName(name);
        }
        if (description != null) {
            model.setDescription(description);
        }
        model.setUpdateTime(LocalDateTime.now());
        modelRepository.save(model);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "模型更新成功");
        return result;
    }

    @Override
    @Transactional
    public void deleteModel(String modelId) {
        Optional<Model3D> optional = modelRepository.findByModelId(modelId);
        if (optional.isPresent()) {
            Model3D model = optional.get();
            // 删除磁盘文件
            if (model.getFilePath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(model.getFilePath()));
                } catch (IOException e) {
                    log.warn("Failed to delete model file: {}", model.getFilePath(), e);
                }
            }
            modelRepository.deleteByModelId(modelId);
        }
    }

    @Override
    public Map<String, Object> mergeModel(String modelId, String sceneId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("modelId", modelId);
        result.put("sceneId", sceneId);
        result.put("message", "模型融合成功");
        result.put("mergedSceneId", "scene_" + System.currentTimeMillis());
        return result;
    }

    @Override
    public Map<String, Object> getModelMetadata(String modelId) {
        Optional<Model3D> optional = modelRepository.findByModelId(modelId);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "模型不存在");
            return result;
        }

        Model3D model = optional.get();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("id", model.getModelId());
        metadata.put("name", model.getName());
        metadata.put("format", model.getFormat());
        metadata.put("size", model.getFileSize());
        metadata.put("createdAt", model.getCreateTime() != null ? model.getCreateTime().format(DT_FMT) : null);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("metadata", metadata);
        return result;
    }

    private Map<String, Object> modelToMap(Model3D model) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", model.getModelId());
        map.put("name", model.getName());
        map.put("description", model.getDescription());
        map.put("format", model.getFormat() != null ? model.getFormat().toUpperCase() : "OBJ");
        map.put("size", model.getFileSize());
        map.put("uploadId", model.getUploadId());
        map.put("version", model.getVersion());
        map.put("vertexCount", model.getVertexCount());
        map.put("faceCount", model.getFaceCount());
        map.put("createdAt", model.getCreateTime() != null ? model.getCreateTime().format(DT_FMT) : null);
        return map;
    }
}
