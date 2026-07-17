package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.entity.GoodsInstance;
import com.example.smartvisionstock.entity.StockReservation;
import com.example.smartvisionstock.repository.GoodsInstanceRepository;
import com.example.smartvisionstock.service.StockReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservation")
public class StockReservationController {

    @Autowired
    private StockReservationService reservationService;

    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;

    /** 查询某货物实例的库存预占与可用量 */
    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> getAvailable(@RequestParam Long goodsInstanceId) {
        GoodsInstance instance = goodsInstanceRepository.findById(goodsInstanceId).orElse(null);
        int total = instance == null ? 0 : instance.getQuantity();
        int reserved = reservationService.getReservedQuantity(goodsInstanceId);
        int available = total - reserved;

        Map<String, Object> result = new HashMap<>();
        result.put("goodsInstanceId", goodsInstanceId);
        result.put("totalQuantity", total);
        result.put("reservedQuantity", reserved);
        result.put("availableQuantity", available);
        return ResponseEntity.ok(result);
    }

    /** 查询某出库单的全部预占明细 */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<StockReservation>> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(reservationService.findByOrderId(orderId));
    }
}
