package com.example.smartvisionstock.dto.response;

import java.time.LocalDateTime;

public class InoutRecordDTO {
    private Long id;
    private String orderNo;
    private String type;
    private String goodsName;
    private String goodsCode;
    private String batchNo;
    private Integer quantity;
    private String operator;
    private LocalDateTime operateTime;
    private Long goodsInstanceId;
    private Long fromLocationId;
    private String fromLocationCode;
    private Long toLocationId;
    private String toLocationCode;

    public InoutRecordDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
    public String getGoodsCode() { return goodsCode; }
    public void setGoodsCode(String goodsCode) { this.goodsCode = goodsCode; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getOperateTime() { return operateTime; }
    public void setOperateTime(LocalDateTime operateTime) { this.operateTime = operateTime; }
    public Long getGoodsInstanceId() { return goodsInstanceId; }
    public void setGoodsInstanceId(Long goodsInstanceId) { this.goodsInstanceId = goodsInstanceId; }
    public Long getFromLocationId() { return fromLocationId; }
    public void setFromLocationId(Long fromLocationId) { this.fromLocationId = fromLocationId; }
    public String getFromLocationCode() { return fromLocationCode; }
    public void setFromLocationCode(String fromLocationCode) { this.fromLocationCode = fromLocationCode; }
    public Long getToLocationId() { return toLocationId; }
    public void setToLocationId(Long toLocationId) { this.toLocationId = toLocationId; }
    public String getToLocationCode() { return toLocationCode; }
    public void setToLocationCode(String toLocationCode) { this.toLocationCode = toLocationCode; }
}
