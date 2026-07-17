package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.entity.GoodsInstance;
import com.example.smartvisionstock.entity.StockCount;
import com.example.smartvisionstock.entity.StockCountItem;
import com.example.smartvisionstock.entity.StorageLocation;
import com.example.smartvisionstock.repository.GoodsInstanceRepository;
import com.example.smartvisionstock.repository.StockCountItemRepository;
import com.example.smartvisionstock.repository.StockCountRepository;
import com.example.smartvisionstock.repository.StorageLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/stock-count")
public class StockCountController {

    private static final AtomicLong COUNT_SEQ = new AtomicLong(1);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private StockCountRepository stockCountRepository;

    @Autowired
    private StockCountItemRepository stockCountItemRepository;

    @Autowired
    private StorageLocationRepository storageLocationRepository;

    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;

    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createStockCount(@RequestBody Map<String, Object> request) {
        try {
            StockCount count = new StockCount();
            count.setCountNo(generateCode("SC"));
            count.setWarehouseId(request.get("warehouseId") != null ? ((Number) request.get("warehouseId")).longValue() : null);
            count.setWarehouseName((String) request.get("warehouseName"));
            count.setArea((String) request.get("area"));
            count.setCountType((String) request.getOrDefault("countType", "FULL"));
            count.setStatus(0);
            count.setOperatorId(request.get("operatorId") != null ? ((Number) request.get("operatorId")).longValue() : null);
            count.setOperatorName((String) request.get("operatorName"));
            count.setRemark((String) request.get("remark"));

            stockCountRepository.save(count);

            List<StorageLocation> locations;
            if (count.getArea() != null && !count.getArea().isEmpty()) {
                locations = storageLocationRepository.findByArea(count.getArea());
            } else {
                locations = storageLocationRepository.findAll();
            }

            for (StorageLocation loc : locations) {
                StockCountItem item = new StockCountItem();
                item.setCountId(count.getId());
                item.setLocationId(loc.getId());
                item.setLocationCode(loc.getLocationCode());
                item.setArea(loc.getArea());
                item.setStatus("PENDING");

                GoodsInstance instance = goodsInstanceRepository.findFirstByLocationId(loc.getId()).orElse(null);
                if (instance != null) {
                    item.setGoodsId(instance.getGoodsId());
                    item.setBatchNo(instance.getBatchNo());
                    item.setExpectedQty(instance.getQuantity());
                    item.setActualQty(null);
                    item.setDiffQty(null);
                } else {
                    item.setExpectedQty(0);
                    item.setActualQty(null);
                    item.setDiffQty(null);
                }

                stockCountItemRepository.save(item);
            }

            count.setTotalCount(locations.size());
            stockCountRepository.save(count);

            Map<String, Object> result = new HashMap<>();
            result.put("countNo", count.getCountNo());
            result.put("totalItems", locations.size());

            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getStockCountList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<StockCount> countPage;

        if (status != null) {
            countPage = stockCountRepository.findByStatus(status, pageable);
        } else {
            countPage = stockCountRepository.findAll(pageable);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", countPage.getContent());
        result.put("totalElements", countPage.getTotalElements());
        result.put("totalPages", countPage.getTotalPages());
        result.put("currentPage", countPage.getNumber());
        result.put("pageSize", countPage.getSize());
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getStockCount(@PathVariable Long id) {
        StockCount count = stockCountRepository.findById(id).orElse(null);
        if (count == null) {
            return ApiResponse.notFound("盘点单不存在");
        }

        List<StockCountItem> items = stockCountItemRepository.findByCountId(id);

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("items", items);
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/start")
    public ApiResponse<Object> startStockCount(@PathVariable Long id) {
        StockCount count = stockCountRepository.findById(id).orElse(null);
        if (count == null) {
            return ApiResponse.notFound("盘点单不存在");
        }
        if (count.getStatus() != 0) {
            return ApiResponse.error(400, "只能开始待盘点状态的盘点单");
        }

        count.setStatus(1);
        count.setStartTime(LocalDateTime.now());
        stockCountRepository.save(count);

        return ApiResponse.success("盘点开始");
    }

    @PostMapping("/{id}/items")
    public ApiResponse<Object> updateStockCountItems(@PathVariable Long id, @RequestBody List<Map<String, Object>> items) {
        StockCount count = stockCountRepository.findById(id).orElse(null);
        if (count == null) {
            return ApiResponse.notFound("盘点单不存在");
        }
        if (count.getStatus() != 1) {
            return ApiResponse.error(400, "只能在盘点进行中状态下更新数据");
        }

        for (Map<String, Object> itemData : items) {
            Long itemId = ((Number) itemData.get("id")).longValue();
            StockCountItem item = stockCountItemRepository.findById(itemId).orElse(null);
            if (item != null && item.getCountId().equals(id)) {
                Integer actualQty = itemData.get("actualQty") != null ? ((Number) itemData.get("actualQty")).intValue() : null;
                item.setActualQty(actualQty);
                if (item.getExpectedQty() != null && actualQty != null) {
                    item.setDiffQty(actualQty - item.getExpectedQty());
                    item.setStatus(item.getDiffQty() == 0 ? "MATCH" : "DIFF");
                } else {
                    item.setDiffQty(null);
                    item.setStatus(actualQty != null ? "COUNTED" : "PENDING");
                }
                stockCountItemRepository.save(item);
            }
        }

        return ApiResponse.success("更新成功");
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<Object> completeStockCount(@PathVariable Long id) {
        StockCount count = stockCountRepository.findById(id).orElse(null);
        if (count == null) {
            return ApiResponse.notFound("盘点单不存在");
        }
        if (count.getStatus() != 1) {
            return ApiResponse.error(400, "只能在盘点进行中状态下完成");
        }

        List<StockCountItem> items = stockCountItemRepository.findByCountId(id);
        int matchCount = 0;
        int diffCount = 0;

        for (StockCountItem item : items) {
            if ("MATCH".equals(item.getStatus())) {
                matchCount++;
            } else if ("DIFF".equals(item.getStatus())) {
                diffCount++;
            }
        }

        count.setStatus(2);
        count.setEndTime(LocalDateTime.now());
        count.setMatchCount(matchCount);
        count.setDiffCount(diffCount);
        stockCountRepository.save(count);

        return ApiResponse.success("盘点完成");
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<Object> confirmStockCount(@PathVariable Long id) {
        StockCount count = stockCountRepository.findById(id).orElse(null);
        if (count == null) {
            return ApiResponse.notFound("盘点单不存在");
        }
        if (count.getStatus() != 2) {
            return ApiResponse.error(400, "只能确认已完成的盘点单");
        }

        List<StockCountItem> diffItems = stockCountItemRepository.findByCountIdAndStatus(id, "DIFF");
        for (StockCountItem item : diffItems) {
            StorageLocation location = storageLocationRepository.findById(item.getLocationId()).orElse(null);
            if (location != null) {
                GoodsInstance instance = goodsInstanceRepository.findFirstByLocationId(item.getLocationId()).orElse(null);
                if (instance != null) {
                    instance.setQuantity(item.getActualQty());
                    goodsInstanceRepository.save(instance);
                }
                location.setStatus(item.getActualQty() != null && item.getActualQty() > 0 ? 1 : 0);
                storageLocationRepository.save(location);
            }
        }

        count.setStatus(3);
        stockCountRepository.save(count);

        return ApiResponse.success("盘点确认完成，库存已更新");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> deleteStockCount(@PathVariable Long id) {
        StockCount count = stockCountRepository.findById(id).orElse(null);
        if (count == null) {
            return ApiResponse.notFound("盘点单不存在");
        }
        if (count.getStatus() >= 2) {
            return ApiResponse.error(400, "已完成的盘点单不能删除");
        }

        stockCountItemRepository.deleteByCountId(id);
        stockCountRepository.deleteById(id);

        return ApiResponse.success("删除成功");
    }

    private String generateCode(String prefix) {
        return prefix + DT_FMT.format(LocalDateTime.now()) + String.format("%04d", COUNT_SEQ.getAndIncrement() % 9999 + 1);
    }
}