package com.example.smartvisionstock.dto.request;

import java.time.LocalDateTime;

public class InoutRecordQueryRequest {
    private String goodsName;
    private String type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String operator;
    private Integer page;
    private Integer size;

    public InoutRecordQueryRequest() {}

    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
