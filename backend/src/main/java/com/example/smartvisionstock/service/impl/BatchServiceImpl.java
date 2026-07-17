package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.entity.Goods;
import com.example.smartvisionstock.entity.GoodsInstance;
import com.example.smartvisionstock.repository.GoodsInstanceRepository;
import com.example.smartvisionstock.repository.GoodsRepository;
import com.example.smartvisionstock.service.BatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 批次管理 ServiceImpl（已从内存存储迁移到数据库 GoodsInstance 表）
 *
 * 业务对接说明：
 * - BatchService 的"批次"概念对应数据库中的 GoodsInstance（一条入库记录 = 一个批次）
 * - FIFO/LIFO/FEFO 策略基于 GoodsInstance 的 inTime / expiryDate 字段排序
 * - createBatch 实际上是创建一条 GoodsInstance 记录（入库）
 */
@Service
public class BatchServiceImpl implements BatchService {

    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;

    @Autowired
    private GoodsRepository goodsRepository;

    @Override
    @Transactional
    public Map<String, Object> createBatch(String sku, String batchNo, int quantity, String expiryDate) {
        // 根据 sku（goods.code）查找商品
        Goods goods = goodsRepository.findByCode(sku).orElse(null);
        if (goods == null) {
            return errorResult("商品不存在，sku=" + sku);
        }

        GoodsInstance instance = new GoodsInstance();
        instance.setGoodsId(goods.getId());
        instance.setBatchNo(batchNo);
        instance.setQuantity(quantity);
        instance.setInTime(LocalDateTime.now());
        instance.setFrozen(false);
        instance.setOperator("system");
        if (expiryDate != null && !expiryDate.isEmpty()) {
            try {
                instance.setExpiryDate(LocalDateTime.parse(expiryDate + "T00:00:00"));
            } catch (Exception e) {
                try {
                    instance.setExpiryDate(LocalDateTime.parse(expiryDate));
                } catch (Exception e2) {
                    // 日期格式不正确，记录警告但继续创建批次
                    org.slf4j.LoggerFactory.getLogger(BatchServiceImpl.class)
                            .warn("无效的效期日期格式: {}, 使用默认值", expiryDate);
                }
            }
        }
        GoodsInstance saved = goodsInstanceRepository.save(instance);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("batchId", saved.getId());
        result.put("message", "批次创建成功");
        return result;
    }

    @Override
    public Map<String, Object> getBatch(String batchId) {
        try {
            Long id = Long.parseLong(batchId);
            Optional<GoodsInstance> opt = goodsInstanceRepository.findById(id);
            if (opt.isEmpty()) return errorResult("批次不存在");
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", toMap(opt.get()));
            return result;
        } catch (NumberFormatException e) {
            return errorResult("批次ID格式错误");
        }
    }

