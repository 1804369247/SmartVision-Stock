package com.example.smartvisionstock.service;

import java.util.List;
import java.util.Map;

public interface LocationService {

    Map<String, Object> allocateLocation(String sku, int quantity);

    List<Map<String, Object>> suggestLocations(String sku);

    Map<String, Object> getHotZoneAnalysis();

    Map<String, Object> classifyProductABC(String sku);

    List<Map<String, Object>> getABCDistribution();

    Map<String, Object> optimizeLocationLayout();

    Map<String, Object> getLocationUtilization(String zone);

    List<Map<String, Object>> getEmptyLocations();

    Map<String, Object> releaseLocation(String locationCode);
}