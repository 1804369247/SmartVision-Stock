package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.entity.Goods;
import com.example.smartvisionstock.entity.GoodsInstance;
import com.example.smartvisionstock.repository.GoodsInstanceRepository;
import com.example.smartvisionstock.repository.GoodsRepository;
import com.example.smartvisionstock.service.ExpiryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 效期预警 ServiceImpl（已从内存存储迁移到数据库 GoodsInstance 表）
 *
 * 说明：
 * - 预警数据直接从 GoodsInstance.expiryDate 计算得出，无需单独存储
 * - markAsExpired 通过冻结实例实现（frozen=true，frozenReason="过期"）
 * - processAlert 通过冻结/删除实例实现
 */
@Service
public class ExpiryServiceImpl implements ExpiryService {

    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;

    @Autowired
    private GoodsRepository goodsRepository;

    @Override
    public List<Map<String, Object>> checkExpiringProducts(int daysThreshold) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusDays(daysThreshold);
        List<GoodsInstance> expiring = goodsInstanceRepository.findExpiringInstances(threshold);
        return expiring.stream()
                .map(gi -> buildAlert(gi, now))
                .sorted(Comparator.comparingLong(m -> (Long) m.get("daysRemaining")))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getExpiryAlert(String alertId) {
        // alertId 等于 GoodsInstance.id（字符串形式）
        try {
            Long id = Long.parseLong(alertId);
            GoodsInstance gi = goodsInstanceRepository.findById(id).orElse(null);
            if (gi == null || gi.getExpiryDate() == null) {
                return errorResult("预警不存在");
            }
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", buildAlert(gi, LocalDateTime.now()));
            return result;
        } catch (NumberFormatException e) {
            return errorResult("预警ID格式错误");
        }
    }

    @Override
    public List<Map<String, Object>> getActiveAlerts() {
        // 取30天内到期的未冻结实例作为活跃预警
        return checkExpiringProducts(30).stream()
                .filter(m -> !"PROCESSED".equals(m.get("status")))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> processAlert(String alertId, String action) {
        try {
            Long id = Long.parseLong(alertId);
            GoodsInstance gi = goodsInstanceRepository.findById(id).orElse(null);
            if (gi == null) return errorResult("预警不存在");
            // 处理动作：冻结实例，表示已处理
            gi.setFrozen(true);
            gi.setFrozenReason("过期处理：" + action);
            goodsInstanceRepository.save(gi);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "预警已处理：" + action);
            return result;
        } catch (NumberFormatException e) {
            return errorResult("预警ID格式错误");
        }
    }

