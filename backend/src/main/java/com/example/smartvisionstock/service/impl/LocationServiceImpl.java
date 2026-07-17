package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.entity.GoodsInstance;
import com.example.smartvisionstock.entity.StorageLocation;
import com.example.smartvisionstock.repository.GoodsInstanceRepository;
import com.example.smartvisionstock.repository.StorageLocationRepository;
import com.example.smartvisionstock.service.AbcClassificationService;
import com.example.smartvisionstock.service.GoodsCacheService;
import com.example.smartvisionstock.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 库位管理服务 —— 基于数据库 StorageLocation 表
 * ABC 分类基于 GoodsInstance 库存数据按帕累托原则动态计算。
 */
@Service
public class LocationServiceImpl implements LocationService {

    @Autowired
    private StorageLocationRepository storageLocationRepository;

    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;

    @Autowired
    private GoodsCacheService goodsCacheService;

    @Autowired
    private AbcClassificationService abcClassificationService;

    /**
     * 判定库位是否属于"热区"：area=A 且离出入口近
     */
    private boolean isHotZone(StorageLocation loc) {
        // 简化规则：A区 且 yCoord < 100 视为热区
        return "A".equalsIgnoreCase(loc.getArea()) && loc.getYCoord() != null && loc.getYCoord() < 100;
    }

    /**
     * 计算库位到出入口的近似距离
     */
    private double calcDistance(StorageLocation loc) {
        double x = loc.getXCoord() != null ? loc.getXCoord() : 0;
        double y = loc.getYCoord() != null ? loc.getYCoord() : 0;
        double z = loc.getZCoord() != null ? loc.getZCoord() : 0;
        return Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * 根据 goodsId 获取 ABC 分类（分类结果由 AbcClassificationService 统一缓存）。
     */
    private String getABCClass(Long goodsId) {
        if (goodsId == null) return "C";
        return abcClassificationService.computeClassification().getOrDefault(goodsId, "C");
    }

    @Override
    @CacheEvict(cacheNames = "abcClassification", allEntries = true)
    public Map<String, Object> allocateLocation(String sku, int quantity) {
        // 通过 SKU 查找商品 ID，再确定 ABC 分类
        Long goodsId = goodsCacheService.findByCode(sku).map(g -> g.getId()).orElse(null);
        String abcClass = getABCClass(goodsId);
        List<StorageLocation> emptyLocs = storageLocationRepository.findByStatus(0);
        if (emptyLocs.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "无可用库位");
            return result;
        }

        // 按 ABC 规则排序，选择最优库位
        StorageLocation best = emptyLocs.stream()
                .sorted((a, b) -> {
                    double scoreA = locationScore(a, abcClass);
                    double scoreB = locationScore(b, abcClass);
                    return Double.compare(scoreB, scoreA); // 高分在前
                })
                .findFirst()
                .orElse(null);

        if (best == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "无可用库位");
            return result;
        }

