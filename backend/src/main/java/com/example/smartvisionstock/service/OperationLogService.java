package com.example.smartvisionstock.service;

import com.example.smartvisionstock.entity.OperationLog;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public interface OperationLogService {
    void log(Long userId, String username, String operation, String module, 
             String url, String method, String params, String result, 
             String ip, Long duration, String status);
    Page<OperationLog> getLogs(int page, int size, Long userId, String username, 
                               String module, LocalDateTime startTime, LocalDateTime endTime);
    List<OperationLog> getRecentLogs();
}