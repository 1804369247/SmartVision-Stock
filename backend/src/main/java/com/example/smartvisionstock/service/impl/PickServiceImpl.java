package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.entity.*;
import com.example.smartvisionstock.repository.*;
import com.example.smartvisionstock.service.PickService;
import com.example.smartvisionstock.util.UserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 波次拣货服务 —— 基于数据库持久化，WavePick + PickTask 实体。
 * 拣货数据基于真实出库单和库存数据生成。
 */
@Service
public class PickServiceImpl implements PickService {

    private static final Logger log = LoggerFactory.getLogger(PickServiceImpl.class);

    @Autowired
    private OutboundOrderRepository outboundOrderRepository;

    @Autowired
    private OutboundOrderItemRepository outboundOrderItemRepository;

    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;

    @Autowired
    private WavePickRepository wavePickRepository;

    @Autowired
    private PickTaskRepository pickTaskRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final AtomicLong WAVE_SEQ = new AtomicLong(0);

    @Override
    @Transactional
    public Map<String, Object> createWavePick(List<Long> orderIds) {
        // 查询真实的出库单
        List<OutboundOrder> orders = outboundOrderRepository.findAllById(orderIds);
        if (orders.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "未找到有效的出库单");
            return result;
        }

        // 只允许审核中或拣货中的订单
        List<OutboundOrder> validOrders = orders.stream()
                .filter(o -> "PICKING".equals(o.getStatus()) || "AUDITING".equals(o.getStatus()))
                .collect(Collectors.toList());

