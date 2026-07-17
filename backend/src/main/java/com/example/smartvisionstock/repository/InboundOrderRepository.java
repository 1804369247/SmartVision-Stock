package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.InboundOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InboundOrderRepository extends JpaRepository<InboundOrder, Long> {
    Page<InboundOrder> findByStatus(String status, Pageable pageable);
    Page<InboundOrder> findByOrderNoContaining(String orderNo, Pageable pageable);
    Page<InboundOrder> findByCreateTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<InboundOrder> findByType(String type, Pageable pageable);
    InboundOrder findByOrderNo(String orderNo);
    List<InboundOrder> findByStatusIn(List<String> statuses);
}