package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.entity.Warehouse;
import com.example.smartvisionstock.repository.GoodsInstanceRepository;
import com.example.smartvisionstock.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/basic/warehouses")
public class WarehouseBasicController {

    private static final AtomicLong WAREHOUSE_SEQ = new AtomicLong(1);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> getWarehouses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            List<Warehouse> all = warehouseRepository.findByStatus(1);
            List<Warehouse> filtered = all.stream()
                    .filter(w -> (w.getName() != null && w.getName().toLowerCase().contains(kw))
                            || (w.getCode() != null && w.getCode().toLowerCase().contains(kw))
                            || (w.getAddress() != null && w.getAddress().toLowerCase().contains(kw))
                            || (w.getManager() != null && w.getManager().toLowerCase().contains(kw)))
                    .toList();
            int start = page * size;
            int end = Math.min(start + size, filtered.size());
            List<Warehouse> pageContent = start < filtered.size() ? filtered.subList(start, end) : List.of();
            
            Map<String, Object> result = new HashMap<>();
            result.put("content", pageContent);
            result.put("totalElements", filtered.size());
            result.put("totalPages", (int) Math.ceil((double) filtered.size() / size));
            result.put("currentPage", page);
            result.put("pageSize", size);
            return ApiResponse.success(result);
        }
        
        Page<Warehouse> warehousePage = warehouseRepository.findByStatus(1, pageable);
        Map<String, Object> result = new HashMap<>();
        result.put("content", warehousePage.getContent());
        result.put("totalElements", warehousePage.getTotalElements());
        result.put("totalPages", warehousePage.getTotalPages());
        result.put("currentPage", warehousePage.getNumber());
        result.put("pageSize", warehousePage.getSize());
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<Object> createWarehouse(@RequestBody Warehouse warehouse) {
        try {
            warehouse.setId(null);
            warehouse.setStatus(1);
            warehouse.setCode(generateCode("WH"));
            warehouseRepository.save(warehouse);
            return ApiResponse.success("仓库创建成功");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Object> updateWarehouse(@PathVariable Long id, @RequestBody Warehouse warehouse) {
        try {
            Warehouse existing = warehouseRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("仓库不存在"));
            existing.setName(warehouse.getName());
            existing.setAddress(warehouse.getAddress());
            existing.setManager(warehouse.getManager());
            existing.setPhone(warehouse.getPhone());
            warehouseRepository.save(existing);
            return ApiResponse.success("仓库更新成功");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> deleteWarehouse(@PathVariable Long id) {
        try {
            Warehouse warehouse = warehouseRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("仓库不存在"));

            long stockCount = goodsInstanceRepository.countByLocationIdIsNotNull();
            if (stockCount > 0) {
                return ApiResponse.error(400, "仓库下仍有 " + stockCount + " 条在库货物，请先清空或转移后重试");
            }

            warehouse.setStatus(0);
            warehouseRepository.save(warehouse);
            return ApiResponse.success("仓库删除成功");
        } catch (Exception e) {
            return ApiResponse.error(400, "删除失败：" + e.getMessage());
        }
    }

    private String generateCode(String prefix) {
        return prefix + DT_FMT.format(LocalDateTime.now()) + String.format("%04d", WAREHOUSE_SEQ.getAndIncrement() % 9999 + 1);
    }
}