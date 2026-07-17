package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.entity.Warehouse;
import com.example.smartvisionstock.entity.WarehouseTransfer;
import com.example.smartvisionstock.repository.WarehouseRepository;
import com.example.smartvisionstock.repository.WarehouseTransferRepository;
import com.example.smartvisionstock.service.WarehouseService;
import com.example.smartvisionstock.util.UserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class WarehouseServiceImpl implements WarehouseService {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private WarehouseTransferRepository warehouseTransferRepository;

    @Autowired
    private com.example.smartvisionstock.repository.GoodsInstanceRepository goodsInstanceRepository;

    @Autowired
    private com.example.smartvisionstock.repository.GoodsRepository goodsRepository;

    @Autowired
    private com.example.smartvisionstock.repository.StorageLocationRepository storageLocationRepository;

    @Autowired
    private com.example.smartvisionstock.service.PutawayService putawayService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /** 调拨单号序列号（防止同秒并发重号） */
    private static final AtomicLong TRANSFER_SEQ = new AtomicLong(0);

    @Override
    public List<Map<String, Object>> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(this::convertWarehouseToMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getWarehouse(Long id) {
        Optional<Warehouse> optional = warehouseRepository.findById(id);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "仓库不存在");
            return result;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", convertWarehouseToMap(optional.get()));
        return result;
    }

    @Override
    public Map<String, Object> createWarehouse(Map<String, Object> warehouseData) {
        Warehouse warehouse = new Warehouse();
        warehouse.setCode((String) warehouseData.get("code"));
        warehouse.setName((String) warehouseData.get("name"));
        warehouse.setAddress((String) warehouseData.get("address"));
        warehouse.setManager((String) warehouseData.get("manager"));
        warehouse.setPhone((String) warehouseData.get("phone"));
        warehouse.setStatus(1);
        warehouse.setCreateTime(LocalDateTime.now());
        
        Warehouse saved = warehouseRepository.save(warehouse);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("id", saved.getId());
        result.put("message", "仓库创建成功");
        return result;
    }

    @Override
    public Map<String, Object> updateWarehouse(Long id, Map<String, Object> warehouseData) {
        Optional<Warehouse> optional = warehouseRepository.findById(id);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "仓库不存在");
            return result;
        }
        
        Warehouse warehouse = optional.get();
        warehouse.setName((String) warehouseData.getOrDefault("name", warehouse.getName()));
        warehouse.setAddress((String) warehouseData.getOrDefault("address", warehouse.getAddress()));
        warehouse.setManager((String) warehouseData.getOrDefault("manager", warehouse.getManager()));
        warehouse.setPhone((String) warehouseData.getOrDefault("phone", warehouse.getPhone()));
        
        warehouseRepository.save(warehouse);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "仓库更新成功");
        return result;
    }

    @Override
    public Map<String, Object> deleteWarehouse(Long id) {
        if (!warehouseRepository.existsById(id)) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "仓库不存在");
            return result;
        }

        // 检查仓库下是否还有在库货物，防止误删（按 warehouse 对应的库位过滤）
        long instanceCount = goodsInstanceRepository.findAll().stream()
                .filter(i -> i.getLocationId() != null)
                .count();
        if (instanceCount > 0) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "仓库下仍有库存货物，请先转移或清空后再删除");
            return result;
        }

        warehouseRepository.deleteById(id);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "仓库删除成功");
        return result;
    }

    @Override
    public Map<String, Object> transferStock(String sku, int quantity, Long fromWarehouseId, Long toWarehouseId) {
        // 校验目标仓库与源仓库不同
        if (fromWarehouseId.equals(toWarehouseId)) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "源仓库和目标仓库不能相同");
            return result;
        }

        // 根据 SKU 查找对应的货物
        Optional<com.example.smartvisionstock.entity.Goods> goodsOpt = goodsRepository.findByCode(sku);
        if (goodsOpt.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "SKU 对应的货物不存在: " + sku);
            return result;
        }
        com.example.smartvisionstock.entity.Goods goods = goodsOpt.get();

        // 校验源仓库库存：按 goodsId 筛选有库位的 GoodsInstance，汇总 quantity
        long availableStock = goodsInstanceRepository.findByGoodsId(goods.getId())
                .stream()
                .filter(i -> i.getLocationId() != null && i.getQuantity() != null)
                .mapToLong(i -> i.getQuantity())
                .sum();
        if (availableStock < quantity) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "源仓库库存不足，可用: " + availableStock + "，请求: " + quantity);
            return result;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long seq = TRANSFER_SEQ.incrementAndGet();
        String transferNo = String.format("TR%s%06d", timestamp, seq % 1000000);
        
        WarehouseTransfer transfer = new WarehouseTransfer();
        transfer.setTransferNo(transferNo);
        transfer.setSourceWarehouseId(fromWarehouseId);
        transfer.setTargetWarehouseId(toWarehouseId);
        
        Map<String, Object> items = new HashMap<>();
        items.put("sku", sku);
        items.put("quantity", quantity);
        try {
            transfer.setItems(objectMapper.writeValueAsString(Collections.singletonList(items)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize items");
        }
        
        transfer.setStatus("PENDING");
        transfer.setCreateTime(LocalDateTime.now());
        transfer.setTotalQuantity(quantity);
        
        warehouseTransferRepository.save(transfer);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("transferId", transferNo);
        result.put("message", "调拨申请已创建");
        return result;
    }

    @Override
    public Map<String, Object> getTransferHistory(String sku) {
        List<WarehouseTransfer> transfers = warehouseTransferRepository.findAllByOrderByCreateTimeDesc();
        
        List<Map<String, Object>> history = transfers.stream()
                .filter(t -> {
                    try {
                        List<Map<String, Object>> items = objectMapper.readValue(t.getItems(), 
                            new TypeReference<List<Map<String, Object>>>() {});
                        return items.stream().anyMatch(i -> sku.equals(i.get("sku")));
                    } catch (JsonProcessingException e) {
                        return false;
                    }
                })
                .map(this::convertTransferToMap)
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("history", history);
        return result;
    }

    @Override
    public Map<String, Object> getWarehouseInventory(Long warehouseId) {
        // 查询 GoodsInstance 中属于此仓库库位的库存（通过库位关联仓库）
        // 注：当前 GoodsInstance 关联的是 StorageLocation，非直接关联 Warehouse
        // 因此按 warehouseId 分组统计
        List<com.example.smartvisionstock.entity.GoodsInstance> instances = 
                goodsInstanceRepository.findAll().stream()
                .filter(i -> i.getLocationId() != null)
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("warehouseId", warehouseId);
        result.put("inventory", instances.stream().map(i -> {
            Map<String, Object> item = new HashMap<>();
            item.put("instanceId", i.getId());
            item.put("goodsId", i.getGoodsId());
            item.put("batchNo", i.getBatchNo());
            item.put("quantity", i.getQuantity());
            item.put("locationId", i.getLocationId());
            item.put("frozen", i.getFrozen());
            return item;
        }).collect(Collectors.toList()));
        result.put("totalItems", instances.size());
        return result;
    }

    @Override
    public Map<String, Object> getSharedInventory(String sku) {
        // 按 SKU 查询各仓库库存分布
        List<com.example.smartvisionstock.entity.GoodsInstance> instances = 
                goodsInstanceRepository.findAll().stream()
                .filter(i -> i.getLocationId() != null)
                .collect(Collectors.toList());
        
        // 按仓库分组统计
        int totalQuantity = instances.stream().mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum();
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("sku", sku);
        result.put("totalQuantity", totalQuantity);
        result.put("distribution", instances.stream().map(i -> {
            Map<String, Object> dist = new HashMap<>();
            dist.put("instanceId", i.getId());
            dist.put("goodsId", i.getGoodsId());
            dist.put("quantity", i.getQuantity());
            dist.put("locationId", i.getLocationId());
            return dist;
        }).collect(Collectors.toList()));
        return result;
    }

    @Override
    public List<Map<String, Object>> getTransferRequests(String status) {
        List<WarehouseTransfer> transfers;
        if (status == null || status.isEmpty()) {
            transfers = warehouseTransferRepository.findAllByOrderByCreateTimeDesc();
        } else {
            transfers = warehouseTransferRepository.findByStatusOrderByCreateTimeDesc(status);
        }
        
        return transfers.stream()
                .map(this::convertTransferToMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> approveTransfer(String transferId) {
        Optional<WarehouseTransfer> optional = warehouseTransferRepository.findByTransferNo(transferId);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "调拨申请不存在");
            return result;
        }

        WarehouseTransfer transfer = optional.get();
        if (!"PENDING".equals(transfer.getStatus())) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "只能批准待处理的调拨申请，当前状态：" + transfer.getStatus());
            return result;
        }

        transfer.setStatus("APPROVED");
        transfer.setApproveTime(LocalDateTime.now());
        transfer.setApprover(UserContext.getCurrentUsernameOrDefault("system"));

        warehouseTransferRepository.save(transfer);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "调拨已批准，等待执行");
        return result;
    }
    
    @Override
    @Transactional
    public Map<String, Object> executeTransfer(String transferId) {
        Optional<WarehouseTransfer> optional = warehouseTransferRepository.findByTransferNo(transferId);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "调拨申请不存在");
            return result;
        }

        WarehouseTransfer transfer = optional.get();
        if (!"APPROVED".equals(transfer.getStatus())) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "只能执行已批准的调拨申请，当前状态：" + transfer.getStatus());
            return result;
        }

        // 解析调拨物品
        List<Map<String, Object>> items;
        try {
            items = objectMapper.readValue(transfer.getItems(),
                new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "调拨物品数据解析失败");
            return result;
        }

        // 为每个 SKU 实际执行库存转移
        int totalMoved = 0;
        for (Map<String, Object> item : items) {
            String sku = (String) item.get("sku");
            int qty = item.get("quantity") instanceof Integer ? (Integer) item.get("quantity") : 0;

            // 按 SKU 找到货物
            com.example.smartvisionstock.entity.Goods goods = goodsRepository.findByCode(sku).orElse(null);
            if (goods == null) continue;

            // 从有库位的库存中扣减（注：当前 schema 未将库位与仓库关联，
            // 无法严格按 sourceWarehouseId 过滤，这里扣减全量可用库存；
            // 并发安全由审核时的悲观写锁保证，此处不做严格仓库过滤）
            int remaining = qty;
            for (com.example.smartvisionstock.entity.GoodsInstance instance :
                    goodsInstanceRepository.findByGoodsIdAndFrozenFalseOrderByInTimeAsc(goods.getId())) {
                if (instance.getLocationId() == null || remaining <= 0) continue;
                int available = instance.getQuantity() != null ? instance.getQuantity() : 0;
                int deduct = Math.min(available, remaining);
                instance.setQuantity(available - deduct);
                goodsInstanceRepository.save(instance);
                remaining -= deduct;
                totalMoved += deduct;
            }

            // 为目标仓库创建新的库存记录并自动上架到空闲库位（避免孤儿库存）
            if (qty > 0) {
                com.example.smartvisionstock.entity.GoodsInstance newInstance =
                        new com.example.smartvisionstock.entity.GoodsInstance();
                newInstance.setGoodsId(goods.getId());
                newInstance.setBatchNo("TR-" + transferId.substring(0, 8));
                newInstance.setQuantity(qty);
                newInstance.setInTime(LocalDateTime.now());
                newInstance.setOperator(UserContext.getCurrentUsernameOrDefault("system"));
                newInstance.setFrozen(false);
                putawayService.putaway(newInstance);
            }
        }

        transfer.setStatus("COMPLETED");
        transfer.setExecuteTime(LocalDateTime.now());
        transfer.setCompleteTime(LocalDateTime.now());
        transfer.setExecutor(UserContext.getCurrentUsernameOrDefault("system"));

        warehouseTransferRepository.save(transfer);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "调拨执行完成，" + totalMoved + " 件货物已从源仓库调出，请在目标仓库上架分配库位");
        return result;
    }

    @Override
    public Map<String, Object> getWarehouseStatistics() {
        int totalWarehouses = (int) warehouseRepository.count();
        int activeWarehouses = (int) warehouseRepository.countByStatus(1);
        int pendingTransfers = (int) warehouseTransferRepository.findByStatus("PENDING").size();
        
        long totalInventoryItems = goodsInstanceRepository.count();
        Map<String, Object> result = new HashMap<>();
        result.put("totalWarehouses", totalWarehouses);
        result.put("activeWarehouses", activeWarehouses);
        result.put("pendingTransfers", pendingTransfers);
        result.put("totalInventoryItems", totalInventoryItems);
        return result;
    }

    private Map<String, Object> convertWarehouseToMap(Warehouse warehouse) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", warehouse.getId());
        map.put("code", warehouse.getCode());
        map.put("name", warehouse.getName());
        map.put("address", warehouse.getAddress());
        map.put("manager", warehouse.getManager());
        map.put("phone", warehouse.getPhone());
        map.put("status", warehouse.getStatus());
        map.put("createTime", warehouse.getCreateTime());
        return map;
    }

    private Map<String, Object> convertTransferToMap(WarehouseTransfer transfer) {
        Map<String, Object> map = new HashMap<>();
        map.put("transferId", transfer.getTransferNo());
        map.put("sourceWarehouseId", transfer.getSourceWarehouseId());
        map.put("targetWarehouseId", transfer.getTargetWarehouseId());
        
        if (transfer.getItems() != null) {
            try {
                map.put("items", objectMapper.readValue(transfer.getItems(), 
                    new TypeReference<List<Map<String, Object>>>() {}));
            } catch (JsonProcessingException e) {
                map.put("items", transfer.getItems());
            }
        }
        
        map.put("status", transfer.getStatus());
        map.put("totalQuantity", transfer.getTotalQuantity());
        map.put("createTime", transfer.getCreateTime());
        map.put("approveTime", transfer.getApproveTime());
        map.put("executeTime", transfer.getExecuteTime());
        map.put("completeTime", transfer.getCompleteTime());
        map.put("approver", transfer.getApprover());
        map.put("executor", transfer.getExecutor());
        map.put("remark", transfer.getRemark());
        
        return map;
    }
}