package com.example.smartvisionstock.dto.response;

import java.time.LocalDateTime;

public class InboundOrderItemDTO {
    private Long id;
    private Long goodsId;
    private String goodsName;
    private String goodsCode;
    private String batchNo;
    private Integer quantity;
    private Long locationId;
    private String locationCode;
    private LocalDateTime expiryDate;

    public InboundOrderItemDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGoodsId() { return goodsId; }
    public void setGoodsId(Long goodsId) { this.goodsId = goodsId; }
    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
    public String getGoodsCode() { return goodsCode; }
    public void setGoodsCode(String goodsCode) { this.goodsCode = goodsCode; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
}
