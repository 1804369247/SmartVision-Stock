package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    Page<OperationLog> findByOperatorId(Long operatorId, Pageable pageable);

    Page<OperationLog> findByModule(String module, Pageable pageable);

    Page<OperationLog> findByOperateTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<OperationLog> findByOperatorNameContaining(String operatorName, Pageable pageable);

    List<OperationLog> findByOperatorIdOrderByOperateTimeDesc(Long operatorId);

    List<OperationLog> findByModuleOrderByOperateTimeDesc(String module);
}