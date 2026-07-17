package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByEnabledTrue();
    Page<Supplier> findByEnabledTrue(Pageable pageable);
    Supplier findBySupplierCode(String supplierCode);
}