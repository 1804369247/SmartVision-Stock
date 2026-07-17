package com.example.smartvisionstock.dto.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class InboundOrderItemRequest {
    @NotNull(message = "货物ID不能为空")
    private Long goodsId;
    private String batchNo;
    @NotNull(message = "入库数量不能为空")
    @Min(value = 1, message = "入库数量必须大于0")
    private Integer quantity;
    private Long locationId;
    private String expiryDate;

    public InboundOrderItemRequest() {}

    public Long getGoodsId() { return goodsId; }
    public void setGoodsId(Long goodsId) { this.goodsId = goodsId; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
}
