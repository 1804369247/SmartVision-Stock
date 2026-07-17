package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.entity.Shipment;
import com.example.smartvisionstock.repository.ShipmentRepository;
import com.example.smartvisionstock.service.LogisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 物流管理服务 —— 基于 Shipment 数据库表，不再使用内存存储。
 * 轨迹数据为模拟生成（生产环境应接入真实物流 API：顺丰/京东/圆通等）。
 */
@Service
public class LogisticsServiceImpl implements LogisticsService {

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Value("${app.logistics.label-url:https://api.logistics.example.com/label/}")
    private String labelUrlBase;

    /** 运单号序列号（防止同秒并发重号） */
    private static final AtomicLong TRACKING_SEQ = new AtomicLong(0);

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public Map<String, Object> createShipment(Map<String, Object> shipmentData) {
        String timestamp = LocalDateTime.now().format(DT_FMT);
        long seq = TRACKING_SEQ.incrementAndGet();
        String trackingNo = String.format("SF%s%06d", timestamp, seq % 1000000);

        Shipment shipment = new Shipment();
        shipment.setTrackingNo(trackingNo);
        shipment.setOrderId(shipmentData.get("orderId") instanceof Number 
                ? ((Number) shipmentData.get("orderId")).longValue() : null);
        shipment.setShippingCompany((String) shipmentData.getOrDefault("shippingCompany", "顺丰速运"));
        shipment.setSenderName((String) shipmentData.get("senderName"));
        shipment.setSenderAddress((String) shipmentData.get("senderAddress"));
        shipment.setReceiverName((String) shipmentData.get("receiverName"));
        shipment.setReceiverAddress((String) shipmentData.get("receiverAddress"));
        shipment.setWeight(shipmentData.get("weight") instanceof Number 
                ? ((Number) shipmentData.get("weight")).doubleValue() : 1.0);
        shipment.setStatus("CREATED");
        shipment.setCreateTime(LocalDateTime.now());
        shipment.setUpdateTime(LocalDateTime.now());
        shipment.setEstimatedDelivery(LocalDateTime.now().plusDays(3)); // 默认3天到达

        shipment = shipmentRepository.save(shipment);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("trackingNo", trackingNo);
        result.put("estimatedDelivery", shipment.getEstimatedDelivery().format(DateTimeFormatter.ISO_LOCAL_DATE));
        result.put("message", "运单创建成功");
        return result;
    }

