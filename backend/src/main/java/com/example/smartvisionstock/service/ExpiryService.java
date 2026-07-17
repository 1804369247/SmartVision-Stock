package com.example.smartvisionstock.service;

import java.util.List;
import java.util.Map;

public interface ExpiryService {

    List<Map<String, Object>> checkExpiringProducts(int daysThreshold);

    Map<String, Object> getExpiryAlert(String alertId);

    List<Map<String, Object>> getActiveAlerts();

    Map<String, Object> processAlert(String alertId, String action);

    Map<String, Object> sendExpiryNotification(List<String> alertIds);

    Map<String, Object> getExpiryStatistics();

    void runDailyExpiryCheck();

    List<Map<String, Object>> getExpiredProducts();

    Map<String, Object> markAsExpired(String batchId);
}