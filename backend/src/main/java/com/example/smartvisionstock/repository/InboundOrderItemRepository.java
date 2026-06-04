package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.InboundOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InboundOrderItemRepository extends JpaRepository<InboundOrderItem, Long> {
    List<InboundOrderItem> findByOrderId(Long orderId);
}