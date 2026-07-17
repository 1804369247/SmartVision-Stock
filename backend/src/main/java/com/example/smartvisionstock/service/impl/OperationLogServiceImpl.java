package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.entity.OperationLog;
import com.example.smartvisionstock.repository.OperationLogRepository;
import com.example.smartvisionstock.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void log(Long userId, String username, String operation, String module,
                    String url, String method, String params, String result,
                    String ip, Long duration, String status) {
        OperationLog log = new OperationLog();
        log.setOperatorId(userId);
        log.setOperatorName(username);
        log.setOperationType(operation);
        log.setModule(module);
        // 将完整信息结构化为 JSON 存入 detail 字段
        log.setDetail(String.format(
            "{\"url\":\"%s\",\"method\":\"%s\",\"params\":\"%s\",\"result\":\"%s\",\"duration\":%d,\"status\":\"%s\"}",
            escapeJson(url), escapeJson(method), escapeJson(params),
            escapeJson(result), duration != null ? duration : 0,
            escapeJson(status != null ? status : "SUCCESS")));
        log.setIpAddress(ip);
        operationLogRepository.save(log);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public Page<OperationLog> getLogs(int page, int size, Long userId, String username,
                                      String module, LocalDateTime startTime, LocalDateTime endTime) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "operateTime"));

        // 优先使用 userId 过滤
        if (userId != null) {
            return operationLogRepository.findByOperatorId(userId, pageable);
        }
        // 用户名模糊搜索
        if (username != null && !username.isEmpty()) {
            return operationLogRepository.findByOperatorNameContaining(username, pageable);
        }
        // 模块过滤
        if (module != null && !module.isEmpty()) {
            return operationLogRepository.findByModule(module, pageable);
        }
        // 时间范围过滤
        if (startTime != null && endTime != null) {
            return operationLogRepository.findByOperateTimeBetween(startTime, endTime, pageable);
        }

        return operationLogRepository.findAll(pageable);
    }

    @Override
    public List<OperationLog> getRecentLogs() {
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "operateTime"));
        return operationLogRepository.findAll(pageable).getContent();
    }
}