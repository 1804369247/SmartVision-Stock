package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.InoutRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InoutRecordRepository extends JpaRepository<InoutRecord, Long> {
    List<InoutRecord> findByType(String type);
    List<InoutRecord> findByOperateTimeBetween(LocalDateTime start, LocalDateTime end);
    List<InoutRecord> findByGoodsInstanceId(Long goodsInstanceId);
}
