package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.StorageLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {
    Optional<StorageLocation> findByLocationCode(String locationCode);
    Optional<StorageLocation> findByBarcode(String barcode);
    List<StorageLocation> findByArea(String area);
    List<StorageLocation> findByStatus(Integer status);
}
