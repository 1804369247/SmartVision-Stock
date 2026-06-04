package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.service.StockService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/export")
@CrossOrigin(origins = "*")
public class ExportController {

    @Autowired
    private StockService stockService;

    @GetMapping("/inventory")
    public void exportInventory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String attribute,
            @RequestParam(required = false) String area,
            HttpServletResponse response) throws IOException {
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("库存报表");

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        String[] headers = {"库位编码", "分区", "库位属性", "状态", "货物名称", "货物编码", 
                "批次号", "数量", "供应商", "入库时间", "过期时间", "冻结状态", "冻结原因", "存储规则"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 15 * 256);
        }

        List<com.example.smartvisionstock.dto.response.LocationInfoDTO> locations = stockService.getAllLocations();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        int rowNum = 1;
        if (locations != null) {
            for (var loc : locations) {
            if (status != null && !status.isEmpty()) {
                if ("frozen".equals(status) && !Boolean.TRUE.equals(loc.getFrozen())) continue;
                if (!"frozen".equals(status)) {
                    try {
                        int statusInt = Integer.parseInt(status);
                        if (loc.getStatus() != statusInt) continue;
                    } catch (NumberFormatException e) {}
                }
            }
            if (attribute != null && !attribute.isEmpty() && !attribute.equals(loc.getAttribute())) continue;
            if (area != null && !area.isEmpty() && !area.equals(loc.getArea())) continue;

            Row row = sheet.createRow(rowNum++);
            
            String statusText;
            switch (loc.getStatus()) {
                case 0: statusText = "空闲"; break;
                case 1: statusText = "正常"; break;
                case 2: statusText = "预警"; break;
                case 3: statusText = "异常"; break;
                default: statusText = "未知"; break;
            }
            
            String attributeText;
            if (loc.getAttribute() != null) {
                switch (loc.getAttribute()) {
                    case "NORMAL": attributeText = "普通"; break;
                    case "COLD": attributeText = "冷藏"; break;
                    case "DANGEROUS": attributeText = "危险品"; break;
                    case "VALUABLE": attributeText = "高价值"; break;
                    default: attributeText = loc.getAttribute(); break;
                }
            } else {
                attributeText = "普通";
            }

            int col = 0;
            createCell(row, col++, loc.getLocationCode(), dataStyle);
            createCell(row, col++, loc.getArea() != null ? loc.getArea() + "区" : "", dataStyle);
            createCell(row, col++, attributeText, dataStyle);
            createCell(row, col++, statusText, dataStyle);
            createCell(row, col++, loc.getGoodsName() != null ? loc.getGoodsName() : "-", dataStyle);
            createCell(row, col++, loc.getGoodsCode() != null ? loc.getGoodsCode() : "-", dataStyle);
            createCell(row, col++, loc.getBatchNo() != null ? loc.getBatchNo() : "-", dataStyle);
            createCell(row, col++, loc.getQuantity() != null ? loc.getQuantity().toString() : "0", dataStyle);
            createCell(row, col++, loc.getSupplier() != null ? loc.getSupplier() : "-", dataStyle);
            createCell(row, col++, loc.getInTime() != null ? loc.getInTime().format(formatter) : "-", dataStyle);
            createCell(row, col++, loc.getExpiryDate() != null ? loc.getExpiryDate().format(formatter) : "-", dataStyle);
            createCell(row, col++, Boolean.TRUE.equals(loc.getFrozen()) ? "已冻结" : "正常", dataStyle);
            createCell(row, col++, loc.getFrozenReason() != null ? loc.getFrozenReason() : "-", dataStyle);
            createCell(row, col++, loc.getStorageRule() != null ? loc.getStorageRule() : "-", dataStyle);
            }
        }

        setResponseHeader(response, workbook, "库存报表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
    }

    @GetMapping("/inout-records")
    public void exportInoutRecords(
            @RequestParam(required = false) String goodsName,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String operator,
            HttpServletResponse response) throws IOException {
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("出入库记录");

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        String[] headers = {"单号", "类型", "货物名称", "货物编码", "批次号", "数量", 
                "源库位", "目标库位", "操作人", "操作时间"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 18 * 256);
        }

        var request = new com.example.smartvisionstock.dto.request.InoutRecordQueryRequest();
        request.setGoodsName(goodsName);
        request.setType(type);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setOperator(operator);
        request.setPage(0);
        request.setSize(10000);

        var recordPage = stockService.getInoutRecords(request);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        int rowNum = 1;
        for (var record : recordPage.getContent()) {
            Row row = sheet.createRow(rowNum++);
            
            String typeText = "in".equals(record.getType()) ? "入库" : 
                             "out".equals(record.getType()) ? "出库" : "移库";
            
            int col = 0;
            createCell(row, col++, record.getOrderNo(), dataStyle);
            createCell(row, col++, typeText, dataStyle);
            createCell(row, col++, record.getGoodsName() != null ? record.getGoodsName() : "-", dataStyle);
            createCell(row, col++, record.getGoodsCode() != null ? record.getGoodsCode() : "-", dataStyle);
            createCell(row, col++, record.getBatchNo() != null ? record.getBatchNo() : "-", dataStyle);
            createCell(row, col++, record.getQuantity() != null ? record.getQuantity().toString() : "0", dataStyle);
            createCell(row, col++, record.getFromLocationCode() != null ? record.getFromLocationCode() : "-", dataStyle);
            createCell(row, col++, record.getToLocationCode() != null ? record.getToLocationCode() : "-", dataStyle);
            createCell(row, col++, record.getOperator() != null ? record.getOperator() : "-", dataStyle);
            createCell(row, col++, record.getOperateTime() != null ? record.getOperateTime().format(formatter) : "-", dataStyle);
        }

        setResponseHeader(response, workbook, "出入库记录_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
    }

    @GetMapping("/statistics")
    public void exportStatistics(HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        
        var locations = stockService.getAllLocations();
        
        Sheet summarySheet = workbook.createSheet("统计概览");
        createStatisticsSheet(summarySheet, locations);

        Sheet areaSheet = workbook.createSheet("分区统计");
        createAreaStatistics(areaSheet, locations);

        Sheet statusSheet = workbook.createSheet("状态统计");
        createStatusStatistics(statusSheet, locations);

        setResponseHeader(response, workbook, "仓库统计报表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
    }

    private void createStatisticsSheet(Sheet sheet, java.util.List<com.example.smartvisionstock.dto.response.LocationInfoDTO> locations) {
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        CellStyle dataStyle = sheet.getWorkbook().createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.CENTER);

        int totalLocations = locations.size();
        int usedLocations = (int) locations.stream().filter(l -> l.getStatus() != 0).count();
        int emptyLocations = totalLocations - usedLocations;
        int normalCount = (int) locations.stream().filter(l -> l.getStatus() == 1).count();
        int warningCount = (int) locations.stream().filter(l -> l.getStatus() == 2).count();
        int errorCount = (int) locations.stream().filter(l -> l.getStatus() == 3).count();
        int frozenCount = (int) locations.stream().filter(l -> Boolean.TRUE.equals(l.getFrozen())).count();
        int totalQuantity = locations.stream().mapToInt(l -> l.getQuantity() != null ? l.getQuantity() : 0).sum();

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("仓库统计概览");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 1));

        String[][] data = {
            {"指标", "数值"},
            {"库位总数", String.valueOf(totalLocations)},
            {"已用库位", String.valueOf(usedLocations)},
            {"空闲库位", String.valueOf(emptyLocations)},
            {"库位利用率", String.format("%.1f%%", (double) usedLocations / totalLocations * 100)},
            {"正常库位", String.valueOf(normalCount)},
            {"预警库位", String.valueOf(warningCount)},
            {"异常库位", String.valueOf(errorCount)},
            {"冻结库位", String.valueOf(frozenCount)},
            {"库存总量", String.valueOf(totalQuantity)}
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 2);
            Cell c1 = row.createCell(0);
            c1.setCellValue(data[i][0]);
            c1.setCellStyle(i == 0 ? headerStyle : dataStyle);
            Cell c2 = row.createCell(1);
            c2.setCellValue(data[i][1]);
            c2.setCellStyle(i == 0 ? headerStyle : dataStyle);
        }

        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 15 * 256);
    }

    private void createAreaStatistics(Sheet sheet, java.util.List<com.example.smartvisionstock.dto.response.LocationInfoDTO> locations) {
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        CellStyle dataStyle = sheet.getWorkbook().createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.CENTER);

        String[] headers = {"分区", "库位数", "已用", "空闲", "利用率", "库存量"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 12 * 256);
        }

        java.util.Map<String, int[]> areaStats = new java.util.TreeMap<>();
        for (var loc : locations) {
            String area = loc.getArea() != null ? loc.getArea() : "未知";
            areaStats.computeIfAbsent(area, k -> new int[4]);
            int[] stats = areaStats.get(area);
            stats[0]++;
            if (loc.getStatus() != 0) stats[1]++;
            else stats[2]++;
            stats[3] += loc.getQuantity() != null ? loc.getQuantity() : 0;
        }

        int rowNum = 1;
        for (var entry : areaStats.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            int[] stats = entry.getValue();
            createCell(row, 0, entry.getKey() + "区", dataStyle);
            createCell(row, 1, String.valueOf(stats[0]), dataStyle);
            createCell(row, 2, String.valueOf(stats[1]), dataStyle);
            createCell(row, 3, String.valueOf(stats[2]), dataStyle);
            createCell(row, 4, String.format("%.1f%%", (double) stats[1] / stats[0] * 100), dataStyle);
            createCell(row, 5, String.valueOf(stats[3]), dataStyle);
        }
    }

    private void createStatusStatistics(Sheet sheet, java.util.List<com.example.smartvisionstock.dto.response.LocationInfoDTO> locations) {
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        CellStyle dataStyle = sheet.getWorkbook().createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.CENTER);

        String[] headers = {"状态", "库位数", "占比"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 15 * 256);
        }

        int total = locations.size();
        int normal = (int) locations.stream().filter(l -> l.getStatus() == 1).count();
        int warning = (int) locations.stream().filter(l -> l.getStatus() == 2).count();
        int error = (int) locations.stream().filter(l -> l.getStatus() == 3).count();
        int empty = total - normal - warning - error;

        String[][] statusData = {
            {"正常", String.valueOf(normal), String.format("%.1f%%", (double) normal / total * 100)},
            {"预警", String.valueOf(warning), String.format("%.1f%%", (double) warning / total * 100)},
            {"异常", String.valueOf(error), String.format("%.1f%%", (double) error / total * 100)},
            {"空闲", String.valueOf(empty), String.format("%.1f%%", (double) empty / total * 100)}
        };

        for (int i = 0; i < statusData.length; i++) {
            Row row = sheet.createRow(i + 1);
            createCell(row, 0, statusData[i][0], dataStyle);
            createCell(row, 1, statusData[i][1], dataStyle);
            createCell(row, 2, statusData[i][2], dataStyle);
        }
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void setResponseHeader(HttpServletResponse response, Workbook workbook, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}