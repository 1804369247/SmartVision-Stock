package com.example.smartvisionstock.dto.response;

import java.time.LocalDateTime;

public class ReplayRecordDTO {
    private Long id;
    private String orderNo;
    private String type;
    private String goodsName;
    private String batchNo;
    private Integer quantity;
    private LocalDateTime operateTime;
    private Long fromLocationId;
    private String fromLocationCode;
    private Double fromX;
    private Double fromY;
    private Double fromZ;
    private Long toLocationId;
    private String toLocationCode;
    private Double toX;
    private Double toY;
    private Double toZ;

    public ReplayRecordDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public LocalDateTime getOperateTime() { return operateTime; }
    public void setOperateTime(LocalDateTime operateTime) { this.operateTime = operateTime; }
    public Long getFromLocationId() { return fromLocationId; }
    public void setFromLocationId(Long fromLocationId) { this.fromLocationId = fromLocationId; }
    public String getFromLocationCode() { return fromLocationCode; }
    public void setFromLocationCode(String fromLocationCode) { this.fromLocationCode = fromLocationCode; }
    public Double getFromX() { return fromX; }
    public void setFromX(Double fromX) { this.fromX = fromX; }
    public Double getFromY() { return fromY; }
    public void setFromY(Double fromY) { this.fromY = fromY; }
    public Double getFromZ() { return fromZ; }
    public void setFromZ(Double fromZ) { this.fromZ = fromZ; }
    public Long getToLocationId() { return toLocationId; }
    public void setToLocationId(Long toLocationId) { this.toLocationId = toLocationId; }
    public String getToLocationCode() { return toLocationCode; }
    public void setToLocationCode(String toLocationCode) { this.toLocationCode = toLocationCode; }
    public Double getToX() { return toX; }
    public void setToX(Double toX) { this.toX = toX; }
    public Double getToY() { return toY; }
    public void setToY(Double toY) { this.toY = toY; }
    public Double getToZ() { return toZ; }
    public void setToZ(Double toZ) { this.toZ = toZ; }
}
