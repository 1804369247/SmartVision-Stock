package com.example.smartvisionstock.dto.request;

import javax.validation.constraints.NotNull;

public class MoveRequest {
    @NotNull(message = "货物实例ID不能为空")
    private Long goodsInstanceId;
    @NotNull(message = "目标库位ID不能为空")
    private Long targetLocationId;

    public Long getGoodsInstanceId() {
        return goodsInstanceId;
    }

    public void setGoodsInstanceId(Long goodsInstanceId) {
        this.goodsInstanceId = goodsInstanceId;
    }

    public Long getTargetLocationId() {
        return targetLocationId;
    }

    public void setTargetLocationId(Long targetLocationId) {
        this.targetLocationId = targetLocationId;
    }
}
