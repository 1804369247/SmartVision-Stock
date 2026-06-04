package com.example.smartvisionstock.dto.request;

public class OutboundOrderItemRequest {
    private Long goodsInstanceId;
    private Integer quantity;

    public OutboundOrderItemRequest() {}

    public Long getGoodsInstanceId() { return goodsInstanceId; }
    public void setGoodsInstanceId(Long goodsInstanceId) { this.goodsInstanceId = goodsInstanceId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
