package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.entity.ReturnRequest;
import com.example.smartvisionstock.repository.ReturnRequestRepository;
import com.example.smartvisionstock.service.ReturnService;
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
public class ReturnServiceImpl implements ReturnService {

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Autowired
    private com.example.smartvisionstock.repository.GoodsInstanceRepository goodsInstanceRepository;

    @Autowired
    private com.example.smartvisionstock.repository.GoodsRepository goodsRepository;

    @Autowired
    private com.example.smartvisionstock.service.PutawayService putawayService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final AtomicLong RETURN_SEQ = new AtomicLong(0);

    @Override
    @Transactional
    public Map<String, Object> createReturnRequest(Map<String, Object> returnData) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long seq = RETURN_SEQ.incrementAndGet();
        String returnNo = String.format("RET%s%06d", timestamp, seq % 1000000);
        
        Object orderIdObj = returnData.get("orderId");
        Object customerIdObj = returnData.get("customerId");
        
        if (orderIdObj == null || customerIdObj == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "orderId 和 customerId 不能为空");
            return result;
        }
        
        Long orderId = orderIdObj instanceof Long ? (Long) orderIdObj 
                : (orderIdObj instanceof Number ? ((Number) orderIdObj).longValue() : null);
        Long customerId = customerIdObj instanceof Long ? (Long) customerIdObj 
                : (customerIdObj instanceof Number ? ((Number) customerIdObj).longValue() : null);
        
        if (orderId == null || customerId == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "orderId 和 customerId 必须为数字");
            return result;
        }
        
        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setReturnNo(returnNo);
        returnRequest.setOrderId(orderId);
        returnRequest.setCustomerId(customerId);
        returnRequest.setOperatorId(UserContext.getCurrentUserIdOrDefault(1L));
        
        try {
            returnRequest.setItems(objectMapper.writeValueAsString(returnData.get("items")));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize items");
        }
        
        returnRequest.setReason((String) returnData.get("reason"));
        returnRequest.setStatus("PENDING");
        returnRequest.setCreateTime(LocalDateTime.now());
        returnRequest.setRefundAmount(0.0);
        
        returnRequestRepository.save(returnRequest);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("returnId", returnNo);
        result.put("message", "退货申请已创建");
        return result;
    }

    @Override
    public Map<String, Object> getReturnRequest(String returnId) {
        Optional<ReturnRequest> optional = returnRequestRepository.findByReturnNo(returnId);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "退货申请不存在");
            return result;
        }
        
        ReturnRequest request = optional.get();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", convertToMap(request));
        return result;
    }

    @Override
    public List<Map<String, Object>> getReturnRequests(String status) {
        List<ReturnRequest> requests;
        if (status == null || status.isEmpty()) {
            requests = returnRequestRepository.findAllByOrderByCreateTimeDesc();
        } else {
            requests = returnRequestRepository.findByStatusOrderByCreateTimeDesc(status);
        }
        
        return requests.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> inspectReturn(String returnId, Map<String, Object> inspectData) {
        Optional<ReturnRequest> optional = returnRequestRepository.findByReturnNo(returnId);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "退货申请不存在");
            return result;
        }
        
        ReturnRequest request = optional.get();
        
        if (!"PENDING".equals(request.getStatus())) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "当前状态不允许质检，仅待处理状态的退货可质检。当前状态：" + request.getStatus());
            return result;
        }
        
        request.setStatus("INSPECTED");
        request.setInspectTime(LocalDateTime.now());
        
        try {
            request.setInspectResult(objectMapper.writeValueAsString(inspectData));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize inspect result");
        }
        
        request.setQualityGrade((String) inspectData.getOrDefault("qualityGrade", "A"));
        request.setInspector(UserContext.getCurrentUsernameOrDefault("system"));
        
        double refundAmount = calculateRefundAmount(request, inspectData);
        request.setRefundAmount(refundAmount);
        
        returnRequestRepository.save(request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("qualityGrade", inspectData.get("qualityGrade"));
        result.put("refundAmount", refundAmount);
        result.put("message", "质检完成");
        return result;
    }

    private double calculateRefundAmount(ReturnRequest request, Map<String, Object> inspectData) {
        String qualityGrade = (String) inspectData.getOrDefault("qualityGrade", "A");
        
        List<Map<String, Object>> items;
        try {
            items = objectMapper.readValue(request.getItems(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            return 0;
        }
        
        double totalAmount = 0;
        for (Map<String, Object> item : items) {
            double price = ((Number) item.getOrDefault("price", 100.0)).doubleValue();
            int quantity = ((Number) item.getOrDefault("quantity", 1)).intValue();
            totalAmount += price * quantity;
        }
        
        double refundRate;
        switch (qualityGrade) {
            case "A": refundRate = 1.0; break;
            case "B": refundRate = 0.8; break;
            case "C": refundRate = 0.5; break;
            case "D": refundRate = 0.0; break;
            default: refundRate = 0.5; break;
        }
        
        return totalAmount * refundRate;
    }

    @Override
    @Transactional
    public Map<String, Object> confirmReturn(String returnId) {
        Optional<ReturnRequest> optional = returnRequestRepository.findByReturnNo(returnId);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "退货申请不存在");
            return result;
        }
        
        ReturnRequest request = optional.get();
        
        if (!"INSPECTED".equals(request.getStatus())) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "当前状态不允许确认，仅已质检状态的退货可确认。当前状态：" + request.getStatus());
            return result;
        }

        List<Map<String, Object>> items;
        try {
            items = objectMapper.readValue(request.getItems(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "退货物品数据解析失败");
            return result;
        }

        int restoredCount = 0;
        for (Map<String, Object> item : items) {
            String sku = (String) item.getOrDefault("sku", "");
            String barcode = (String) item.getOrDefault("barcode", "");
            int qty = item.get("quantity") instanceof Number ? ((Number) item.get("quantity")).intValue() : 1;

            com.example.smartvisionstock.entity.Goods goods = null;
            if (!sku.isEmpty()) {
                goods = goodsRepository.findByCode(sku).orElse(null);
            }
            if (goods == null && !barcode.isEmpty()) {
                goods = goodsRepository.findByBarcode(barcode).orElse(null);
            }

            if (goods == null || qty <= 0) continue;

            com.example.smartvisionstock.entity.GoodsInstance instance =
                    new com.example.smartvisionstock.entity.GoodsInstance();
            instance.setGoodsId(goods.getId());
            instance.setBatchNo("RET-" + returnId.substring(0, 8));
            instance.setQuantity(qty);
            instance.setInTime(LocalDateTime.now());
            instance.setOperator(UserContext.getCurrentUsernameOrDefault("system"));
            instance.setFrozen(false);
            putawayService.putaway(instance);
            restoredCount++;
        }
        
        request.setStatus("CONFIRMED");
        request.setConfirmTime(LocalDateTime.now());
        
        returnRequestRepository.save(request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "退货已确认，" + restoredCount + " 件货物已恢复至库存（待上架分配库位）");
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> rejectReturn(String returnId, String reason) {
        Optional<ReturnRequest> optional = returnRequestRepository.findByReturnNo(returnId);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "退货申请不存在");
            return result;
        }
        
        ReturnRequest request = optional.get();
        
        if (!"PENDING".equals(request.getStatus()) && !"INSPECTED".equals(request.getStatus())) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "当前状态不允许拒绝，仅待处理或已质检状态的退货可拒绝。当前状态：" + request.getStatus());
            return result;
        }
        
        request.setStatus("REJECTED");
        request.setRejectTime(LocalDateTime.now());
        request.setRejectReason(reason);
        
        returnRequestRepository.save(request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "退货已拒绝：" + reason);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> processRefund(String returnId) {
        Optional<ReturnRequest> optional = returnRequestRepository.findByReturnNo(returnId);
        if (!optional.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "退货申请不存在");
            return result;
        }
        
        ReturnRequest request = optional.get();
        
        if (!"CONFIRMED".equals(request.getStatus())) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "当前状态不允许退款，仅已确认状态的退货可退款。当前状态：" + request.getStatus());
            return result;
        }
        
        request.setStatus("REFUNDED");
        request.setRefundTime(LocalDateTime.now());
        
        returnRequestRepository.save(request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("refundAmount", request.getRefundAmount());
        result.put("message", "退款已处理");
        return result;
    }

    @Override
    public Map<String, Object> getReturnStatistics() {
        List<ReturnRequest> allRequests = returnRequestRepository.findAll();
        
        int totalReturns = allRequests.size();
        int pendingReturns = 0;
        int inspectedReturns = 0;
        int confirmedReturns = 0;
        int rejectedReturns = 0;
        int refundedReturns = 0;
        double totalRefundAmount = 0;
        
        for (ReturnRequest request : allRequests) {
            String status = request.getStatus();
            switch (status) {
                case "PENDING": pendingReturns++; break;
                case "INSPECTED": inspectedReturns++; break;
                case "CONFIRMED": confirmedReturns++; break;
                case "REJECTED": rejectedReturns++; break;
                case "REFUNDED": refundedReturns++; break;
            }
            if (request.getRefundAmount() != null) {
                totalRefundAmount += request.getRefundAmount();
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("totalReturns", totalReturns);
        result.put("pendingReturns", pendingReturns);
        result.put("inspectedReturns", inspectedReturns);
        result.put("confirmedReturns", confirmedReturns);
        result.put("rejectedReturns", rejectedReturns);
        result.put("refundedReturns", refundedReturns);
        result.put("totalRefundAmount", totalRefundAmount);
        return result;
    }

    @Override
    public List<Map<String, Object>> getReturnHistory(String customerId) {
        Long customerIdLong = Long.parseLong(customerId);
        List<ReturnRequest> requests = returnRequestRepository.findByCustomerIdOrderByCreateTimeDesc(customerIdLong);
        
        return requests.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    private Map<String, Object> convertToMap(ReturnRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("returnId", request.getReturnNo());
        map.put("orderId", request.getOrderId());
        map.put("customerId", request.getCustomerId());
        
        if (request.getItems() != null) {
            try {
                map.put("items", objectMapper.readValue(request.getItems(), new TypeReference<List<Map<String, Object>>>() {}));
            } catch (JsonProcessingException e) {
                map.put("items", request.getItems());
            }
        }
        
        map.put("reason", request.getReason());
        map.put("status", request.getStatus());
        
        if (request.getInspectResult() != null) {
            try {
                map.put("inspectResult", objectMapper.readValue(request.getInspectResult(), new TypeReference<Map<String, Object>>() {}));
            } catch (JsonProcessingException e) {
                map.put("inspectResult", request.getInspectResult());
            }
        }
        
        map.put("qualityGrade", request.getQualityGrade());
        map.put("inspector", request.getInspector());
        map.put("refundAmount", request.getRefundAmount());
        map.put("rejectReason", request.getRejectReason());
        map.put("operatorId", request.getOperatorId());
        map.put("createTime", request.getCreateTime());
        map.put("inspectTime", request.getInspectTime());
        map.put("confirmTime", request.getConfirmTime());
        map.put("rejectTime", request.getRejectTime());
        map.put("refundTime", request.getRefundTime());
        map.put("remark", request.getRemark());
        
        return map;
    }
}