package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.Goods;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsRepository extends JpaRepository<Goods, Long> {
    Optional<Goods> findByCode(String code);
    Optional<Goods> findByBarcode(String barcode);
    List<Goods> findByEnabledTrue();
    Page<Goods> findByEnabledTrue(Pageable pageable);
    Page<Goods> findByCategoryAndEnabledTrue(String category, Pageable pageable);
}