    @Override
    public Map<String, Object> trackShipment(String trackingNo) {
        Optional<Shipment> optional = shipmentRepository.findByTrackingNo(trackingNo);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "运单不存在");
            return result;
        }

        Shipment shipment = optional.get();
        List<Map<String, Object>> trackingHistory = generateTrackingHistory(shipment);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("trackingNo", trackingNo);
        result.put("status", shipment.getStatus());
        result.put("statusText", getStatusText(shipment.getStatus()));
        result.put("shippingCompany", shipment.getShippingCompany());
        result.put("currentLocation", getCurrentLocation(shipment));
        result.put("estimatedDelivery", shipment.getEstimatedDelivery() != null 
                ? shipment.getEstimatedDelivery().format(DateTimeFormatter.ISO_LOCAL_DATE) : "待定");
        result.put("trackingHistory", trackingHistory);
        return result;
    }

    /**
     * 根据运单状态和创建时间生成半真实轨迹。
     * 生产环境应接入真实物流 API。
     */
    private List<Map<String, Object>> generateTrackingHistory(Shipment shipment) {
        List<Map<String, Object>> history = new ArrayList<>();
        LocalDateTime base = shipment.getCreateTime();

        // 节点1：已揽收
        history.add(createTrackingNode(
                base.plusHours(1), shipment.getSenderAddress(), "已揽收", "快递员已揽收"));

        // 节点2：到达转运中心
        history.add(createTrackingNode(
                base.plusHours(4), "转运中心", "运输中", "已到达转运中心"));

        if (isStatusAtLeast(shipment.getStatus(), "IN_TRANSIT")) {
            history.add(createTrackingNode(
                    base.plusHours(8), "干线运输中", "运输中", "正在发往目的地"));
        }

        if (isStatusAtLeast(shipment.getStatus(), "DELIVERED")) {
            history.add(createTrackingNode(
                    base.plusHours(20), "目的地转运中心", "派送中", "已到达目的地，准备派送"));
            history.add(createTrackingNode(
                    base.plusHours(24), shipment.getReceiverAddress(), "已签收", "已签收，感谢使用"));
        }

        return history;
    }

    private Map<String, Object> createTrackingNode(LocalDateTime time, String location, 
                                                     String status, String description) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("time", time.format(DISPLAY_FMT));
        node.put("location", location);
        node.put("status", status);
        node.put("description", description);
        return node;
    }

    private boolean isStatusAtLeast(String current, String target) {
        String[] order = {"CREATED", "IN_TRANSIT", "DELIVERED"};
        int cur = java.util.Arrays.asList(order).indexOf(current);
        int tgt = java.util.Arrays.asList(order).indexOf(target);
        return cur >= tgt;
    }

    private String getCurrentLocation(Shipment shipment) {
        switch (shipment.getStatus()) {
            case "CREATED": return shipment.getSenderAddress();
            case "IN_TRANSIT": return "干线运输中";
            case "DELIVERED": return shipment.getReceiverAddress();
            default: return "未知";
        }
    }

    private String getStatusText(String status) {
        switch (status) {
            case "CREATED": return "已揽收";
            case "IN_TRANSIT": return "运输中";
            case "DELIVERED": return "已签收";
            default: return status;
        }
    }

    @Override
    public Map<String, Object> generateLabel(String orderId) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("orderId", orderId);
        // 生产环境应调用物流公司电子面单 API
        result.put("labelUrl", labelUrlBase + orderId);
        result.put("message", "电子面单生成成功（模拟）");
        return result;
    }

    @Override
    public Map<String, Object> getShippingCompanies() {
        List<Map<String, Object>> companies = new ArrayList<>();
        companies.add(Map.of("code", "SF", "name", "顺丰速运", "pricePerKg", 12.0));
        companies.add(Map.of("code", "JD", "name", "京东物流", "pricePerKg", 8.0));
        companies.add(Map.of("code", "YT", "name", "圆通速递", "pricePerKg", 6.0));
        companies.add(Map.of("code", "ZT", "name", "中通快递", "pricePerKg", 5.0));
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("companies", companies);
        return result;
    }

    @Override
    public Map<String, Object> estimateShippingCost(String from, String to, double weight) {
        double baseCost = 10.0;
        double weightCost = weight * 8.0;
        double totalCost = baseCost + weightCost;

        // 粗略的距离估算（同省 vs 跨省）
        if (from != null && to != null) {
            String fromProv = extractProvince(from);
            String toProv = extractProvince(to);
            if (fromProv.equals(toProv)) {
                totalCost -= 5.0;
            } else {
                totalCost += 10.0;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("from", from);
        result.put("to", to);
        result.put("weight", weight);
        result.put("baseCost", baseCost);
        result.put("weightCost", weightCost);
        result.put("totalCost", Math.max(totalCost, 15.0));
        result.put("estimatedDays", 3);
        return result;
    }

    private String extractProvince(String address) {
        if (address == null) return "";
        for (String prov : new String[]{"北京","天津","上海","重庆","广东","浙江","江苏","山东","河南",
                "四川","湖北","湖南","福建","安徽","河北","辽宁","陕西","江西","广西","云南","贵州",
                "山西","吉林","黑龙江","甘肃","内蒙古","新疆","海南","宁夏","青海","西藏"}) {
            if (address.contains(prov)) return prov;
        }
        return address;
    }

    @Override
    public List<Map<String, Object>> getShipmentsByOrder(Long orderId) {
        return shipmentRepository.findByOrderId(orderId).stream()
                .map(this::shipmentToMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> updateShipmentStatus(String trackingNo, String status) {
        Optional<Shipment> optional = shipmentRepository.findByTrackingNo(trackingNo);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "运单不存在");
            return result;
        }

        Shipment shipment = optional.get();
        shipment.setStatus(status);
        shipment.setUpdateTime(LocalDateTime.now());
        
        // 签收时更新预计送达
        if ("DELIVERED".equals(status) && shipment.getEstimatedDelivery() == null) {
            shipment.setEstimatedDelivery(LocalDateTime.now());
        }
        
        shipmentRepository.save(shipment);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("status", status);
        result.put("statusText", getStatusText(status));
        result.put("message", "运单状态已更新");
        return result;
    }

    @Override
    public Map<String, Object> getLogisticsStatistics() {
        long totalShipments = shipmentRepository.count();
        long createdShipments = shipmentRepository.countByStatus("CREATED");
        long inTransitShipments = shipmentRepository.countByStatus("IN_TRANSIT");
        long deliveredShipments = shipmentRepository.countByStatus("DELIVERED");
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("totalShipments", (int) totalShipments);
        result.put("createdShipments", (int) createdShipments);
        result.put("inTransitShipments", (int) inTransitShipments);
        result.put("deliveredShipments", (int) deliveredShipments);
        return result;
    }

    private Map<String, Object> shipmentToMap(Shipment s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("trackingNo", s.getTrackingNo());
        m.put("orderId", s.getOrderId());
        m.put("shippingCompany", s.getShippingCompany());
        m.put("senderName", s.getSenderName());
        m.put("senderAddress", s.getSenderAddress());
        m.put("receiverName", s.getReceiverName());
        m.put("receiverAddress", s.getReceiverAddress());
        m.put("weight", s.getWeight());
        m.put("status", s.getStatus());
        m.put("statusText", getStatusText(s.getStatus()));
        m.put("createTime", s.getCreateTime());
        m.put("updateTime", s.getUpdateTime());
        m.put("estimatedDelivery", s.getEstimatedDelivery() != null 
                ? s.getEstimatedDelivery().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
        return m;
    }
}
