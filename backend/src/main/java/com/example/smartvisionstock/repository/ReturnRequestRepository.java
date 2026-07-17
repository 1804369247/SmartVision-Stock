package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    
    Optional<ReturnRequest> findByReturnNo(String returnNo);
    
    List<ReturnRequest> findByStatus(String status);
    
    List<ReturnRequest> findByCustomerId(Long customerId);
    
    List<ReturnRequest> findAllByOrderByCreateTimeDesc();
    
    List<ReturnRequest> findByStatusOrderByCreateTimeDesc(String status);
    
    List<ReturnRequest> findByCustomerIdOrderByCreateTimeDesc(Long customerId);
    
    boolean existsByReturnNo(String returnNo);
}