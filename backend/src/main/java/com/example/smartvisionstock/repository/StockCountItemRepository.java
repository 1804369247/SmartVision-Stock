package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.StockCountItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockCountItemRepository extends JpaRepository<StockCountItem, Long> {
    List<StockCountItem> findByCountId(Long countId);
    List<StockCountItem> findByCountIdAndStatus(Long countId, String status);
    long countByCountId(Long countId);
    long countByCountIdAndStatus(Long countId, String status);
    void deleteByCountId(Long countId);
}