        best.setStatus(1); // 占用
        storageLocationRepository.save(best);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("locationCode", best.getLocationCode());
        result.put("abcClass", abcClass);
        result.put("message", "库位分配成功，商品" + sku + "已分配到" + best.getLocationCode());
        return result;
    }

    private double locationScore(StorageLocation loc, String abcClass) {
        double distance = calcDistance(loc);
        boolean hot = isHotZone(loc);
        if ("A".equals(abcClass)) {
            return hot ? 100.0 - distance : 0;
        } else if ("B".equals(abcClass)) {
            return "A".equalsIgnoreCase(loc.getArea()) || "B".equalsIgnoreCase(loc.getArea()) ? 80.0 - distance : 30.0 - distance;
        } else {
            return 40.0 - distance;
        }
    }

    @Override
    public List<Map<String, Object>> suggestLocations(String sku) {
        Long goodsId = goodsCacheService.findByCode(sku).map(g -> g.getId()).orElse(null);
        String abcClass = getABCClass(goodsId);
        List<StorageLocation> emptyLocs = storageLocationRepository.findByStatus(0);

        return emptyLocs.stream()
                .sorted((a, b) -> Double.compare(locationScore(b, abcClass), locationScore(a, abcClass)))
                .limit(5)
                .map(loc -> {
                    Map<String, Object> m = locationToMap(loc);
                    m.put("abcClass", abcClass);
                    m.put("priority", (int) locationScore(loc, abcClass));
                    m.put("reason", getAllocationReason(abcClass, loc));
                    return m;
                })
                .collect(Collectors.toList());
    }

    private String getAllocationReason(String abcClass, StorageLocation loc) {
        if ("A".equals(abcClass) && isHotZone(loc)) {
            return "A级商品，分配到热区，便于快速拣货";
        } else if ("B".equals(abcClass) && ("A".equalsIgnoreCase(loc.getArea()) || "B".equalsIgnoreCase(loc.getArea()))) {
            return "B级商品，分配到次热区";
        } else {
            return "C级商品，分配到普通区域";
        }
    }

    @Override
    public Map<String, Object> getHotZoneAnalysis() {
        List<StorageLocation> all = storageLocationRepository.findAll();
        List<StorageLocation> hotLocs = all.stream().filter(this::isHotZone).collect(Collectors.toList());
        long occupied = hotLocs.stream().filter(l -> l.getStatus() != 0).count();

        Map<String, Object> result = new HashMap<>();
        result.put("totalHotLocations", hotLocs.size());
        result.put("occupiedHotLocations", (int) occupied);
        result.put("utilizationRate", hotLocs.isEmpty() ? 0 : (int) (occupied * 100 / hotLocs.size()));
        result.put("recommendation", occupied > hotLocs.size() * 0.8 ? "热区利用率过高，建议优化布局" : "热区利用率正常");
        return result;
    }

    @Override
    public Map<String, Object> classifyProductABC(String sku) {
        Long goodsId = goodsCacheService.findByCode(sku).map(g -> g.getId()).orElse(null);
        String abcClass = getABCClass(goodsId);
        Map<String, Object> result = new HashMap<>();
        result.put("sku", sku);
        result.put("abcClass", abcClass);
        result.put("description", getABCDescription(abcClass));
        result.put("salesFrequency", abcClass.equals("A") ? "高" : abcClass.equals("B") ? "中" : "低");
        result.put("recommendedZone", abcClass.equals("A") ? "A区(热区)" : abcClass.equals("B") ? "B区(次热区)" : "C/D区");
        return result;
    }

    private String getABCDescription(String abcClass) {
        switch (abcClass) {
            case "A": return "高频商品，销售额占比70%，应放置在热区";
            case "B": return "中频商品，销售额占比20%，放置在次热区";
            case "C": return "低频商品，销售额占比10%，放置在普通区域";
            default: return "未分类商品";
        }
    }

    @Override
    public List<Map<String, Object>> getABCDistribution() {
        List<StorageLocation> occupied = storageLocationRepository.findByStatus(1);
        Map<String, Long> dist = new HashMap<>();
        dist.put("A", 0L);
        dist.put("B", 0L);
        dist.put("C", 0L);

        // 根据库位关联的 GoodsInstance 动态确定 ABC 分类
        Map<Long, String> abcMap = abcClassificationService.computeClassification();
        for (StorageLocation loc : occupied) {
            String abcClass = "C";
            if (loc.getCurrentGoodsInstanceId() != null) {
                GoodsInstance gi = goodsInstanceRepository.findById(loc.getCurrentGoodsInstanceId()).orElse(null);
                if (gi != null && gi.getGoodsId() != null) {
                    abcClass = abcMap.getOrDefault(gi.getGoodsId(), "C");
                }
            }
            dist.merge(abcClass, 1L, Long::sum);
        }

        long total = dist.values().stream().mapToLong(Long::longValue).sum();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : dist.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("class", entry.getKey());
            item.put("count", entry.getValue());
            item.put("percentage", total > 0 ? (int) (entry.getValue() * 100 / total) : 0);
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> optimizeLocationLayout() {
        List<StorageLocation> occupied = storageLocationRepository.findByStatus(1);
        List<Map<String, Object>> recommendations = new ArrayList<>();
        Map<Long, String> abcMap = abcClassificationService.computeClassification();

        for (StorageLocation loc : occupied) {
            if (loc.getCurrentGoodsInstanceId() == null) continue;
            GoodsInstance gi = goodsInstanceRepository.findById(loc.getCurrentGoodsInstanceId()).orElse(null);
            if (gi == null || gi.getGoodsId() == null) continue;
            
            String abcClass = abcMap.getOrDefault(gi.getGoodsId(), "C");
            boolean hot = isHotZone(loc);
            String area = loc.getArea();
            // A级商品占用非热区 → 建议移至热区；C级商品占用热区 → 建议移出
            if ("A".equals(abcClass) && !hot) {
                recommendations.add(Map.of(
                    "locationCode", loc.getLocationCode(),
                    "currentLocation", loc.getLocationCode(),
                    "recommendation", "建议移至热区",
                    "reason", "A级商品("+abcClass+")位于非热区，拣货效率低"
                ));
            } else if (!"A".equals(abcClass) && hot) {
                recommendations.add(Map.of(
                    "locationCode", loc.getLocationCode(),
                    "currentLocation", loc.getLocationCode(),
                    "recommendation", "建议移至非热区",
                    "reason", abcClass+"级商品占用热区库位，建议让位给A级商品"
                ));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("recommendations", recommendations);
        result.put("optimizationScore", recommendations.isEmpty() ? 100 : 60);
        result.put("message", recommendations.isEmpty() ? "布局已最优" : "发现" + recommendations.size() + "处优化建议");
        return result;
    }

    @Override
    public Map<String, Object> getLocationUtilization(String zone) {
        List<StorageLocation> inZone = storageLocationRepository.findByArea(zone);
        long occupied = inZone.stream().filter(l -> l.getStatus() != 0).count();

        Map<String, Object> result = new HashMap<>();
        result.put("zone", zone);
        result.put("totalLocations", inZone.size());
        result.put("occupiedLocations", (int) occupied);
        result.put("emptyLocations", inZone.size() - (int) occupied);
        result.put("utilizationRate", inZone.isEmpty() ? 0 : (int) (occupied * 100 / inZone.size()));
        return result;
    }

    @Override
    public List<Map<String, Object>> getEmptyLocations() {
        return storageLocationRepository.findByStatus(0).stream()
                .map(this::locationToMap)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = "abcClassification", allEntries = true)
    public Map<String, Object> releaseLocation(String locationCode) {
        StorageLocation loc = storageLocationRepository.findByLocationCode(locationCode).orElse(null);
        if (loc == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "库位不存在");
            return result;
        }

        loc.setStatus(0);
        loc.setCurrentGoodsInstanceId(null);
        storageLocationRepository.save(loc);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "库位已释放");
        return result;
    }

    // ---- 工具方法 ----

    private Map<String, Object> locationToMap(StorageLocation loc) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", loc.getId());
        m.put("locationCode", loc.getLocationCode());
        m.put("zone", loc.getArea());
        m.put("area", loc.getArea());
        m.put("status", "EMPTY".equals(getStatusString(loc.getStatus())) ? "EMPTY" : "OCCUPIED");
        m.put("statusCode", loc.getStatus());
        m.put("isHotZone", isHotZone(loc));
        m.put("distance", calcDistance(loc));
        m.put("attribute", loc.getAttribute());
        m.put("description", loc.getDescription());
        m.put("barcode", loc.getBarcode());
        m.put("currentGoodsInstanceId", loc.getCurrentGoodsInstanceId());
        return m;
    }

    private String getStatusString(int status) {
        switch (status) {
            case 0: return "EMPTY";
            case 1: return "OCCUPIED";
            default: return "UNKNOWN";
        }
    }
}
