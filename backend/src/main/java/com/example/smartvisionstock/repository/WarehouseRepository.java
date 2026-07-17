package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    List<Warehouse> findByStatus(Integer status);
    Page<Warehouse> findByStatus(Integer status, Pageable pageable);
    Warehouse findByCode(String code);
    long countByStatus(Integer status);
}