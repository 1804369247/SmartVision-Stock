package com.example.smartvisionstock.service;

import java.util.List;
import java.util.Map;

public interface PrintService {

    List<Map<String, Object>> getPrinters();

    Map<String, Object> printOrder(Long orderId, String type);

    Map<String, Object> printLabel(Map<String, Object> labelData);
}