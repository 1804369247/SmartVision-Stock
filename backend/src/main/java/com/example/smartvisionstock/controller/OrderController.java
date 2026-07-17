package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.request.InboundOrderRequest;
import com.example.smartvisionstock.dto.request.OutboundOrderRequest;
import com.example.smartvisionstock.dto.response.InboundOrderDTO;
import com.example.smartvisionstock.dto.response.OutboundOrderDTO;
import com.example.smartvisionstock.entity.InboundOrder;
import com.example.smartvisionstock.entity.OutboundOrder;
import com.example.smartvisionstock.repository.InboundOrderItemRepository;
import com.example.smartvisionstock.repository.InboundOrderRepository;
import com.example.smartvisionstock.repository.OutboundOrderItemRepository;
import com.example.smartvisionstock.repository.OutboundOrderRepository;
import com.example.smartvisionstock.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import com.example.smartvisionstock.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private InboundOrderRepository inboundOrderRepository;

    @Autowired
    private OutboundOrderRepository outboundOrderRepository;

    @Autowired
    private InboundOrderItemRepository inboundOrderItemRepository;

    @Autowired
    private OutboundOrderItemRepository outboundOrderItemRepository;

    @PostMapping("/inbound")
    public ApiResponse<InboundOrderDTO> createInboundOrder(@RequestBody @Valid InboundOrderRequest request) {
        try {
            InboundOrderDTO order = orderService.createInboundOrder(request);
            return ApiResponse.success(order);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/inbound/{orderId}/audit")
    public ApiResponse<Object> auditInboundOrder(@PathVariable Long orderId) {
        try {
            orderService.auditInboundOrder(orderId);
            return ApiResponse.success("审核通过");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/inbound/{orderId}/confirm")
    public ApiResponse<Object> confirmInboundOrder(@PathVariable Long orderId) {
        try {
            orderService.confirmInboundOrder(orderId);
            return ApiResponse.success("入库确认成功");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/inbound/list")
    public ApiResponse<Map<String, Object>> getInboundOrders(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        Page<InboundOrderDTO> orderPage = orderService.getInboundOrders(page, size, orderNo, status, type, startTime, endTime);
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", orderPage.getContent());
        result.put("totalElements", orderPage.getTotalElements());
        result.put("totalPages", orderPage.getTotalPages());
        result.put("currentPage", orderPage.getNumber());
        result.put("pageSize", orderPage.getSize());
        return ApiResponse.success(result);
    }

    @GetMapping("/inbound/{orderId}")
    public ApiResponse<InboundOrderDTO> getInboundOrder(@PathVariable Long orderId) {
        try {
            InboundOrderDTO order = orderService.getInboundOrder(orderId);
            return ApiResponse.success(order);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/outbound")
    public ApiResponse<OutboundOrderDTO> createOutboundOrder(@RequestBody @Valid OutboundOrderRequest request) {
        try {
            OutboundOrderDTO order = orderService.createOutboundOrder(request);
            return ApiResponse.success(order);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/outbound/{orderId}/audit")
    public ApiResponse<Object> auditOutboundOrder(@PathVariable Long orderId) {
        try {
            orderService.auditOutboundOrder(orderId);
            return ApiResponse.success("审核通过");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/outbound/{orderId}/pick")
    public ApiResponse<Object> pickOutboundOrder(@PathVariable Long orderId) {
        try {
            orderService.pickOutboundOrder(orderId);
            return ApiResponse.success("拣货完成");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/outbound/{orderId}/confirm")
    public ApiResponse<Object> confirmOutboundOrder(@PathVariable Long orderId) {
        orderService.confirmOutboundOrder(orderId);
        return ApiResponse.success("出库确认成功");
    }

    @PutMapping("/outbound/{orderId}/cancel")
    public ApiResponse<Object> cancelOutboundOrder(@PathVariable Long orderId) {
        try {
            orderService.cancelOutboundOrder(orderId);
            return ApiResponse.success("出库单已取消，库存预占已释放");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/outbound/list")
    public ApiResponse<Map<String, Object>> getOutboundOrders(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        Page<OutboundOrderDTO> orderPage = orderService.getOutboundOrders(page, size, orderNo, status, type, startTime, endTime);
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", orderPage.getContent());
        result.put("totalElements", orderPage.getTotalElements());
        result.put("totalPages", orderPage.getTotalPages());
        result.put("currentPage", orderPage.getNumber());
        result.put("pageSize", orderPage.getSize());
        return ApiResponse.success(result);
    }

    @GetMapping("/outbound/{orderId}")
    public ApiResponse<OutboundOrderDTO> getOutboundOrder(@PathVariable Long orderId) {
        try {
            OutboundOrderDTO order = orderService.getOutboundOrder(orderId);
            return ApiResponse.success(order);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/inbound/{orderId}")
    public ApiResponse<Object> deleteInboundOrder(@PathVariable Long orderId) {
        try {
            InboundOrder order = inboundOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("入库单不存在"));
            if (!"DRAFT".equals(order.getStatus())) {
                return ApiResponse.error(400, "只能删除草稿状态的订单");
            }
            inboundOrderItemRepository.deleteByOrderId(orderId);
            inboundOrderRepository.deleteById(orderId);
            return ApiResponse.success("删除成功");
        } catch (Exception e) {
            return ApiResponse.error(400, "删除失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/outbound/{orderId}")
    public ApiResponse<Object> deleteOutboundOrder(@PathVariable Long orderId) {
        try {
            OutboundOrder order = outboundOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("出库单不存在"));
            if (!"DRAFT".equals(order.getStatus())) {
                return ApiResponse.error(400, "只能删除草稿状态的订单");
            }
            outboundOrderItemRepository.deleteByOrderId(orderId);
            outboundOrderRepository.deleteById(orderId);
            return ApiResponse.success("删除成功");
        } catch (Exception e) {
            return ApiResponse.error(400, "删除失败：" + e.getMessage());
        }
    }
}