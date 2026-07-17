package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.request.InoutRecordQueryRequest;
import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.dto.response.InoutRecordDTO;
import com.example.smartvisionstock.dto.response.ReplayRecordDTO;
import com.example.smartvisionstock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/inout")
public class InoutRecordController {

    @Autowired
    private StockService stockService;

    @GetMapping("/records")
    public ApiResponse<Map<String, Object>> getInoutRecords(
            @RequestParam(required = false) String goodsName,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String operator,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        InoutRecordQueryRequest request = new InoutRecordQueryRequest();
        request.setGoodsName(goodsName);
        request.setType(type);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setOperator(operator);
        request.setPage(page);
        request.setSize(size);

        Page<InoutRecordDTO> recordPage = stockService.getInoutRecords(request);

        Map<String, Object> result = new HashMap<>();
        result.put("content", recordPage.getContent());
        result.put("totalElements", recordPage.getTotalElements());
        result.put("totalPages", recordPage.getTotalPages());
        result.put("currentPage", recordPage.getNumber());
        result.put("pageSize", recordPage.getSize());

        return ApiResponse.success(result);
    }

    @GetMapping("/replay/{recordId}")
    public ApiResponse<ReplayRecordDTO> getReplayRecord(@PathVariable Long recordId) {
        return ApiResponse.success(stockService.getReplayRecord(recordId));
    }
}
