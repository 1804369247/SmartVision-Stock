package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.request.MoveRequest;
import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.dto.response.LocationInfoDTO;
import com.example.smartvisionstock.entity.Goods;
import com.example.smartvisionstock.entity.GoodsInstance;
import com.example.smartvisionstock.entity.StorageLocation;
import com.example.smartvisionstock.repository.StorageLocationRepository;
import com.example.smartvisionstock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StockController {

    @Autowired
    private StockService stockService;

    @Autowired
    private StorageLocationRepository storageLocationRepository;

    @GetMapping("/goods")
    public ApiResponse<List<Goods>> getAllGoods() {
        return ApiResponse.success(stockService.getAllGoods());
    }

    @GetMapping("/locations")
    public ApiResponse<Map<String, Object>> getAllLocations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) Integer status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "locationCode"));
        
        List<LocationInfoDTO> all = stockService.getAllLocations();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            all = all.stream()
                    .filter(l -> (l.getLocationCode() != null && l.getLocationCode().toLowerCase().contains(kw))
                            || (l.getGoodsName() != null && l.getGoodsName().toLowerCase().contains(kw))
                            || (l.getGoodsCode() != null && l.getGoodsCode().toLowerCase().contains(kw))
                            || (l.getBatchNo() != null && l.getBatchNo().toLowerCase().contains(kw)))
                    .toList();
        }
        
        if (area != null && !area.trim().isEmpty()) {
            all = all.stream().filter(l -> area.equals(l.getArea())).toList();
        }
        
        if (status != null) {
            all = all.stream().filter(l -> status.equals(l.getStatus())).toList();
        }
        
        int start = page * size;
        int end = Math.min(start + size, all.size());
        List<LocationInfoDTO> pageContent = start < all.size() ? all.subList(start, end) : List.of();
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", pageContent);
        result.put("totalElements", all.size());
        result.put("totalPages", (int) Math.ceil((double) all.size() / size));
        result.put("currentPage", page);
        result.put("pageSize", size);
        return ApiResponse.success(result);
    }

    @GetMapping("/locations/{id}")
    public ApiResponse<StorageLocation> getLocationById(@PathVariable Long id) {
        StorageLocation location = storageLocationRepository.findById(id).orElse(null);
        if (location == null) {
            return ApiResponse.error(400, "库位不存在");
        }
        return ApiResponse.success(location);
    }

    @PostMapping("/locations")
    public ApiResponse<Object> createLocation(@RequestBody StorageLocation location) {
        try {
            location.setId(null);
            location.setStatus(0);
            storageLocationRepository.save(location);
            return ApiResponse.success("库位创建成功");
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @PutMapping("/locations/{id}")
    public ApiResponse<Object> updateLocation(@PathVariable Long id, @RequestBody StorageLocation location) {
        try {
            StorageLocation existing = storageLocationRepository.findById(id).orElseThrow(() -> new RuntimeException("库位不存在"));
            existing.setLocationCode(location.getLocationCode());
            existing.setArea(location.getArea());
            existing.setAttribute(location.getAttribute());
            existing.setDescription(location.getDescription());
            existing.setXCoord(location.getXCoord());
            existing.setYCoord(location.getYCoord());
            existing.setZCoord(location.getZCoord());
            storageLocationRepository.save(existing);
            return ApiResponse.success("库位更新成功");
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @DeleteMapping("/locations/{id}")
    public ApiResponse<Object> deleteLocation(@PathVariable Long id) {
        try {
            StorageLocation location = storageLocationRepository.findById(id).orElseThrow(() -> new RuntimeException("库位不存在"));
            if (location.getStatus() != 0) {
                return ApiResponse.error(400, "只能删除空闲库位");
            }
            storageLocationRepository.deleteById(id);
            return ApiResponse.success("库位删除成功");
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/locations/area")
    public ApiResponse<List<LocationInfoDTO>> getLocationsByArea(@RequestParam String area) {
        List<StorageLocation> locations = storageLocationRepository.findByArea(area);
        List<LocationInfoDTO> result = locations.stream().map(this::convertToLocationInfoDTO).collect(java.util.stream.Collectors.toList());
        return ApiResponse.success(result);
    }

    private LocationInfoDTO convertToLocationInfoDTO(StorageLocation location) {
        LocationInfoDTO dto = new LocationInfoDTO();
        dto.setId(location.getId());
        dto.setLocationCode(location.getLocationCode());
        dto.setArea(location.getArea());
        dto.setStatus(location.getStatus());
        dto.setXCoord(location.getXCoord());
        dto.setYCoord(location.getYCoord());
        dto.setZCoord(location.getZCoord());
        dto.setCurrentGoodsInstanceId(location.getCurrentGoodsInstanceId());
        dto.setAttribute(location.getAttribute() != null ? location.getAttribute() : "NORMAL");
        dto.setDescription(location.getDescription());
        return dto;
    }

    @GetMapping("/instances")
    public ApiResponse<Page<GoodsInstance>> getAllInstances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(stockService.getAllInstances(page, size));
    }

    @PostMapping("/move")
    public ApiResponse<Object> move(@RequestBody @Valid MoveRequest request) {
        Map<String, Object> result = stockService.move(request.getGoodsInstanceId(), request.getTargetLocationId());
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success("库存移动成功");
    }

    @PostMapping("/adjust")
    public ApiResponse<Object> adjustInventory(@RequestBody Map<String, Object> request) {
        Long goodsInstanceId = request.get("goodsInstanceId") instanceof Long 
                ? (Long) request.get("goodsInstanceId")
                : Long.parseLong(request.get("goodsInstanceId").toString());
        Integer quantity = request.get("quantity") instanceof Integer
                ? (Integer) request.get("quantity")
                : Integer.parseInt(request.get("quantity").toString());
        Map<String, Object> result = stockService.adjustInventory(goodsInstanceId, quantity);
        Integer code = (Integer) result.get("code");
        if (code != null && code != 200) {
            return ApiResponse.error(code, (String) result.get("message"));
        }
        return ApiResponse.success("库存调整成功");
    }
}
