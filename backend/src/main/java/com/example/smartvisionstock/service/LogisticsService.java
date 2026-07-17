package com.example.smartvisionstock.service;

import java.util.List;
import java.util.Map;

public interface LogisticsService {

    Map<String, Object> trackShipment(String trackingNo);

    Map<String, Object> createShipment(Map<String, Object> shipmentData);

    Map<String, Object> generateLabel(String orderId);

    Map<String, Object> getShippingCompanies();

    Map<String, Object> estimateShippingCost(String from, String to, double weight);

    List<Map<String, Object>> getShipmentsByOrder(Long orderId);

    Map<String, Object> updateShipmentStatus(String trackingNo, String status);

    Map<String, Object> getLogisticsStatistics();
}