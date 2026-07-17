package com.example.smartvisionstock.dto.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class OutboundRequest {
    @NotNull(message = "货物实例ID不能为空")
    private Long goodsInstanceId;
    @NotNull(message = "出库数量不能为空")
    @Min(value = 1, message = "出库数量必须大于0")
    private Integer quantity;

    public OutboundRequest() {}

    public Long getGoodsInstanceId() { return goodsInstanceId; }
    public void setGoodsInstanceId(Long goodsInstanceId) { this.goodsInstanceId = goodsInstanceId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
