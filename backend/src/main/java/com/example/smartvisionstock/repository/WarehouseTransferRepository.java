package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.WarehouseTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseTransferRepository extends JpaRepository<WarehouseTransfer, Long> {
    
    Optional<WarehouseTransfer> findByTransferNo(String transferNo);
    
    List<WarehouseTransfer> findByStatus(String status);
    
    List<WarehouseTransfer> findBySourceWarehouseId(Long warehouseId);
    
    List<WarehouseTransfer> findByTargetWarehouseId(Long warehouseId);
    
    List<WarehouseTransfer> findAllByOrderByCreateTimeDesc();
    
    List<WarehouseTransfer> findByStatusOrderByCreateTimeDesc(String status);
    
    boolean existsByTransferNo(String transferNo);
}