        if (validOrders.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "没有可拣货的出库单（状态必须为AUDITING或PICKING）");
            return result;
        }

        String waveId = "WAVE-" + System.currentTimeMillis() + "-" + WAVE_SEQ.incrementAndGet();

        // 创建波次实体
        WavePick wave = new WavePick();
        wave.setWaveId(waveId);
        wave.setStatus("CREATED");
        wave.setCreateTime(LocalDateTime.now());
        wave.setAssignedTo(null);

        // 保存关联的出库单ID
        List<Long> validOrderIds = validOrders.stream().map(OutboundOrder::getId).collect(Collectors.toList());
        try {
            wave.setOrderIdsJson(MAPPER.writeValueAsString(validOrderIds));
        } catch (JsonProcessingException e) {
            wave.setOrderIdsJson(validOrderIds.toString());
        }

        // 基于数据库真实数据生成拣货任务并持久化
        List<PickTask> tasks = generateAndSavePickTasks(waveId, validOrders);
        int totalQty = tasks.stream().mapToInt(t -> t.getQuantity() != null ? t.getQuantity() : 0).sum();
        int totalItems = tasks.stream().mapToInt(t -> {
            try {
                List<?> items = MAPPER.readValue(t.getItemsJson(), List.class);
                return items.size();
            } catch (Exception ex) { 
                log.warn("解析拣货任务商品明细失败: taskId={}, error={}", t.getId(), ex.getMessage());
                return 0; 
            }
        }).sum();

        wave.setTotalQuantity(totalQty);
        wave.setTotalItems(totalItems);
        wavePickRepository.save(wave);

        // 更新出库单状态为拣货中
        validOrders.forEach(o -> {
            o.setStatus("PICKING");
            o.setPickTime(LocalDateTime.now());
            outboundOrderRepository.save(o);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("waveId", waveId);
        result.put("taskCount", tasks.size());
        result.put("totalQuantity", totalQty);
        result.put("message", "波次拣货任务创建成功，共" + tasks.size() + "个任务、" + totalQty + "件商品");
        return result;
    }

    /**
     * 基于真实出库单数据生成拣货任务并持久化到数据库
     */
    private List<PickTask> generateAndSavePickTasks(String waveId, List<OutboundOrder> orders) {
        List<PickTask> savedTasks = new ArrayList<>();

        for (int i = 0; i < orders.size(); i++) {
            OutboundOrder order = orders.get(i);
            List<OutboundOrderItem> items = outboundOrderItemRepository.findByOrderId(order.getId());

            String taskId = waveId + "-TASK-" + (i + 1);

            PickTask task = new PickTask();
            task.setTaskId(taskId);
            task.setWaveId(waveId);
            task.setOrderId(order.getId());
            task.setOrderNo(order.getOrderNo());
            task.setStatus("PENDING");
            task.setPriority(i < 3 ? "HIGH" : "NORMAL");
            task.setCreateTime(LocalDateTime.now());
            task.setWarehouseId(order.getWarehouseId());

            // 构建拣货明细和库位信息
            List<Map<String, Object>> pickItems = new ArrayList<>();
            List<Map<String, Object>> locations = new ArrayList<>();
            int totalQty = 0;

            for (OutboundOrderItem item : items) {
                GoodsInstance instance = goodsInstanceRepository.findById(item.getGoodsInstanceId()).orElse(null);
                if (instance == null) continue;

                Map<String, Object> pickItem = new LinkedHashMap<>();
                pickItem.put("goodsInstanceId", instance.getId());
                pickItem.put("goodsId", instance.getGoodsId());
                pickItem.put("batchNo", instance.getBatchNo());
                pickItem.put("quantity", item.getQuantity());
                pickItem.put("picked", 0);
                pickItems.add(pickItem);
                totalQty += item.getQuantity();

                if (instance.getLocationId() != null) {
                    Map<String, Object> loc = new LinkedHashMap<>();
                    loc.put("goodsInstanceId", instance.getId());
                    loc.put("locationId", instance.getLocationId());
                    loc.put("batchNo", instance.getBatchNo());
                    loc.put("quantity", instance.getQuantity());
                    locations.add(loc);
                }
            }

            try {
                task.setItemsJson(MAPPER.writeValueAsString(pickItems));
                task.setLocationsJson(MAPPER.writeValueAsString(locations));
            } catch (JsonProcessingException e) {
                task.setItemsJson(pickItems.toString());
                task.setLocationsJson(locations.toString());
            }

            task.setQuantity(totalQty);
            savedTasks.add(pickTaskRepository.save(task));
        }

        return savedTasks;
    }

    @Override
    public Map<String, Object> getWavePick(String waveId) {
        Optional<WavePick> optional = wavePickRepository.findByWaveId(waveId);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "波次不存在");
            return result;
        }

        WavePick wave = optional.get();
        Map<String, Object> data = waveToMap(wave);

        // 附加任务列表
        List<PickTask> tasks = pickTaskRepository.findByWaveId(waveId);
        data.put("tasks", tasks.stream().map(this::taskToMap).collect(Collectors.toList()));

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    @Override
    public List<Map<String, Object>> getPickTasks(String waveId) {
        return pickTaskRepository.findByWaveId(waveId).stream()
                .map(this::taskToMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getPickTaskDetail(String taskId) {
        Optional<PickTask> optional = pickTaskRepository.findByTaskId(taskId);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "任务不存在");
            return result;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", taskToMap(optional.get()));
        return result;
    }

    @Override
    public Map<String, Object> completePickTask(String taskId, Map<String, Object> completionData) {
        Optional<PickTask> optional = pickTaskRepository.findByTaskId(taskId);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "任务不存在");
            return result;
        }

        PickTask task = optional.get();
        task.setStatus("COMPLETED");
        task.setCompleteTime(LocalDateTime.now());
        task.setOperator(UserContext.getCurrentUsernameOrDefault("unknown"));

        // 保存完成的拣货明细
        if (completionData != null && completionData.containsKey("pickedItems")) {
            try {
                task.setItemsJson(MAPPER.writeValueAsString(completionData.get("pickedItems")));
            } catch (JsonProcessingException ex) { 
                log.warn("序列化拣货完成明细失败: taskId={}, error={}", taskId, ex.getMessage());
            }
        }

        pickTaskRepository.save(task);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "拣货任务完成");
        result.put("taskId", taskId);
        return result;
    }

    @Override
    public Map<String, Object> optimizePickPath(String waveId) {
        Optional<WavePick> optional = wavePickRepository.findByWaveId(waveId);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "波次不存在");
            return result;
        }

        List<PickTask> tasks = pickTaskRepository.findByWaveId(waveId);
        // 按优先级排序（高优先级在前），同优先级按第一个库位ID升序
        tasks.sort((a, b) -> {
            String pa = a.getPriority() != null ? a.getPriority() : "NORMAL";
            String pb = b.getPriority() != null ? b.getPriority() : "NORMAL";
            if ("HIGH".equals(pa) && !"HIGH".equals(pb)) return -1;
            if (!"HIGH".equals(pa) && "HIGH".equals(pb)) return 1;

            List<Map<String, Object>> locsA = parseLocations(a.getLocationsJson());
            List<Map<String, Object>> locsB = parseLocations(b.getLocationsJson());
            Long idA = locsA != null && !locsA.isEmpty() ? toLong(locsA.get(0).get("locationId")) : 0L;
            Long idB = locsB != null && !locsB.isEmpty() ? toLong(locsB.get(0).get("locationId")) : 0L;
            return Long.compare(idA, idB);
        });

        List<Object> optimizedPath = new ArrayList<>();
        for (PickTask task : tasks) {
            List<Map<String, Object>> locations = parseLocations(task.getLocationsJson());
            if (locations != null) {
                for (Map<String, Object> loc : locations) {
                    optimizedPath.add(loc.get("locationId"));
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("optimizedPath", optimizedPath);
        result.put("totalStops", optimizedPath.size());
        result.put("message", "路径优化完成");
        return result;
    }

    @Override
    public List<Map<String, Object>> getActiveWaves() {
        return wavePickRepository.findByStatusIn(Arrays.asList("CREATED", "IN_PROGRESS")).stream()
                .map(this::waveToMap)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> cancelWavePick(String waveId) {
        Optional<WavePick> optional = wavePickRepository.findByWaveId(waveId);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "波次不存在");
            return result;
        }

        WavePick wave = optional.get();
        wave.setStatus("CANCELLED");
        wave.setCancelTime(LocalDateTime.now());
        wavePickRepository.save(wave);

        // 取消所有子任务
        List<PickTask> tasks = pickTaskRepository.findByWaveId(waveId);
        tasks.forEach(t -> {
            t.setStatus("CANCELLED");
            pickTaskRepository.save(t);
        });

        // 恢复出库单状态为审核中
        List<Long> orderIds = parseOrderIds(wave.getOrderIdsJson());
        if (orderIds != null) {
            orderIds.forEach(id -> {
                OutboundOrder order = outboundOrderRepository.findById(id).orElse(null);
                if (order != null && "PICKING".equals(order.getStatus())) {
                    order.setStatus("AUDITING");
                    outboundOrderRepository.save(order);
                }
            });
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "波次已取消");
        return result;
    }

    @Override
    public Map<String, Object> getPickStatistics(String waveId) {
        List<PickTask> tasks = pickTaskRepository.findByWaveId(waveId);

        int totalTasks = tasks.size();
        int completedTasks = 0;
        int pendingTasks = 0;
        int totalQuantity = 0;

        for (PickTask task : tasks) {
            if ("COMPLETED".equals(task.getStatus())) {
                completedTasks++;
            } else if ("PENDING".equals(task.getStatus())) {
                pendingTasks++;
            }
            totalQuantity += task.getQuantity() != null ? task.getQuantity() : 0;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("totalTasks", totalTasks);
        result.put("completedTasks", completedTasks);
        result.put("pendingTasks", pendingTasks);
        result.put("totalQuantity", totalQuantity);
        result.put("completionRate", totalTasks > 0 ? (completedTasks * 100 / totalTasks) : 0);
        return result;
    }

    // ===== 工具方法 =====

    private Map<String, Object> waveToMap(WavePick wave) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("waveId", wave.getWaveId());
        map.put("orderIds", parseOrderIds(wave.getOrderIdsJson()));
        map.put("status", wave.getStatus());
        map.put("createTime", wave.getCreateTime());
        map.put("assignedTo", wave.getAssignedTo());
        map.put("totalQuantity", wave.getTotalQuantity());
        map.put("totalItems", wave.getTotalItems());
        map.put("cancelTime", wave.getCancelTime());
        return map;
    }

    private Map<String, Object> taskToMap(PickTask task) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("taskId", task.getTaskId());
        map.put("waveId", task.getWaveId());
        map.put("orderId", task.getOrderId());
        map.put("orderNo", task.getOrderNo());
        map.put("status", task.getStatus());
        map.put("priority", task.getPriority());
        map.put("items", parseItemList(task.getItemsJson()));
        map.put("locations", parseLocations(task.getLocationsJson()));
        map.put("quantity", task.getQuantity());
        map.put("warehouseId", task.getWarehouseId());
        map.put("createTime", task.getCreateTime());
        map.put("completeTime", task.getCompleteTime());
        map.put("operator", task.getOperator());
        return map;
    }

    private List<Long> parseOrderIds(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return MAPPER.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> parseItemList(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseLocations(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        return 0L;
    }
}
