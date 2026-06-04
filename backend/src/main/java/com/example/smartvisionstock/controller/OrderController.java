package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.request.InboundOrderRequest;
import com.example.smartvisionstock.dto.request.OutboundOrderRequest;
import com.example.smartvisionstock.dto.response.InboundOrderDTO;
import com.example.smartvisionstock.dto.response.OutboundOrderDTO;
import com.example.smartvisionstock.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/inbound")
    public Map<String, Object> createInboundOrder(@RequestBody InboundOrderRequest request) {
        try {
            InboundOrderDTO order = orderService.createInboundOrder(request);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", order);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @PutMapping("/inbound/{orderId}/audit")
    public Map<String, Object> auditInboundOrder(@PathVariable Long orderId) {
        try {
            orderService.auditInboundOrder(orderId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @PutMapping("/inbound/{orderId}/confirm")
    public Map<String, Object> confirmInboundOrder(@PathVariable Long orderId) {
        try {
            orderService.confirmInboundOrder(orderId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @GetMapping("/inbound/list")
    public Map<String, Object> getInboundOrders(
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
        return result;
    }

    @GetMapping("/inbound/{orderId}")
    public Map<String, Object> getInboundOrder(@PathVariable Long orderId) {
        try {
            InboundOrderDTO order = orderService.getInboundOrder(orderId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", order);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @PostMapping("/outbound")
    public Map<String, Object> createOutboundOrder(@RequestBody OutboundOrderRequest request) {
        try {
            OutboundOrderDTO order = orderService.createOutboundOrder(request);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", order);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @PutMapping("/outbound/{orderId}/audit")
    public Map<String, Object> auditOutboundOrder(@PathVariable Long orderId) {
        try {
            orderService.auditOutboundOrder(orderId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @PutMapping("/outbound/{orderId}/pick")
    public Map<String, Object> pickOutboundOrder(@PathVariable Long orderId) {
        try {
            orderService.pickOutboundOrder(orderId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @PutMapping("/outbound/{orderId}/confirm")
    public Map<String, Object> confirmOutboundOrder(@PathVariable Long orderId) {
        try {
            orderService.confirmOutboundOrder(orderId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @GetMapping("/outbound/list")
    public Map<String, Object> getOutboundOrders(
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
        return result;
    }

    @GetMapping("/outbound/{orderId}")
    public Map<String, Object> getOutboundOrder(@PathVariable Long orderId) {
        try {
            OutboundOrderDTO order = orderService.getOutboundOrder(orderId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", order);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }
}