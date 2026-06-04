package com.example.smartvisionstock.dto.request;

public class OutboundRequest {
    private Long goodsInstanceId;
    private Integer quantity;

    public OutboundRequest() {}

    public Long getGoodsInstanceId() { return goodsInstanceId; }
    public void setGoodsInstanceId(Long goodsInstanceId) { this.goodsInstanceId = goodsInstanceId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
