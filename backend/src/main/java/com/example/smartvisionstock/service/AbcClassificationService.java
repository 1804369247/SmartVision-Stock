package com.example.smartvisionstock.service;

import com.example.smartvisionstock.repository.GoodsInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ABC 分类计算服务。
 * 基于 GoodsInstance 库存总量按帕累托原则（前70%为A、中间20%为B、后10%为C）动态计算。
 * 结果通过 @Cacheable 缓存（默认内存 / redis profile 下为 Redis），避免每次请求都聚合全表。
 */
@Service
public class AbcClassificationService {

    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;

    /**
     * 计算所有商品的 ABC 分类。结果缓存 5 分钟（由 CacheManager 的 TTL 控制）。
     * 当库存布局变化（上架/释放库位）时由调用方通过 @CacheEvict 主动失效。
     */
    @Cacheable(cacheNames = "abcClassification", key = "'all'")
    public Map<Long, String> computeClassification() {
        List<Object[]> stockSummary = goodsInstanceRepository.sumQuantityByGoodsId();
        Map<Long, String> classification = new HashMap<>();
        if (stockSummary == null || stockSummary.isEmpty()) {
            return classification;
        }

        // 按库存量降序排列
        List<Object[]> sorted = new ArrayList<>(stockSummary);
        sorted.sort((a, b) -> Long.compare(
                ((Number) b[1]).longValue(), ((Number) a[1]).longValue()));

        // 计算总库存量
        long totalQty = sorted.stream().mapToLong(row -> ((Number) row[1]).longValue()).sum();
        if (totalQty == 0) {
            return classification;
        }

        // 按帕累托原则分配 ABC 类别
        long cumulative = 0;
        for (Object[] row : sorted) {
            Long goodsId = ((Number) row[0]).longValue();
            cumulative += ((Number) row[1]).longValue();
            double pct = (double) cumulative / totalQty;
            if (pct <= 0.70) {
                classification.put(goodsId, "A");
            } else if (pct <= 0.90) {
                classification.put(goodsId, "B");
            } else {
                classification.put(goodsId, "C");
            }
        }
        return classification;
    }
}
