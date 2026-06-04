package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.OutboundOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboundOrderRepository extends JpaRepository<OutboundOrder, Long> {
    Page<OutboundOrder> findByStatus(String status, Pageable pageable);
    Page<OutboundOrder> findByCreateTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<OutboundOrder> findByType(String type, Pageable pageable);
    OutboundOrder findByOrderNo(String orderNo);
    List<OutboundOrder> findByStatusIn(List<String> statuses);
}