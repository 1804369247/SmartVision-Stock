package com.example.smartvisionstock.service;

import java.util.List;
import java.util.Map;

public interface VisionService {
    
    Map<String, Object> recognize(String uploadId, int angleCount);
    
    List<Map<String, Object>> detectObjects(String imageId);
    
    Map<String, Object> analyzeShelf(String uploadId);
    
    Map<String, Object> measureDistance(String imageId, Map<String, Integer> points);
}