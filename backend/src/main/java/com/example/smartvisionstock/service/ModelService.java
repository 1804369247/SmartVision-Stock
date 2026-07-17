package com.example.smartvisionstock.service;

import java.util.List;
import java.util.Map;

public interface ModelService {
    
    Map<String, Object> saveModel(String name, byte[] modelData, String format, String uploadId);
    
    Map<String, Object> getModel(String modelId);
    
    List<Map<String, Object>> listModels(int page, int size, String name);
    
    Map<String, Object> updateModel(String modelId, String name, String description);
    
    void deleteModel(String modelId);
    
    Map<String, Object> mergeModel(String modelId, String sceneId);
    
    Map<String, Object> getModelMetadata(String modelId);
}