    @Override
    public List<Map<String, Object>> getBatchesBySku(String sku) {
        Goods goods = goodsRepository.findByCode(sku).orElse(null);
        if (goods == null) return Collections.emptyList();
        return goodsInstanceRepository.findByGoodsId(goods.getId()).stream()
                .filter(gi -> !gi.getFrozen() && gi.getQuantity() > 0)
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> selectBatchByFIFO(String sku, int quantity) {
        Goods goods = goodsRepository.findByCode(sku).orElse(null);
        if (goods == null) return errorResult("商品不存在");
        List<GoodsInstance> list = goodsInstanceRepository.findByGoodsIdAndFrozenFalseOrderByInTimeAsc(goods.getId());
        for (GoodsInstance gi : list) {
            if (gi.getQuantity() >= quantity) {
                Map<String, Object> result = new HashMap<>();
                result.put("code", 200);
                result.put("selectedBatch", toMap(gi));
                result.put("strategy", "FIFO(先进先出)");
                result.put("message", "根据FIFO策略选择批次" + gi.getBatchNo());
                return result;
            }
        }
        return errorResult("无足够库存的批次");
    }

    @Override
    public Map<String, Object> selectBatchByLIFO(String sku, int quantity) {
        Goods goods = goodsRepository.findByCode(sku).orElse(null);
        if (goods == null) return errorResult("商品不存在");
        List<GoodsInstance> list = goodsInstanceRepository.findByGoodsIdAndFrozenFalseOrderByInTimeDesc(goods.getId());
        for (GoodsInstance gi : list) {
            if (gi.getQuantity() >= quantity) {
                Map<String, Object> result = new HashMap<>();
                result.put("code", 200);
                result.put("selectedBatch", toMap(gi));
                result.put("strategy", "LIFO(后进先出)");
                result.put("message", "根据LIFO策略选择批次" + gi.getBatchNo());
                return result;
            }
        }
        return errorResult("无足够库存的批次");
    }

    @Override
    public Map<String, Object> selectBatchByFEFO(String sku, int quantity) {
        Goods goods = goodsRepository.findByCode(sku).orElse(null);
        if (goods == null) return errorResult("商品不存在");
        List<GoodsInstance> list = goodsInstanceRepository.findByGoodsIdAndFrozenFalseOrderByExpiryDateAsc(goods.getId());
        for (GoodsInstance gi : list) {
            if (gi.getQuantity() >= quantity) {
                Map<String, Object> result = new HashMap<>();
                result.put("code", 200);
                result.put("selectedBatch", toMap(gi));
                result.put("strategy", "FEFO(先到期先出)");
                result.put("message", "根据FEFO策略选择批次" + gi.getBatchNo()
                        + "，效期：" + (gi.getExpiryDate() != null ? gi.getExpiryDate().toLocalDate() : "无"));
                return result;
            }
        }
        return errorResult("无足够库存的批次");
    }

    @Override
    public List<Map<String, Object>> getBatchHistory(String sku) {
        Goods goods = goodsRepository.findByCode(sku).orElse(null);
        if (goods == null) return Collections.emptyList();
        return goodsInstanceRepository.findByGoodsId(goods.getId()).stream()
                .sorted(Comparator.comparing(GoodsInstance::getInTime).reversed())
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> updateBatchQuantity(String batchId, int quantity) {
        try {
            Long id = Long.parseLong(batchId);
            GoodsInstance gi = goodsInstanceRepository.findById(id).orElse(null);
            if (gi == null) return errorResult("批次不存在");
            if (gi.getQuantity() < quantity) return errorResult("库存不足");
            gi.setQuantity(gi.getQuantity() - quantity);
            if (gi.getQuantity() == 0) {
                gi.setOutTime(LocalDateTime.now());
            }
            goodsInstanceRepository.save(gi);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("remainingQuantity", gi.getQuantity());
            result.put("message", "批次数量已更新");
            return result;
        } catch (NumberFormatException e) {
            return errorResult("批次ID格式错误");
        }
    }

    @Override
    public Map<String, Object> getBatchTraceability(String batchId) {
        try {
            Long id = Long.parseLong(batchId);
            GoodsInstance gi = goodsInstanceRepository.findById(id).orElse(null);
            if (gi == null) return errorResult("批次不存在");
            
            Map<String, Object> traceability = new HashMap<>();
            traceability.put("batchId", gi.getId());
            traceability.put("batchNo", gi.getBatchNo());
            traceability.put("goodsId", gi.getGoodsId());
            traceability.put("quantity", gi.getQuantity());
            traceability.put("expiryDate", gi.getExpiryDate());
            traceability.put("locationId", gi.getLocationId());
            traceability.put("inTime", gi.getInTime());
            traceability.put("outTime", gi.getOutTime());
            traceability.put("operator", gi.getOperator());
            traceability.put("frozen", gi.getFrozen());
            traceability.put("frozenReason", gi.getFrozenReason());

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", traceability);
            return result;
        } catch (NumberFormatException e) {
            return errorResult("批次ID格式错误");
        }
    }

    @Override
    public List<Map<String, Object>> getExpiringBatches(int daysThreshold) {
        LocalDateTime threshold = LocalDateTime.now().plusDays(daysThreshold);
        return goodsInstanceRepository.findExpiringInstances(threshold).stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getBatchStatistics() {
        long totalBatches = goodsInstanceRepository.count();
        long active = goodsInstanceRepository.countActiveBatches();
        long depleted = goodsInstanceRepository.countDepletedBatches();
        long frozen = goodsInstanceRepository.countFrozenBatches();
        long totalQuantity = goodsInstanceRepository.sumTotalQuantity();

        Map<String, Object> result = new HashMap<>();
        result.put("totalBatches", totalBatches);
        result.put("activeBatches", active);
        result.put("depletedBatches", depleted);
        result.put("frozenBatches", frozen);
        result.put("totalQuantity", totalQuantity);
        return result;
    }

    // ---- 内部工具方法 ----

    private Map<String, Object> toMap(GoodsInstance gi) {
        Map<String, Object> m = new HashMap<>();
        m.put("batchId", gi.getId());
        m.put("batchNo", gi.getBatchNo());
        m.put("goodsId", gi.getGoodsId());
        m.put("quantity", gi.getQuantity());
        m.put("expiryDate", gi.getExpiryDate());
        m.put("locationId", gi.getLocationId());
        m.put("inTime", gi.getInTime());
        m.put("outTime", gi.getOutTime());
        m.put("operator", gi.getOperator());
        m.put("frozen", gi.getFrozen());
        m.put("frozenReason", gi.getFrozenReason());
        return m;
    }

    private Map<String, Object> errorResult(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);
        result.put("message", message);
        return result;
    }
}
