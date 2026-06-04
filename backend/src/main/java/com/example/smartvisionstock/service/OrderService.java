package com.example.smartvisionstock.service;

import com.example.smartvisionstock.dto.request.InboundOrderRequest;
import com.example.smartvisionstock.dto.request.OutboundOrderRequest;
import com.example.smartvisionstock.dto.response.InboundOrderDTO;
import com.example.smartvisionstock.dto.response.OutboundOrderDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

public interface OrderService {
    InboundOrderDTO createInboundOrder(InboundOrderRequest request);
    void auditInboundOrder(Long orderId);
    void confirmInboundOrder(Long orderId);
    Page<InboundOrderDTO> getInboundOrders(Integer page, Integer size, String orderNo, String status, String type, LocalDateTime startTime, LocalDateTime endTime);
    InboundOrderDTO getInboundOrder(Long orderId);

    OutboundOrderDTO createOutboundOrder(OutboundOrderRequest request);
    void auditOutboundOrder(Long orderId);
    void pickOutboundOrder(Long orderId);
    void confirmOutboundOrder(Long orderId);
    Page<OutboundOrderDTO> getOutboundOrders(Integer page, Integer size, String orderNo, String status, String type, LocalDateTime startTime, LocalDateTime endTime);
    OutboundOrderDTO getOutboundOrder(Long orderId);
}