package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.GoodsInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsInstanceRepository extends JpaRepository<GoodsInstance, Long> {
    List<GoodsInstance> findByLocationId(Long locationId);
    List<GoodsInstance> findByGoodsId(Long goodsId);
}
