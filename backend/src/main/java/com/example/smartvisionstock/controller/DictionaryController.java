package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dictionary")
public class DictionaryController {

    @GetMapping("/goods-category")
    public ApiResponse<List<Map<String, String>>> getGoodsCategory() {
        List<Map<String, String>> categories = Arrays.asList(
                createDictItem("电子产品", "电子产品"),
                createDictItem("服装鞋帽", "服装鞋帽"),
                createDictItem("食品饮料", "食品饮料"),
                createDictItem("日用百货", "日用百货"),
                createDictItem("办公用品", "办公用品"),
                createDictItem("医疗器械", "医疗器械"),
                createDictItem("化工原料", "化工原料"),
                createDictItem("五金建材", "五金建材")
        );
        return ApiResponse.success(categories);
    }

    @GetMapping("/location-attribute")
    public ApiResponse<List<Map<String, String>>> getLocationAttribute() {
        List<Map<String, String>> attributes = Arrays.asList(
                createDictItem("NORMAL", "普通"),
                createDictItem("COLD", "冷藏"),
                createDictItem("DANGEROUS", "危险品"),
                createDictItem("VALUABLE", "高价值")
        );
        return ApiResponse.success(attributes);
    }

    @GetMapping("/location-status")
    public ApiResponse<List<Map<String, String>>> getLocationStatus() {
        List<Map<String, String>> statusList = Arrays.asList(
                createDictItem("0", "空闲"),
                createDictItem("1", "正常"),
                createDictItem("2", "预警"),
                createDictItem("3", "异常")
        );
        return ApiResponse.success(statusList);
    }

    @GetMapping("/order-status")
    public ApiResponse<List<Map<String, String>>> getOrderStatus() {
        List<Map<String, String>> statusList = Arrays.asList(
                createDictItem("DRAFT", "草稿"),
                createDictItem("AUDITING", "审核中"),
                createDictItem("COMPLETED", "已完成"),
                createDictItem("CANCELLED", "已取消")
        );
        return ApiResponse.success(statusList);
    }

    @GetMapping("/stock-count-status")
    public ApiResponse<List<Map<String, String>>> getStockCountStatus() {
        List<Map<String, String>> statusList = Arrays.asList(
                createDictItem("0", "待盘点"),
                createDictItem("1", "盘点中"),
                createDictItem("2", "已完成"),
                createDictItem("3", "已确认")
        );
        return ApiResponse.success(statusList);
    }

    @GetMapping("/storage-rule")
    public ApiResponse<List<Map<String, String>>> getStorageRule() {
        List<Map<String, String>> rules = Arrays.asList(
                createDictItem("NORMAL", "常温"),
                createDictItem("COLD", "冷藏"),
                createDictItem("FREEZE", "冷冻"),
                createDictItem("HUMIDITY", "控湿"),
                createDictItem("DUSTPROOF", "防尘"),
                createDictItem("ANTISTATIC", "防静电")
        );
        return ApiResponse.success(rules);
    }

    @GetMapping("/notification-type")
    public ApiResponse<List<Map<String, String>>> getNotificationType() {
        List<Map<String, String>> types = Arrays.asList(
                createDictItem("EXPIRY_WARNING", "效期预警"),
                createDictItem("LOW_STOCK", "低库存预警"),
                createDictItem("ORDER_APPROVAL", "订单审核"),
                createDictItem("SYSTEM", "系统通知"),
                createDictItem("STOCK_COUNT", "盘点通知")
        );
        return ApiResponse.success(types);
    }

    @GetMapping("/all")
    public ApiResponse<Map<String, List<Map<String, String>>>> getAllDictionaries() {
        Map<String, List<Map<String, String>>> all = new HashMap<>();
        all.put("goodsCategory", getGoodsCategory().getData());
        all.put("locationAttribute", getLocationAttribute().getData());
        all.put("locationStatus", getLocationStatus().getData());
        all.put("orderStatus", getOrderStatus().getData());
        all.put("stockCountStatus", getStockCountStatus().getData());
        all.put("storageRule", getStorageRule().getData());
        all.put("notificationType", getNotificationType().getData());
        return ApiResponse.success(all);
    }

    private Map<String, String> createDictItem(String value, String label) {
        Map<String, String> item = new HashMap<>();
        item.put("value", value);
        item.put("label", label);
        return item;
    }
}