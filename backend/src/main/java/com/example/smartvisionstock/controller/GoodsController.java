package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.entity.Goods;
import com.example.smartvisionstock.repository.GoodsInstanceRepository;
import com.example.smartvisionstock.repository.GoodsRepository;
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
@RequestMapping("/api/basic/goods")
public class GoodsController {

    private static final AtomicLong GOODS_SEQ = new AtomicLong(1);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> getGoodsList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Goods> goodsPage;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            List<Goods> all = goodsRepository.findByEnabledTrue();
            List<Goods> filtered = all.stream()
                    .filter(g -> (g.getName() != null && g.getName().toLowerCase().contains(kw))
                            || (g.getCode() != null && g.getCode().toLowerCase().contains(kw))
                            || (g.getSpec() != null && g.getSpec().toLowerCase().contains(kw)))
                    .toList();
            int start = page * size;
            int end = Math.min(start + size, filtered.size());
            List<Goods> pageContent = start < filtered.size() ? filtered.subList(start, end) : List.of();
            
            Map<String, Object> result = new HashMap<>();
            result.put("content", pageContent);
            result.put("totalElements", filtered.size());
            result.put("totalPages", (int) Math.ceil((double) filtered.size() / size));
            result.put("currentPage", page);
            result.put("pageSize", size);
            return ApiResponse.success(result);
        } else if (category != null && !category.trim().isEmpty()) {
            goodsPage = goodsRepository.findByCategoryAndEnabledTrue(category, pageable);
        } else {
            goodsPage = goodsRepository.findByEnabledTrue(pageable);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", goodsPage.getContent());
        result.put("totalElements", goodsPage.getTotalElements());
        result.put("totalPages", goodsPage.getTotalPages());
        result.put("currentPage", goodsPage.getNumber());
        result.put("pageSize", goodsPage.getSize());
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<Goods> getGoodsById(@PathVariable Long id) {
        Goods goods = goodsRepository.findById(id).orElse(null);
        if (goods == null) {
            return ApiResponse.notFound("货物不存在");
        }
        return ApiResponse.success(goods);
    }

    @GetMapping("/search")
    public ApiResponse<List<Goods>> searchGoods(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ApiResponse.success(goodsRepository.findByEnabledTrue());
        }
        String kw = keyword.trim().toLowerCase();
        List<Goods> result = goodsRepository.findByEnabledTrue().stream()
                .filter(g -> (g.getName() != null && g.getName().toLowerCase().contains(kw))
                        || (g.getCode() != null && g.getCode().toLowerCase().contains(kw))
                        || (g.getCategory() != null && g.getCategory().toLowerCase().contains(kw))
                        || (g.getSpec() != null && g.getSpec().toLowerCase().contains(kw)))
                .toList();
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<Object> createGoods(@RequestBody Goods goods) {
        try {
            goods.setId(null);
            goods.setCode(generateCode("GD"));
            goodsRepository.save(goods);
            return ApiResponse.success("货物创建成功");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Object> updateGoods(@PathVariable Long id, @RequestBody Goods goods) {
        try {
            Goods existing = goodsRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("货物不存在"));
            existing.setName(goods.getName());
            existing.setSpec(goods.getSpec());
            existing.setUnit(goods.getUnit());
            existing.setCategory(goods.getCategory());
            existing.setWarningQuantity(goods.getWarningQuantity());
            existing.setDefaultShelfLife(goods.getDefaultShelfLife());
            existing.setStorageRule(goods.getStorageRule());
            existing.setDefaultSupplier(goods.getDefaultSupplier());
            goodsRepository.save(existing);
            return ApiResponse.success("货物更新成功");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> deleteGoods(@PathVariable Long id) {
        try {
            Goods goods = goodsRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("货物不存在"));

            List<?> instances = goodsInstanceRepository.findByGoodsId(id);
            if (instances != null && !instances.isEmpty()) {
                return ApiResponse.error(400, "该商品下仍有 " + instances.size() + " 条库存记录，请先清空库存后再删除");
            }
            goods.setEnabled(false);
            goodsRepository.save(goods);
            return ApiResponse.success("货物删除成功");
        } catch (Exception e) {
            return ApiResponse.error(400, "删除失败：" + e.getMessage());
        }
    }

    private String generateCode(String prefix) {
        return prefix + DT_FMT.format(LocalDateTime.now()) + String.format("%04d", GOODS_SEQ.getAndIncrement() % 9999 + 1);
    }
}