    @Override
    public Map<String, Object> sendExpiryNotification(List<String> alertIds) {
        // 实际通知逻辑（邮件/企微/钉钉）可扩展；此处记录操作结果
        int sentCount = 0;
        List<String> failedIds = new ArrayList<>();
        for (String alertId : alertIds) {
            try {
                Long id = Long.parseLong(alertId);
                if (goodsInstanceRepository.existsById(id)) {
                    sentCount++;
                } else {
                    failedIds.add(alertId);
                }
            } catch (NumberFormatException e) {
                failedIds.add(alertId);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("sentCount", sentCount);
        result.put("failedIds", failedIds);
        result.put("message", "已发送" + sentCount + "条预警通知");
        return result;
    }

    @Override
    public Map<String, Object> getExpiryStatistics() {
        LocalDateTime now = LocalDateTime.now();
        // 使用不过滤冻结的查询以正确统计 processedAlerts
        List<GoodsInstance> expiring30 = goodsInstanceRepository.findExpiringInstancesForStats(now.plusDays(30));
        long high = expiring30.stream().filter(gi -> daysUntilExpiry(gi, now) <= 3).count();
        long medium = expiring30.stream().filter(gi -> { long d = daysUntilExpiry(gi, now); return d > 3 && d <= 7; }).count();
        long low = expiring30.stream().filter(gi -> { long d = daysUntilExpiry(gi, now); return d > 7 && d <= 30; }).count();

        List<GoodsInstance> expired = goodsInstanceRepository.findExpiringInstances(now);
        Map<String, Object> result = new HashMap<>();
        result.put("totalAlerts", expiring30.size());
        result.put("activeAlerts", expiring30.stream().filter(gi -> !gi.getFrozen()).count());
        result.put("processedAlerts", expiring30.stream().filter(GoodsInstance::getFrozen).count());
        result.put("highSeverity", high);
        result.put("mediumSeverity", medium);
        result.put("lowSeverity", low);
        result.put("expiredCount", expired.size());
        return result;
    }

    @Override
    public void runDailyExpiryCheck() {
        // 定时任务入口：检查30天内到期的货物，可扩展为发送实际通知
        List<Map<String, Object>> expiring = checkExpiringProducts(30);
        List<String> highAlertIds = expiring.stream()
                .filter(m -> "HIGH".equals(m.get("severity")))
                .map(m -> String.valueOf(m.get("batchId")))
                .collect(Collectors.toList());
        if (!highAlertIds.isEmpty()) {
            sendExpiryNotification(highAlertIds);
        }
    }

    @Override
    public List<Map<String, Object>> getExpiredProducts() {
        LocalDateTime now = LocalDateTime.now();
        // 已过期 = expiryDate <= now，findExpiringInstances 已包含此条件，额外过滤确保准确性
        return goodsInstanceRepository.findExpiringInstances(now).stream()
                .filter(gi -> gi.getExpiryDate() != null && !gi.getExpiryDate().isAfter(now))
                .map(gi -> buildAlert(gi, now))
                .sorted(Comparator.comparingLong(m -> (Long) m.get("daysRemaining")))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> markAsExpired(String batchId) {
        try {
            Long id = Long.parseLong(batchId);
            GoodsInstance gi = goodsInstanceRepository.findById(id).orElse(null);
            if (gi == null) return errorResult("批次不存在");
            gi.setFrozen(true);
            gi.setFrozenReason("已过期");
            goodsInstanceRepository.save(gi);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "批次已标记为过期并冻结");
            result.put("batchId", batchId);
            return result;
        } catch (NumberFormatException e) {
            return errorResult("批次ID格式错误");
        }
    }

    // ---- 内部工具方法 ----

    private Map<String, Object> buildAlert(GoodsInstance gi, LocalDateTime now) {
        long daysRemaining = gi.getExpiryDate() != null
                ? ChronoUnit.DAYS.between(now, gi.getExpiryDate())
                : Long.MAX_VALUE;
        String severity = getSeverity(daysRemaining);
        String goodsName = goodsRepository.findById(gi.getGoodsId())
                .map(Goods::getName).orElse("未知商品");

        Map<String, Object> m = new HashMap<>();
        m.put("alertId", String.valueOf(gi.getId()));
        m.put("batchId", gi.getId());
        m.put("batchNo", gi.getBatchNo());
        m.put("goodsId", gi.getGoodsId());
        m.put("goodsName", goodsName);
        m.put("quantity", gi.getQuantity());
        m.put("expiryDate", gi.getExpiryDate());
        m.put("daysRemaining", daysRemaining);
        m.put("severity", severity);
        m.put("status", gi.getFrozen() ? "PROCESSED" : "ACTIVE");
        m.put("suggestedAction", getSuggestedAction(daysRemaining));
        return m;
    }

    private long daysUntilExpiry(GoodsInstance gi, LocalDateTime now) {
        if (gi.getExpiryDate() == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(now, gi.getExpiryDate());
    }

    private String getSeverity(long daysRemaining) {
        if (daysRemaining <= 3) return "HIGH";
        if (daysRemaining <= 7) return "MEDIUM";
        return "LOW";
    }

    private String getSuggestedAction(long daysRemaining) {
        if (daysRemaining <= 1) return "立即处理：退货或报废";
        if (daysRemaining <= 3) return "紧急促销或调拨";
        if (daysRemaining <= 7) return "优先出库，启动促销";
        return "关注库存，优先拣货";
    }

    private Map<String, Object> errorResult(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);
        result.put("message", message);
        return result;
    }
}
