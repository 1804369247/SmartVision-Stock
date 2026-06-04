package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    List<Warehouse> findByEnabledTrue();
    Warehouse findByWarehouseCode(String warehouseCode);
}