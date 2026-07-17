package com.example.smartvisionstock.service;

import java.util.List;
import java.util.Map;

public interface PickService {

    Map<String, Object> createWavePick(List<Long> orderIds);

    Map<String, Object> getWavePick(String waveId);

    List<Map<String, Object>> getPickTasks(String waveId);

    Map<String, Object> getPickTaskDetail(String taskId);

    Map<String, Object> completePickTask(String taskId, Map<String, Object> completionData);

    Map<String, Object> optimizePickPath(String waveId);

    List<Map<String, Object>> getActiveWaves();

    Map<String, Object> cancelWavePick(String waveId);

    Map<String, Object> getPickStatistics(String waveId);
}