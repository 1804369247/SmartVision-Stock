package com.example.smartvisionstock.dto.request;

import java.util.List;

public class OutboundOrderRequest {
    private String type;
    private Long customerId;
    private Long warehouseId;
    private String remark;
    private List<OutboundOrderItemRequest> items;

    public OutboundOrderRequest() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<OutboundOrderItemRequest> getItems() { return items; }
    public void setItems(List<OutboundOrderItemRequest> items) { this.items = items; }
}
