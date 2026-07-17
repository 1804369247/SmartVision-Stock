package com.example.smartvisionstock.service.impl;

import org.springframework.stereotype.Service;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaSizeName;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 打印服务 —— 集成 Java Print Service API (javax.print)。
 * 自动发现系统安装的打印机，支持订单打印和标签打印。
 *
 * 注意：javax.print.PrintService 与本项目的 com.example.smartvisionstock.service.PrintService
 * 命名冲突，本类用全限定名 implements 项目接口，方法体内直接使用 javax.print.PrintService。
 */
@Service
public class PrintServiceImpl implements com.example.smartvisionstock.service.PrintService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<Map<String, Object>> getPrinters() {
        List<Map<String, Object>> printers = new ArrayList<>();

        try {
            // 尝试发现系统安装的真实打印机
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            for (int i = 0; i < services.length; i++) {
                PrintService ps = services[i];
                Map<String, Object> printer = new LinkedHashMap<>();
                printer.put("id", "printer-" + String.format("%03d", i + 1));
                printer.put("name", ps.getName());
                printer.put("type", classifyPrinter(ps));
                printer.put("status", "online");
                printer.put("location", "系统打印机");
                printer.put("isDefault", isDefaultPrinter(ps));
                printers.add(printer);
            }
        } catch (Exception e) {
            // javax.print 在某些无头环境不可用，返回空列表
        }

        // 如果没有发现打印机或API不可用，返回示例打印机列表（标明暂不可用）
        if (printers.isEmpty()) {
            Map<String, Object> p1 = new LinkedHashMap<>();
            p1.put("id", "printer-001");
            p1.put("name", "仓库标签打印机（待配置）");
            p1.put("type", "label");
            p1.put("status", "offline");
            p1.put("location", "A区-货架1");
            p1.put("note", "系统未检测到打印机，请连接打印机设备");
            printers.add(p1);

            Map<String, Object> p2 = new LinkedHashMap<>();
            p2.put("id", "printer-002");
            p2.put("name", "订单打印机（待配置）");
            p2.put("type", "document");
            p2.put("status", "offline");
            p2.put("location", "出库区");
            p2.put("note", "系统未检测到打印机，请连接打印机设备");
            printers.add(p2);
        }

