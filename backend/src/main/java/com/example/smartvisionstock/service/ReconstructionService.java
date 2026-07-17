package com.example.smartvisionstock.service;

import java.util.Map;

public interface ReconstructionService {
    
    Map<String, Object> reconstruct(String uploadId);
    
    Map<String, Object> getReconstructionProgress(String taskId);
    
    Map<String, Object> cancelReconstruction(String taskId);
    
    byte[] exportModel(String modelId, String format);
    
    Map<String, Object> optimizeModel(String modelId, int quality);
}