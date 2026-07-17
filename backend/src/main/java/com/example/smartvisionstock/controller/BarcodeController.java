package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.entity.Goods;
import com.example.smartvisionstock.entity.StorageLocation;
import com.example.smartvisionstock.repository.GoodsRepository;
import com.example.smartvisionstock.repository.StorageLocationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/barcode")
@Tag(name = "条码接口", description = "条码扫描、绑定相关接口")
public class BarcodeController {

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private StorageLocationRepository storageLocationRepository;

    @GetMapping("/goods/{barcode}")
    @Operation(summary = "通过条码查询商品")
    public ApiResponse<Goods> getGoodsByBarcode(@PathVariable String barcode) {
        Optional<Goods> goodsOpt = goodsRepository.findByBarcode(barcode);
        return goodsOpt.map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "未找到条码对应的商品"));
    }

    @GetMapping("/location/{barcode}")
    @Operation(summary = "通过条码查询库位")
    public ApiResponse<StorageLocation> getLocationByBarcode(@PathVariable String barcode) {
        Optional<StorageLocation> locationOpt = storageLocationRepository.findByBarcode(barcode);
        return locationOpt.map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "未找到条码对应的库位"));
    }

    @PostMapping("/scan")
    @Operation(summary = "通用条码扫描", description = "自动识别是商品还是库位")
    public ApiResponse<Map<String, Object>> scanBarcode(@RequestBody Map<String, String> request) {
        String barcode = request.get("barcode");
        if (barcode == null || barcode.isEmpty()) {
            return ApiResponse.error(400, "条码不能为空");
        }

        Optional<Goods> goodsOpt = goodsRepository.findByBarcode(barcode);
        if (goodsOpt.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("type", "goods");
            result.put("data", goodsOpt.get());
            return ApiResponse.success(result);
        }

        Optional<StorageLocation> locationOpt = storageLocationRepository.findByBarcode(barcode);
        if (locationOpt.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("type", "location");
            result.put("data", locationOpt.get());
            return ApiResponse.success(result);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("type", "unknown");
        result.put("data", null);
        return ApiResponse.error(404, "未识别该条码", result);
    }

    @PostMapping("/goods/bind")
    @Operation(summary = "为商品绑定条码")
    public ApiResponse<Goods> bindGoodsBarcode(@RequestBody Map<String, Object> request) {
        Long goodsId = Long.valueOf(request.get("goodsId").toString());
        String barcode = request.get("barcode").toString();

        Goods goods = goodsRepository.findById(goodsId)
                .orElse(null);
        if (goods == null) {
            return ApiResponse.error(404, "商品不存在");
        }

        Optional<Goods> existing = goodsRepository.findByBarcode(barcode);
        if (existing.isPresent() && !existing.get().getId().equals(goodsId)) {
            return ApiResponse.error(409, "该条码已绑定到其他商品");
        }

        goods.setBarcode(barcode);
        goodsRepository.save(goods);
        return ApiResponse.success(goods);
    }

    @PostMapping("/location/bind")
    @Operation(summary = "为库位绑定条码")
    public ApiResponse<StorageLocation> bindLocationBarcode(@RequestBody Map<String, Object> request) {
        Long locationId = Long.valueOf(request.get("locationId").toString());
        String barcode = request.get("barcode").toString();

        StorageLocation location = storageLocationRepository.findById(locationId)
                .orElse(null);
        if (location == null) {
            return ApiResponse.error(404, "库位不存在");
        }

        Optional<StorageLocation> existing = storageLocationRepository.findByBarcode(barcode);
        if (existing.isPresent() && !existing.get().getId().equals(locationId)) {
            return ApiResponse.error(409, "该条码已绑定到其他库位");
        }

        location.setBarcode(barcode);
        storageLocationRepository.save(location);
        return ApiResponse.success(location);
    }
}