        return printers;
    }

    @Override
    public Map<String, Object> printOrder(Long orderId, String type) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 将订单数据渲染为可打印内容
        String printContent = renderOrderContent(orderId, type);
        String jobId = "JOB-" + System.currentTimeMillis();

        // 尝试查找匹配的打印机
        PrintService printer = findSuitablePrinter("document");
        if (printer != null) {
            try {
                // 创建打印作业
                DocPrintJob printJob = printer.createPrintJob();
                DocFlavor flavor = DocFlavor.STRING.TEXT_PLAIN;
                Doc doc = new SimpleDoc(printContent, flavor, null);
                PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
                attrs.add(new Copies(1));
                attrs.add(MediaSizeName.ISO_A4);

                printJob.print(doc, attrs);

                result.put("code", 200);
                result.put("message", "订单已发送至打印机 " + printer.getName());
                result.put("printerId", getPrinterId(printer));
                result.put("printerName", printer.getName());
            } catch (PrintException e) {
                result.put("code", 400);
                result.put("message", "打印失败：" + e.getMessage());
                result.put("printContent", printContent);
            }
        } else {
            // 无打印机时返回打印内容预览
            result.put("code", 200);
            result.put("message", "打印内容已生成（未检测到打印机，请手动打印）");
            result.put("note", "系统未检测到可用打印机，已将打印内容保存");
            result.put("printContent", printContent);
        }

        result.put("orderId", orderId);
        result.put("type", type);
        result.put("jobId", jobId);
        result.put("printTime", LocalDateTime.now().format(DT_FMT));
        return result;
    }

    @Override
    public Map<String, Object> printLabel(Map<String, Object> labelData) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 将标签数据渲染为可打印内容
        String printContent = renderLabelContent(labelData);
        String jobId = "JOB-" + System.currentTimeMillis();
        int labelCount = labelData.containsKey("count") ? ((Number) labelData.get("count")).intValue() : 1;

        PrintService printer = findSuitablePrinter("label");
        if (printer != null) {
            try {
                DocPrintJob printJob = printer.createPrintJob();
                DocFlavor flavor = DocFlavor.STRING.TEXT_PLAIN;
                Doc doc = new SimpleDoc(printContent, flavor, null);
                PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
                attrs.add(new Copies(labelCount));

                printJob.print(doc, attrs);

                result.put("code", 200);
                result.put("message", "标签已发送至打印机 " + printer.getName());
                result.put("printerId", getPrinterId(printer));
                result.put("printerName", printer.getName());
            } catch (PrintException e) {
                result.put("code", 400);
                result.put("message", "打印失败：" + e.getMessage());
                result.put("printContent", printContent);
            }
        } else {
            result.put("code", 200);
            result.put("message", "标签内容已生成（未检测到打印机，请手动打印）");
            result.put("note", "系统未检测到可用标签打印机");
            result.put("printContent", printContent);
        }

        result.put("labelCount", labelCount);
        result.put("jobId", jobId);
        result.put("printTime", LocalDateTime.now().format(DT_FMT));

        // 附加标签信息
        if (labelData.containsKey("goodsName")) {
            result.put("goodsName", labelData.get("goodsName"));
        }
        if (labelData.containsKey("barcode")) {
            result.put("barcode", labelData.get("barcode"));
        }

        return result;
    }

    // ===== 内部工具方法 =====

    /**
     * 渲染订单打印内容
     */
    private String renderOrderContent(Long orderId, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("         SmartVision Stock 订单\n");
        sb.append("========================================\n");
        sb.append("订单编号: ").append(orderId).append("\n");
        sb.append("单据类型: ").append(type != null ? type : "订单").append("\n");
        sb.append("打印时间: ").append(LocalDateTime.now().format(DT_FMT)).append("\n");
        sb.append("----------------------------------------\n");
        sb.append("序号 | 商品名称         | 数量 | 库位\n");
        sb.append("----------------------------------------\n");
        sb.append("  1  | 待填充           |   -  | -\n");
        sb.append("----------------------------------------\n");
        sb.append("\n操作员签名: ________    日期: ________\n");
        sb.append("========================================\n");
        return sb.toString();
    }

    /**
     * 渲染标签打印内容
     */
    private String renderLabelContent(Map<String, Object> labelData) {
        StringBuilder sb = new StringBuilder();
        String goodsName = (String) labelData.getOrDefault("goodsName", "未指定");
        String barcode = (String) labelData.getOrDefault("barcode", "0000000000000");
        String location = (String) labelData.getOrDefault("location", "未分配");
        String batchNo = (String) labelData.getOrDefault("batchNo", "");

        sb.append("+------------------------------------+\n");
        sb.append("|  SmartVision Stock - 库存标签       |\n");
        sb.append("+------------------------------------+\n");
        sb.append("| 商品: ").append(padRight(goodsName, 28)).append("|\n");
        if (!batchNo.isEmpty()) {
            sb.append("| 批次: ").append(padRight(batchNo, 28)).append("|\n");
        }
        sb.append("| 库位: ").append(padRight(location, 28)).append("|\n");
        sb.append("| 条码: ").append(padRight(barcode, 28)).append("|\n");
        sb.append("| 日期: ").append(padRight(LocalDateTime.now().format(DT_FMT), 28)).append("|\n");
        sb.append("+------------------------------------+\n");

        // 生成条形码文本表示
        sb.append("||");
        for (char c : barcode.toCharArray()) {
            sb.append(c % 2 == 0 ? "██" : "  ");
        }
        sb.append("||\n");

        return sb.toString();
    }

    /**
     * 查找合适的打印机
     */
    private PrintService findSuitablePrinter(String type) {
        try {
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            if (services.length == 0) return null;

            // 返回默认打印机
            PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
            if (defaultService != null) return defaultService;

            // 返回第一个可用打印机
            return services[0];
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取打印机ID
     */
    private String getPrinterId(PrintService ps) {
        String name = ps.getName();
        if (name.contains("001")) return "printer-001";
        if (name.contains("002")) return "printer-002";
        return "printer-" + Math.abs(name.hashCode() % 1000);
    }

    /**
     * 分类打印机类型
     */
    private String classifyPrinter(PrintService ps) {
        String name = ps.getName().toLowerCase();
        if (name.contains("label") || name.contains("标签") || name.contains("条码") || name.contains("barcode")) {
            return "label";
        }
        if (name.contains("receipt") || name.contains("小票") || name.contains("pos")) {
            return "receipt";
        }
        return "document";
    }

    /**
     * 判断是否为默认打印机
     */
    private boolean isDefaultPrinter(PrintService ps) {
        try {
            PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
            return defaultService != null && defaultService.getName().equals(ps.getName());
        } catch (Exception e) {
            return false;
        }
    }

    private String padRight(String s, int length) {
        if (s == null) s = "";
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < length) {
            sb.append(' ');
        }
        if (sb.length() > length) {
            return sb.substring(0, length - 1) + "…";
        }
        return sb.toString();
    }
}
