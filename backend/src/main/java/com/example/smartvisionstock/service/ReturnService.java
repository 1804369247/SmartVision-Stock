package com.example.smartvisionstock.service;

import java.util.List;
import java.util.Map;

public interface ReturnService {

    Map<String, Object> createReturnRequest(Map<String, Object> returnData);

    Map<String, Object> getReturnRequest(String returnId);

    List<Map<String, Object>> getReturnRequests(String status);

    Map<String, Object> inspectReturn(String returnId, Map<String, Object> inspectData);

    Map<String, Object> confirmReturn(String returnId);

    Map<String, Object> rejectReturn(String returnId, String reason);

    Map<String, Object> processRefund(String returnId);

    Map<String, Object> getReturnStatistics();

    List<Map<String, Object>> getReturnHistory(String customerId);
}