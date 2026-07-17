package com.example.smartvisionstock.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ImageService {
    
    Map<String, Object> uploadImage(MultipartFile file, String type);
    
    Map<String, Object> uploadMultipleImages(MultipartFile[] files);
    
    byte[] getImage(String imageId);
    
    Map<String, Object> preprocessImage(String imageId, String operation);
    
    void deleteImage(String imageId);
}