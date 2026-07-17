package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.dto.request.InboundOrderRequest;
import com.example.smartvisionstock.dto.request.OutboundOrderRequest;
import com.example.smartvisionstock.dto.response.InboundOrderDTO;
import com.example.smartvisionstock.dto.response.InboundOrderItemDTO;
import com.example.smartvisionstock.dto.response.OutboundOrderDTO;
import com.example.smartvisionstock.dto.response.OutboundOrderItemDTO;
import com.example.smartvisionstock.entity.*;
import com.example.smartvisionstock.repository.*;
import com.example.smartvisionstock.service.OrderService;
import com.example.smartvisionstock.service.StockReservationService;
import com.example.smartvisionstock.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    // 订单号序列号（进程内递增，防止同秒并发重号）
    private static final AtomicLong ORDER_SEQ = new AtomicLong(0);

    @Autowired
    private InboundOrderRepository inboundOrderRepository;
    
    @Autowired
    private InboundOrderItemRepository inboundOrderItemRepository;
    
    @Autowired
    private OutboundOrderRepository outboundOrderRepository;
    
    @Autowired
    private OutboundOrderItemRepository outboundOrderItemRepository;
    
    @Autowired
    private SupplierRepository supplierRepository;
    
    @Autowired
    private WarehouseRepository warehouseRepository;
    
    @Autowired
    private GoodsRepository goodsRepository;
    
    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;
    
    @Autowired
    private StorageLocationRepository storageLocationRepository;

    @Autowired
    private StockReservationService reservationService;

    @Override
    @Transactional
    public InboundOrderDTO createInboundOrder(InboundOrderRequest request) {
        InboundOrder order = new InboundOrder();
        order.setOrderNo(generateOrderNo("IN"));
        order.setType(request.getType());
        order.setSupplierId(request.getSupplierId());
        order.setWarehouseId(request.getWarehouseId());
        order.setStatus("DRAFT");
        order.setTotalQuantity(request.getItems().stream().mapToInt(i -> i.getQuantity()).sum());
        order.setCreateTime(LocalDateTime.now());
        order.setRemark(request.getRemark());
        
        order = inboundOrderRepository.save(order);

        for (var itemReq : request.getItems()) {
            InboundOrderItem item = new InboundOrderItem();
            item.setOrderId(order.getId());
            item.setGoodsId(itemReq.getGoodsId());
            item.setBatchNo(itemReq.getBatchNo());
            item.setQuantity(itemReq.getQuantity());
            item.setLocationId(itemReq.getLocationId());
            if (itemReq.getExpiryDate() != null && !itemReq.getExpiryDate().isEmpty()) {
                item.setExpiryDate(LocalDateTime.parse(itemReq.getExpiryDate(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            inboundOrderItemRepository.save(item);
        }

        return convertToInboundDTO(order);
    }

    @Override
    @Transactional
    public void auditInboundOrder(Long orderId) {
        InboundOrder order = inboundOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("入库单不存在"));
        if (!"DRAFT".equals(order.getStatus())) {
            throw new RuntimeException("只能审核草稿状态的单据");
        }
        order.setStatus("AUDITING");
        order.setAuditTime(LocalDateTime.now());
        inboundOrderRepository.save(order);
    }

    @Override
    @Transactional
    public void confirmInboundOrder(Long orderId) {
        InboundOrder order = inboundOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("入库单不存在"));
        if (!"AUDITING".equals(order.getStatus())) {
            throw new RuntimeException("只能确认审核中的单据");
        }

        List<InboundOrderItem> items = inboundOrderItemRepository.findByOrderId(orderId);
        
        // 记录本次入库已处理的库位：key=locationId, value=已创建的 GoodsInstance
        // 解决多个明细指向同一库位时后面覆盖前面的问题
        Map<Long, GoodsInstance> locationInstanceMap = new HashMap<>();
        
        for (var item : items) {
            StorageLocation location = null;
            if (item.getLocationId() != null) {
                location = storageLocationRepository.findById(item.getLocationId()).orElse(null);
            }
            
            // 同一库位且同一商品 → 合并库存，避免覆盖
            GoodsInstance instance = null;
            if (item.getLocationId() != null) {
                instance = locationInstanceMap.get(item.getLocationId());
                if (instance != null && instance.getGoodsId().equals(item.getGoodsId())) {
                    // 同库位同商品：累加数量
                    instance.setQuantity(instance.getQuantity() + item.getQuantity());
                    instance = goodsInstanceRepository.save(instance);
                }
            }
            
            if (instance == null) {
                // 新建货物实例
                instance = new GoodsInstance();
                instance.setGoodsId(item.getGoodsId());
                instance.setLocationId(item.getLocationId());
                instance.setBatchNo(item.getBatchNo());
                instance.setQuantity(item.getQuantity());
                instance.setInTime(LocalDateTime.now());
                instance.setExpiryDate(item.getExpiryDate());
                instance.setFrozen(false);
                instance.setOperator(UserContext.getCurrentUsernameOrDefault("admin"));
                instance = goodsInstanceRepository.save(instance);
                
                if (item.getLocationId() != null) {
                    locationInstanceMap.put(item.getLocationId(), instance);
                }
            }

            if (location != null) {
                location.setCurrentGoodsInstanceId(instance.getId());
                location.setStatus(1);
                storageLocationRepository.save(location);
            }
        }

        order.setStatus("COMPLETED");
        order.setConfirmTime(LocalDateTime.now());
        inboundOrderRepository.save(order);
    }

    @Override
    public Page<InboundOrderDTO> getInboundOrders(Integer page, Integer size, String orderNo, String status, String type, LocalDateTime startTime, LocalDateTime endTime) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<InboundOrder> orderPage;
        
        if (orderNo != null && !orderNo.isEmpty()) {
            orderPage = inboundOrderRepository.findByOrderNoContaining(orderNo, pageable);
        } else if (status != null && !status.isEmpty()) {
            orderPage = inboundOrderRepository.findByStatus(status, pageable);
        } else if (type != null && !type.isEmpty()) {
            orderPage = inboundOrderRepository.findByType(type, pageable);
        } else if (startTime != null && endTime != null) {
            orderPage = inboundOrderRepository.findByCreateTimeBetween(startTime, endTime, pageable);
        } else {
            orderPage = inboundOrderRepository.findAll(pageable);
        }

        return orderPage.map(this::convertToInboundDTO);
    }

    @Override
    public InboundOrderDTO getInboundOrder(Long orderId) {
        InboundOrder order = inboundOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("入库单不存在"));
        return convertToInboundDTO(order);
    }

    @Override
    @Transactional
    public OutboundOrderDTO createOutboundOrder(OutboundOrderRequest request) {
        OutboundOrder order = new OutboundOrder();
        order.setOrderNo(generateOrderNo("OUT"));
        order.setType(request.getType());
        order.setCustomerId(request.getCustomerId());
        order.setWarehouseId(request.getWarehouseId());
        order.setStatus("DRAFT");
        order.setTotalQuantity(request.getItems().stream().mapToInt(i -> i.getQuantity()).sum());
        order.setCreateTime(LocalDateTime.now());
        order.setRemark(request.getRemark());
        
        order = outboundOrderRepository.save(order);

        for (var itemReq : request.getItems()) {
            GoodsInstance instance = goodsInstanceRepository.findById(itemReq.getGoodsInstanceId()).orElse(null);
            if (instance == null) continue;
            
            OutboundOrderItem item = new OutboundOrderItem();
            item.setOrderId(order.getId());
            item.setGoodsInstanceId(itemReq.getGoodsInstanceId());
            item.setGoodsId(instance.getGoodsId());
            item.setBatchNo(instance.getBatchNo());
            item.setQuantity(itemReq.getQuantity());
            item.setLocationId(instance.getLocationId());
            outboundOrderItemRepository.save(item);
        }

        return convertToOutboundDTO(order);
    }

    @Override
    @Transactional
    public void auditOutboundOrder(Long orderId) {
        OutboundOrder order = outboundOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("出库单不存在"));
        if (!"DRAFT".equals(order.getStatus())) {
            throw new RuntimeException("只能审核草稿状态的单据");
        }
        order.setStatus("AUDITING");
        order.setAuditTime(LocalDateTime.now());
        outboundOrderRepository.save(order);

        // 审核通过即预占库存，锁定对应货物实例，防止并发超卖
        List<OutboundOrderItem> items = outboundOrderItemRepository.findByOrderId(orderId);
        reservationService.reserve(orderId, "OUTBOUND", items);
    }

    @Override
    @Transactional
    public void pickOutboundOrder(Long orderId) {
        OutboundOrder order = outboundOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("出库单不存在"));
        if (!"AUDITING".equals(order.getStatus())) {
            throw new RuntimeException("只能拣货审核中的单据");
        }
        order.setStatus("PICKING");
        order.setPickTime(LocalDateTime.now());
        outboundOrderRepository.save(order);
    }

    @Override
    @Transactional
    public void confirmOutboundOrder(Long orderId) {
        OutboundOrder order = outboundOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("出库单不存在"));
        if (!"PICKING".equals(order.getStatus())) {
            throw new RuntimeException("只能确认拣货中的单据");
        }

        List<OutboundOrderItem> items = outboundOrderItemRepository.findByOrderId(orderId);

        // 按货物实例 ID 升序统一加悲观写锁，避免多实例时不同订单加锁顺序不一致导致死锁
        List<Long> instanceIds = items.stream()
                .map(OutboundOrderItem::getGoodsInstanceId)
                .distinct().sorted().collect(Collectors.toList());
        Map<Long, GoodsInstance> locked = new HashMap<>();
        for (Long id : instanceIds) {
            locked.put(id, goodsInstanceRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new RuntimeException("出库明细对应的货物实例不存在，instanceId=" + id)));
        }

        // ---- 第一步：预校验所有明细可用库存是否充足（扣除他人已预占后）----
        // 本订单自身的预占不计入占用，因此 available 反映"真正可动用的库存"
        for (var item : items) {
            GoodsInstance instance = locked.get(item.getGoodsInstanceId());
            if (instance.isFrozen()) {
                throw new RuntimeException("货物实例已被冻结，无法出库，instanceId=" + item.getGoodsInstanceId());
            }
            int reservedByOthers = reservationService.getReservedQuantityExcludeOrder(instance.getId(), orderId);
            int available = instance.getQuantity() - reservedByOthers;
            if (available < item.getQuantity()) {
                throw new RuntimeException(
                        String.format("库存不足：货物实例[%d]当前库存%d，他人已预占%d，可用%d，需出库%d",
                                instance.getId(), instance.getQuantity(), reservedByOthers, available, item.getQuantity()));
            }
        }

        // ---- 第二步：在悲观写锁保护下执行实际扣减 ----
        for (var item : items) {
            GoodsInstance instance = locked.get(item.getGoodsInstanceId());
            int newQuantity = instance.getQuantity() - item.getQuantity();
            if (newQuantity == 0) {
                // 库存清零 → 删除实例并释放库位
                Long locId = instance.getLocationId();
                goodsInstanceRepository.delete(instance);
                if (locId != null) {
                    storageLocationRepository.findById(locId).ifPresent(location -> {
                        location.setCurrentGoodsInstanceId(null);
                        location.setStatus(0);
                        storageLocationRepository.save(location);
                    });
                }
            } else {
                instance.setQuantity(newQuantity);
                instance.setOutTime(LocalDateTime.now());
                // 实体已在事务中且被锁，Hibernate 会在事务提交时自动 flush；
                // 显式 save 以兼容非脏检查场景
                goodsInstanceRepository.save(instance);
            }
        }

        order.setStatus("COMPLETED");
        order.setConfirmTime(LocalDateTime.now());
        outboundOrderRepository.save(order);

        // 出库确认成功 → 消耗（核销）本订单的库存预占
        reservationService.consumeByOrder(orderId);
    }

    @Override
    @Transactional
    public void cancelOutboundOrder(Long orderId) {
        OutboundOrder order = outboundOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("出库单不存在"));
        if ("COMPLETED".equals(order.getStatus())) {
            throw new RuntimeException("已出库的单据不可取消");
        }
        // 释放该订单占用的库存预占
        reservationService.releaseByOrder(orderId);
        order.setStatus("CANCELLED");
        outboundOrderRepository.save(order);
    }

    @Override
    public Page<OutboundOrderDTO> getOutboundOrders(Integer page, Integer size, String orderNo, String status, String type, LocalDateTime startTime, LocalDateTime endTime) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<OutboundOrder> orderPage;
        
        if (orderNo != null && !orderNo.isEmpty()) {
            orderPage = outboundOrderRepository.findByOrderNoContaining(orderNo, pageable);
        } else if (status != null && !status.isEmpty()) {
            orderPage = outboundOrderRepository.findByStatus(status, pageable);
        } else if (type != null && !type.isEmpty()) {
            orderPage = outboundOrderRepository.findByType(type, pageable);
        } else if (startTime != null && endTime != null) {
            orderPage = outboundOrderRepository.findByCreateTimeBetween(startTime, endTime, pageable);
        } else {
            orderPage = outboundOrderRepository.findAll(pageable);
        }

        return orderPage.map(this::convertToOutboundDTO);
    }

    @Override
    public OutboundOrderDTO getOutboundOrder(Long orderId) {
        OutboundOrder order = outboundOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("出库单不存在"));
        return convertToOutboundDTO(order);
    }

    private InboundOrderDTO convertToInboundDTO(InboundOrder order) {
        final String[] supplierName = {""};
        if (order.getSupplierId() != null) {
            supplierRepository.findById(order.getSupplierId()).ifPresent(s -> supplierName[0] = s.getName());
        }
        
        final String[] warehouseName = {""};
        if (order.getWarehouseId() != null) {
            warehouseRepository.findById(order.getWarehouseId()).ifPresent(w -> warehouseName[0] = w.getName());
        }

        List<InboundOrderItemDTO> items = inboundOrderItemRepository.findByOrderId(order.getId())
                .stream()
                .map(item -> {
                    final String[] goodsName = {""};
                    final String[] goodsCode = {""};
                    if (item.getGoodsId() != null) {
                        goodsRepository.findById(item.getGoodsId()).ifPresent(g -> {
                            goodsName[0] = g.getName();
                            goodsCode[0] = g.getCode();
                        });
                    }
                    
                    final String[] locationCode = {""};
                    if (item.getLocationId() != null) {
                        storageLocationRepository.findById(item.getLocationId()).ifPresent(l -> locationCode[0] = l.getLocationCode());
                    }
                    
                    InboundOrderItemDTO itemDTO = new InboundOrderItemDTO();
                    itemDTO.setId(item.getId());
                    itemDTO.setGoodsId(item.getGoodsId());
                    itemDTO.setGoodsName(goodsName[0]);
                    itemDTO.setGoodsCode(goodsCode[0]);
                    itemDTO.setBatchNo(item.getBatchNo());
                    itemDTO.setQuantity(item.getQuantity());
                    itemDTO.setLocationId(item.getLocationId());
                    itemDTO.setLocationCode(locationCode[0]);
                    itemDTO.setExpiryDate(item.getExpiryDate());
                    return itemDTO;
                })
                .collect(Collectors.toList());

        InboundOrderDTO dto = new InboundOrderDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setType(order.getType());
        dto.setTypeText("purchase".equals(order.getType()) ? "采购入库" : "退货入库");
        dto.setSupplierId(order.getSupplierId());
        dto.setSupplierName(supplierName[0]);
        dto.setWarehouseId(order.getWarehouseId());
        dto.setWarehouseName(warehouseName[0]);
        dto.setStatus(order.getStatus());
        dto.setStatusText(getStatusText(order.getStatus()));
        dto.setTotalQuantity(order.getTotalQuantity());
        dto.setCreateTime(order.getCreateTime());
        dto.setAuditTime(order.getAuditTime());
        dto.setConfirmTime(order.getConfirmTime());
        dto.setRemark(order.getRemark());
        dto.setItems(items);
        return dto;
    }

    private OutboundOrderDTO convertToOutboundDTO(OutboundOrder order) {
        final String[] warehouseName = {""};
        if (order.getWarehouseId() != null) {
            warehouseRepository.findById(order.getWarehouseId()).ifPresent(w -> warehouseName[0] = w.getName());
        }

        List<OutboundOrderItemDTO> items = outboundOrderItemRepository.findByOrderId(order.getId())
                .stream()
                .map(item -> {
                    final String[] goodsName = {""};
                    final String[] goodsCode = {""};
                    if (item.getGoodsId() != null) {
                        goodsRepository.findById(item.getGoodsId()).ifPresent(g -> {
                            goodsName[0] = g.getName();
                            goodsCode[0] = g.getCode();
                        });
                    }
                    
                    final String[] locationCode = {""};
                    if (item.getLocationId() != null) {
                        storageLocationRepository.findById(item.getLocationId()).ifPresent(l -> locationCode[0] = l.getLocationCode());
                    }
                    
                    OutboundOrderItemDTO itemDTO = new OutboundOrderItemDTO();
                    itemDTO.setId(item.getId());
                    itemDTO.setGoodsInstanceId(item.getGoodsInstanceId());
                    itemDTO.setGoodsId(item.getGoodsId());
                    itemDTO.setGoodsName(goodsName[0]);
                    itemDTO.setGoodsCode(goodsCode[0]);
                    itemDTO.setBatchNo(item.getBatchNo());
                    itemDTO.setQuantity(item.getQuantity());
                    itemDTO.setLocationId(item.getLocationId());
                    itemDTO.setLocationCode(locationCode[0]);
                    return itemDTO;
                })
                .collect(Collectors.toList());

        OutboundOrderDTO dto = new OutboundOrderDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setType(order.getType());
        dto.setTypeText("sale".equals(order.getType()) ? "销售出库" : "退货出库");
        dto.setCustomerId(order.getCustomerId());
        dto.setWarehouseId(order.getWarehouseId());
        dto.setWarehouseName(warehouseName[0]);
        dto.setStatus(order.getStatus());
        dto.setStatusText(getOutboundStatusText(order.getStatus()));
        dto.setTotalQuantity(order.getTotalQuantity());
        dto.setCreateTime(order.getCreateTime());
        dto.setAuditTime(order.getAuditTime());
        dto.setPickTime(order.getPickTime());
        dto.setConfirmTime(order.getConfirmTime());
        dto.setRemark(order.getRemark());
        dto.setItems(items);
        return dto;
    }

    private String generateOrderNo(String prefix) {
        // 格式：前缀 + 年月日时分秒 + 4位序列号（解决同秒并发重号问题）
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long seq = ORDER_SEQ.incrementAndGet();
        return String.format("%s%s%06d", prefix, timestamp, seq % 1000000);
    }

    private String getStatusText(String status) {
        String text;
        switch (status) {
            case "DRAFT": text = "草稿"; break;
            case "AUDITING": text = "审核中"; break;
            case "COMPLETED": text = "已入库"; break;
            default: text = status; break;
        }
        return text;
    }

    private String getOutboundStatusText(String status) {
        String text;
        switch (status) {
            case "DRAFT": text = "草稿"; break;
            case "AUDITING": text = "审核中"; break;
            case "PICKING": text = "拣货中"; break;
            case "COMPLETED": text = "已出库"; break;
            default: text = status; break;
        }
        return text;
    }
}