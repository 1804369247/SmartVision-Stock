package com.example.smartvisionstock.dto.response;

import java.time.LocalDateTime;

public class LocationInfoDTO {
    private Long id;
    private String locationCode;
    private String area;
    private Integer status;
    private Double xCoord;
    private Double yCoord;
    private Double zCoord;
    private Long currentGoodsInstanceId;
    private String goodsName;
    private String goodsCode;
    private String batchNo;
    private Integer quantity;
    private String attribute;
    private String description;
    private String supplier;
    private LocalDateTime expiryDate;
    private LocalDateTime inTime;
    private Boolean frozen;
    private String frozenReason;
    private String goodsCategory;
    private String storageRule;

    public LocationInfoDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Double getXCoord() { return xCoord; }
    public void setXCoord(Double xCoord) { this.xCoord = xCoord; }
    public Double getYCoord() { return yCoord; }
    public void setYCoord(Double yCoord) { this.yCoord = yCoord; }
    public Double getZCoord() { return zCoord; }
    public void setZCoord(Double zCoord) { this.zCoord = zCoord; }
    public Long getCurrentGoodsInstanceId() { return currentGoodsInstanceId; }
    public void setCurrentGoodsInstanceId(Long currentGoodsInstanceId) { this.currentGoodsInstanceId = currentGoodsInstanceId; }
    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
    public String getGoodsCode() { return goodsCode; }
    public void setGoodsCode(String goodsCode) { this.goodsCode = goodsCode; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getAttribute() { return attribute; }
    public void setAttribute(String attribute) { this.attribute = attribute; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    public LocalDateTime getInTime() { return inTime; }
    public void setInTime(LocalDateTime inTime) { this.inTime = inTime; }
    public Boolean getFrozen() { return frozen; }
    public void setFrozen(Boolean frozen) { this.frozen = frozen; }
    public String getFrozenReason() { return frozenReason; }
    public void setFrozenReason(String frozenReason) { this.frozenReason = frozenReason; }
    public String getGoodsCategory() { return goodsCategory; }
    public void setGoodsCategory(String goodsCategory) { this.goodsCategory = goodsCategory; }
    public String getStorageRule() { return storageRule; }
    public void setStorageRule(String storageRule) { this.storageRule = storageRule; }
}
