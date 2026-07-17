package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.InoutRecordDTO;
import com.example.smartvisionstock.dto.response.LocationInfoDTO;
import com.example.smartvisionstock.entity.Goods;
import com.example.smartvisionstock.entity.GoodsInstance;
import com.example.smartvisionstock.entity.InoutRecord;
import com.example.smartvisionstock.entity.StorageLocation;
import com.example.smartvisionstock.repository.GoodsInstanceRepository;
import com.example.smartvisionstock.repository.GoodsRepository;
import com.example.smartvisionstock.repository.InoutRecordRepository;
import com.example.smartvisionstock.repository.StorageLocationRepository;
import com.example.smartvisionstock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import com.example.smartvisionstock.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private StockService stockService;

    @Autowired
    private StorageLocationRepository storageLocationRepository;

    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private InoutRecordRepository inoutRecordRepository;

    /**
     * 库存台账报表
     */
    @GetMapping("/inventory")
    public ApiResponse<Map<String, Object>> getInventoryReport(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String attribute,
            @RequestParam(required = false) String goodsName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        List<LocationInfoDTO> allLocations = stockService.getAllLocations();

        // 过滤
        List<LocationInfoDTO> filtered = allLocations.stream().filter(loc -> {
            if (area != null && !area.isEmpty() && !area.equals(loc.getArea())) return false;
            if (attribute != null && !attribute.isEmpty() && !attribute.equals(loc.getAttribute())) return false;
            if (status != null && !status.isEmpty()) {
                if ("frozen".equals(status)) {
                    if (!Boolean.TRUE.equals(loc.getFrozen())) return false;
                } else {
                    try {
                        if (loc.getStatus() != Integer.parseInt(status)) return false;
                    } catch (NumberFormatException e) {
                        log.warn("无效的状态值: {}", status);
                        return false;
                    }
                }
            }
            if (goodsName != null && !goodsName.isEmpty()) {
                if (loc.getGoodsName() == null || !loc.getGoodsName().contains(goodsName)) return false;
            }
            return true;
        }).collect(Collectors.toList());

        // 分页
        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        List<LocationInfoDTO> paged = start < filtered.size() ? filtered.subList(start, end) : Collections.emptyList();

        Map<String, Object> result = new HashMap<>();
        result.put("data", paged);
        result.put("total", filtered.size());
        result.put("page", page);
        result.put("size", size);
        return ApiResponse.success(result);
    }

    /**
     * 出入库流水报表
     */
    @GetMapping("/inout-records")
    public ApiResponse<Map<String, Object>> getInoutFlowReport(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String goodsName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        var request = new com.example.smartvisionstock.dto.request.InoutRecordQueryRequest();
        request.setType(type);
        request.setGoodsName(goodsName);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setPage(page);
        request.setSize(size);

        Page<InoutRecordDTO> recordPage = stockService.getInoutRecords(request);

        Map<String, Object> result = new HashMap<>();
        result.put("data", recordPage.getContent());
        result.put("total", recordPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        return ApiResponse.success(result);
    }

    /**
     * 库位利用率报表
     */
    @GetMapping("/utilization")
    public ApiResponse<Map<String, Object>> getUtilizationReport(
            @RequestParam(required = false) String area) {

        List<LocationInfoDTO> locations = stockService.getAllLocations();
        if (area != null && !area.isEmpty()) {
            locations = locations.stream().filter(l -> area.equals(l.getArea())).collect(Collectors.toList());
        }

        // 按分区统计
        Map<String, List<LocationInfoDTO>> byArea = locations.stream()
                .collect(Collectors.groupingBy(l -> l.getArea() != null ? l.getArea() : "未知"));

        List<Map<String, Object>> areaStats = new ArrayList<>();
        for (Map.Entry<String, List<LocationInfoDTO>> entry : new TreeMap<>(byArea).entrySet()) {
            List<LocationInfoDTO> locs = entry.getValue();
            int total = locs.size();
            int used = (int) locs.stream().filter(l -> l.getStatus() != null && l.getStatus() != 0).count();
            int empty = total - used;
            int frozen = (int) locs.stream().filter(l -> Boolean.TRUE.equals(l.getFrozen())).count();
            int totalQty = locs.stream().mapToInt(l -> l.getQuantity() != null ? l.getQuantity() : 0).sum();

            Map<String, Object> stat = new HashMap<>();
            stat.put("area", entry.getKey() + "区");
            stat.put("totalLocations", total);
            stat.put("usedLocations", used);
            stat.put("emptyLocations", empty);
            stat.put("frozenLocations", frozen);
            stat.put("utilizationRate", total > 0 ? String.format("%.1f", (double) used / total * 100) : "0.0");
            stat.put("totalQuantity", totalQty);
            areaStats.add(stat);
        }

        // 全局统计
        int grandTotal = locations.size();
        int grandUsed = (int) locations.stream().filter(l -> l.getStatus() != null && l.getStatus() != 0).count();
        int grandWarning = (int) locations.stream().filter(l -> l.getStatus() != null && l.getStatus() == 2).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalLocations", grandTotal);
        summary.put("usedLocations", grandUsed);
        summary.put("emptyLocations", grandTotal - grandUsed);
        summary.put("utilizationRate", grandTotal > 0 ? String.format("%.1f", (double) grandUsed / grandTotal * 100) : "0.0");
        summary.put("warningCount", grandWarning);

        Map<String, Object> result = new HashMap<>();
        result.put("data", areaStats);
        result.put("summary", summary);
        return ApiResponse.success(result);
    }

    /**
     * 预警报表
     */
    @GetMapping("/alerts")
    public ApiResponse<Map<String, Object>> getAlertsReport() {
        List<LocationInfoDTO> locations = stockService.getAllLocations();

        List<Map<String, Object>> alerts = new ArrayList<>();

        for (LocationInfoDTO loc : locations) {
            // 库存预警
            if (loc.getStatus() != null && loc.getStatus() == 2 && loc.getQuantity() != null) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("type", "库存预警");
                alert.put("locationCode", loc.getLocationCode());
                alert.put("goodsName", loc.getGoodsName() != null ? loc.getGoodsName() : "-");
                alert.put("goodsCode", loc.getGoodsCode() != null ? loc.getGoodsCode() : "-");
                alert.put("quantity", loc.getQuantity());
                alert.put("warningThreshold", "低于安全库存");
                alert.put("expiryDate", "-");
                alert.put("level", "warning");
                alerts.add(alert);
            }

            // 保质期预警（7天内过期）
            if (loc.getExpiryDate() != null) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime warnTime = now.plusDays(7);
                if (loc.getExpiryDate().isBefore(warnTime) && loc.getExpiryDate().isAfter(now.minusDays(1))) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("type", "保质期预警");
                    alert.put("locationCode", loc.getLocationCode());
                    alert.put("goodsName", loc.getGoodsName() != null ? loc.getGoodsName() : "-");
                    alert.put("goodsCode", loc.getGoodsCode() != null ? loc.getGoodsCode() : "-");
                    alert.put("quantity", loc.getQuantity());
                    alert.put("warningThreshold", "-");
                    alert.put("expiryDate", loc.getExpiryDate().toString());
                    alert.put("level", "danger");
                    alerts.add(alert);
                }
                // 已过期
                if (loc.getExpiryDate().isBefore(now)) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("type", "已过期");
                    alert.put("locationCode", loc.getLocationCode());
                    alert.put("goodsName", loc.getGoodsName() != null ? loc.getGoodsName() : "-");
                    alert.put("goodsCode", loc.getGoodsCode() != null ? loc.getGoodsCode() : "-");
                    alert.put("quantity", loc.getQuantity());
                    alert.put("warningThreshold", "-");
                    alert.put("expiryDate", loc.getExpiryDate().toString());
                    alert.put("level", "danger");
                    alerts.add(alert);
                }
            }

            // 冻结预警
            if (Boolean.TRUE.equals(loc.getFrozen())) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("type", "库存冻结");
                alert.put("locationCode", loc.getLocationCode());
                alert.put("goodsName", loc.getGoodsName() != null ? loc.getGoodsName() : "-");
                alert.put("goodsCode", loc.getGoodsCode() != null ? loc.getGoodsCode() : "-");
                alert.put("quantity", loc.getQuantity());
                alert.put("warningThreshold", "-");
                alert.put("expiryDate", "-");
                alert.put("level", "warning");
                alert.put("frozenReason", loc.getFrozenReason() != null ? loc.getFrozenReason() : "未知原因");
                alerts.add(alert);
            }
        }

        // 统计摘要
        long stockWarning = alerts.stream().filter(a -> "库存预警".equals(a.get("type"))).count();
        long expiryWarning = alerts.stream().filter(a -> "保质期预警".equals(a.get("type")) || "已过期".equals(a.get("type"))).count();
        long frozenWarning = alerts.stream().filter(a -> "库存冻结".equals(a.get("type"))).count();

        Map<String, Object> alertSummary = new HashMap<>();
        alertSummary.put("totalAlerts", alerts.size());
        alertSummary.put("stockWarningCount", stockWarning);
        alertSummary.put("expiryWarningCount", expiryWarning);
        alertSummary.put("frozenWarningCount", frozenWarning);

        Map<String, Object> result = new HashMap<>();
        result.put("data", alerts);
        result.put("summary", alertSummary);
        return ApiResponse.success(result);
    }

    /**
     * KPI 统计数据
     */
    @GetMapping("/kpi")
    public ApiResponse<Map<String, Object>> getKpi() {
        List<LocationInfoDTO> locations = stockService.getAllLocations();

        int totalLocations = locations.size();
        int usedLocations = (int) locations.stream().filter(l -> l.getStatus() != null && l.getStatus() != 0).count();
        int emptyLocations = totalLocations - usedLocations;
        int warningCount = (int) locations.stream().filter(l -> l.getStatus() != null && l.getStatus() == 2).count();
        int errorCount = (int) locations.stream().filter(l -> l.getStatus() != null && l.getStatus() == 3).count();
        int frozenCount = (int) locations.stream().filter(l -> Boolean.TRUE.equals(l.getFrozen())).count();

        // 在库货物种类数
        Set<String> goodsNames = locations.stream()
                .filter(l -> l.getGoodsName() != null)
                .map(LocationInfoDTO::getGoodsName)
                .collect(Collectors.toSet());

        // 总库存量
        int totalQuantity = locations.stream().mapToInt(l -> l.getQuantity() != null ? l.getQuantity() : 0).sum();

        // 今日出入库次数（从 InoutRecord 表）
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = todayStart.plusDays(1);
        List<InoutRecord> todayRecords = inoutRecordRepository.findByOperateTimeBetween(todayStart, todayEnd);
        long todayInbound = todayRecords.stream().filter(r -> "IN".equals(r.getType())).count();
        long todayOutbound = todayRecords.stream().filter(r -> "OUT".equals(r.getType())).count();
        long todayMove = todayRecords.stream().filter(r -> "MOVE".equals(r.getType())).count();

        // 货品总数
        long totalGoods = goodsRepository.count();

        // 计算库位利用率
        double utilizationRate = totalLocations > 0 ? (double) usedLocations / totalLocations * 100 : 0;
        String utilizationText = String.format("%.1f%%", utilizationRate);

        Map<String, Object> kpiData = new HashMap<>();
        kpiData.put("totalLocations", totalLocations);
        kpiData.put("usedLocations", usedLocations);
        kpiData.put("emptyLocations", emptyLocations);
        kpiData.put("utilizationRate", utilizationText);
        kpiData.put("warningCount", warningCount);
        kpiData.put("errorCount", errorCount);
        kpiData.put("frozenCount", frozenCount);
        kpiData.put("totalGoodsKinds", goodsNames.size());
        kpiData.put("totalQuantity", totalQuantity);
        kpiData.put("todayInbound", todayInbound);
        kpiData.put("todayOutbound", todayOutbound);
        kpiData.put("todayMove", todayMove);
        kpiData.put("totalGoods", totalGoods);

        return ApiResponse.success(kpiData);
    }
}