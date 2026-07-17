package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/location")
@Tag(name = "库位管理接口", description = "智能上架策略、ABC分类、热区管理")
public class LocationController {

    @Autowired
    private LocationService locationService;

    @PostMapping("/allocate")
    @Operation(summary = "分配库位", description = "根据ABC分类自动分配最优库位")
    public ApiResponse<Map<String, Object>> allocateLocation(
            @RequestParam String sku,
            @RequestParam int quantity) {
        Map<String, Object> result = locationService.allocateLocation(sku, quantity);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @GetMapping("/suggest")
    @Operation(summary = "推荐库位", description = "获取商品推荐上架库位列表")
    public ApiResponse<List<Map<String, Object>>> suggestLocations(@RequestParam String sku) {
        List<Map<String, Object>> suggestions = locationService.suggestLocations(sku);
        return ApiResponse.success(suggestions);
    }

    @GetMapping("/hotzone/analysis")
    @Operation(summary = "热区分析", description = "获取热区利用率分析")
    public ApiResponse<Map<String, Object>> getHotZoneAnalysis() {
        Map<String, Object> result = locationService.getHotZoneAnalysis();
        return ApiResponse.success(result);
    }

    @GetMapping("/abc/{sku}")
    @Operation(summary = "ABC分类", description = "获取商品ABC分类信息")
    public ApiResponse<Map<String, Object>> classifyProductABC(@PathVariable String sku) {
        Map<String, Object> result = locationService.classifyProductABC(sku);
        return ApiResponse.success(result);
    }

    @GetMapping("/abc/distribution")
    @Operation(summary = "ABC分布", description = "获取库位ABC分类分布统计")
    public ApiResponse<List<Map<String, Object>>> getABCDistribution() {
        List<Map<String, Object>> result = locationService.getABCDistribution();
        return ApiResponse.success(result);
    }

    @PostMapping("/optimize")
    @Operation(summary = "优化布局", description = "获取库位布局优化建议")
    public ApiResponse<Map<String, Object>> optimizeLocationLayout() {
        Map<String, Object> result = locationService.optimizeLocationLayout();
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }

    @GetMapping("/utilization/{zone}")
    @Operation(summary = "库位利用率", description = "获取指定区域库位利用率")
    public ApiResponse<Map<String, Object>> getLocationUtilization(@PathVariable String zone) {
        Map<String, Object> result = locationService.getLocationUtilization(zone);
        return ApiResponse.success(result);
    }

    @GetMapping("/empty")
    @Operation(summary = "空库位列表", description = "获取所有空闲库位")
    public ApiResponse<List<Map<String, Object>>> getEmptyLocations() {
        List<Map<String, Object>> result = locationService.getEmptyLocations();
        return ApiResponse.success(result);
    }

    @PostMapping("/release")
    @Operation(summary = "释放库位", description = "释放指定库位")
    public ApiResponse<Map<String, Object>> releaseLocation(@RequestParam String locationCode) {
        Map<String, Object> result = locationService.releaseLocation(locationCode);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        result.remove("code");
        result.remove("message");
        return ApiResponse.success(result);
    }
}