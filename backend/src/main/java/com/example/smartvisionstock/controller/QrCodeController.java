package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.service.QrCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qrcode")
public class QrCodeController {

    @Autowired
    private QrCodeService qrCodeService;

    @GetMapping("/generate")
    public ResponseEntity<byte[]> generateQrCode(
            @RequestParam String content,
            @RequestParam(defaultValue = "200") int width,
            @RequestParam(defaultValue = "200") int height) {
        byte[] qrCode = qrCodeService.generateQrCode(content, width, height);
        if (qrCode.length == 0) {
            return ResponseEntity.badRequest().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData("inline", "qrcode.png");
        return ResponseEntity.ok().headers(headers).body(qrCode);
    }

    @GetMapping("/goods/{goodsId}")
    public ResponseEntity<byte[]> generateGoodsQrCode(@PathVariable Long goodsId) {
        byte[] qrCode = qrCodeService.generateGoodsQrCode(goodsId);
        if (qrCode.length == 0) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData("inline", "goods_" + goodsId + ".png");
        return ResponseEntity.ok().headers(headers).body(qrCode);
    }

    @GetMapping("/location/{locationId}")
    public ResponseEntity<byte[]> generateLocationQrCode(@PathVariable Long locationId) {
        byte[] qrCode = qrCodeService.generateLocationQrCode(locationId);
        if (qrCode.length == 0) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData("inline", "location_" + locationId + ".png");
        return ResponseEntity.ok().headers(headers).body(qrCode);
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<byte[]> generateBatchQrCode(@PathVariable Long batchId) {
        byte[] qrCode = qrCodeService.generateBatchQrCode(batchId);
        if (qrCode.length == 0) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData("inline", "batch_" + batchId + ".png");
        return ResponseEntity.ok().headers(headers).body(qrCode);
    }

    @PostMapping("/goods/batch")
    public ApiResponse<Map<String, Object>> batchGenerateGoodsQrCodes(@RequestBody List<Long> goodsIds) {
        try {
            List<byte[]> qrCodes = qrCodeService.batchGenerateGoodsQrCodes(goodsIds);
            Map<String, Object> result = new HashMap<>();
            result.put("count", qrCodes.size());
            return ApiResponse.success("批量生成成功", result);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/locations/batch")
    public ApiResponse<Map<String, Object>> batchGenerateLocationQrCodes(@RequestBody List<Long> locationIds) {
        try {
            List<byte[]> qrCodes = qrCodeService.batchGenerateLocationQrCodes(locationIds);
            Map<String, Object> result = new HashMap<>();
            result.put("count", qrCodes.size());
            return ApiResponse.success("批量生成成功", result);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/scan")
    public ApiResponse<Map<String, Object>> scanQrCode(@RequestBody Map<String, String> request) {
        String base64Image = request.get("image");
        Map<String, Object> result = qrCodeService.scanQrCodeBase64(base64Image);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success(result);
    }
}