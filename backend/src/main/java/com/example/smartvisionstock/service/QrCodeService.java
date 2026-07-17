package com.example.smartvisionstock.service;

import java.util.List;
import java.util.Map;

public interface QrCodeService {
    
    byte[] generateQrCode(String content, int width, int height);
    
    byte[] generateGoodsQrCode(Long goodsId);
    
    byte[] generateLocationQrCode(Long locationId);
    
    byte[] generateBatchQrCode(Long batchId);
    
    List<byte[]> batchGenerateGoodsQrCodes(List<Long> goodsIds);
    
    List<byte[]> batchGenerateLocationQrCodes(List<Long> locationIds);
    
    Map<String, Object> scanQrCode(byte[] imageData);
    
    Map<String, Object> scanQrCodeBase64(String base64Image);
}