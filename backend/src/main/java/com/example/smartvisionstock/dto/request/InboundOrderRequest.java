package com.example.smartvisionstock.dto.request;

import java.util.List;

public class InboundOrderRequest {
    private String type;
    private Long supplierId;
    private Long warehouseId;
    private String remark;
    private List<InboundOrderItemRequest> items;

    public InboundOrderRequest() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<InboundOrderItemRequest> getItems() { return items; }
    public void setItems(List<InboundOrderItemRequest> items) { this.items = items; }
}
