package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.StockCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockCountRepository extends JpaRepository<StockCount, Long> {
    Page<StockCount> findByStatus(Integer status, Pageable pageable);
    Page<StockCount> findByWarehouseId(Long warehouseId, Pageable pageable);
    Page<StockCount> findByArea(String area, Pageable pageable);
    List<StockCount> findByStatusOrderByCreateTimeDesc(Integer status);
    StockCount findByCountNo(String countNo);
}