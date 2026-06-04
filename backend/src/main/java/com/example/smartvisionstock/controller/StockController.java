package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.request.InboundRequest;
import com.example.smartvisionstock.dto.request.MoveRequest;
import com.example.smartvisionstock.dto.request.OutboundRequest;
import com.example.smartvisionstock.dto.response.LocationInfoDTO;
import com.example.smartvisionstock.entity.Goods;
import com.example.smartvisionstock.entity.GoodsInstance;
import com.example.smartvisionstock.repository.GoodsInstanceRepository;
import com.example.smartvisionstock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class StockController {

    @Autowired
    private StockService stockService;

    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;

    @GetMapping("/goods")
    public List<Goods> getAllGoods() {
        return stockService.getAllGoods();
    }

    @GetMapping("/locations")
    public List<LocationInfoDTO> getAllLocations() {
        return stockService.getAllLocations();
    }

    @GetMapping("/instances")
    public List<GoodsInstance> getAllInstances() {
        return goodsInstanceRepository.findAll();
    }

    @PostMapping("/inbound")
    public Map<String, Object> inbound(@RequestBody InboundRequest request) {
        return stockService.inbound(request.getGoodsId(), request.getBatchNo(), request.getQuantity(), request.getLocationId());
    }

    @PostMapping("/outbound")
    public Map<String, Object> outbound(@RequestBody OutboundRequest request) {
        return stockService.outbound(request.getGoodsInstanceId(), request.getQuantity());
    }

    @PostMapping("/move")
    public Map<String, Object> move(@RequestBody MoveRequest request) {
        return stockService.move(request.getGoodsInstanceId(), request.getTargetLocationId());
    }
}
