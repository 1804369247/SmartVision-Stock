package com.example.smartvisionstock.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class OutboundOrderDTO {
    private Long id;
    private String orderNo;
    private String type;
    private String typeText;
    private Long customerId;
    private String customerName;
    private Long warehouseId;
    private String warehouseName;
    private String status;
    private String statusText;
    private Integer totalQuantity;
    private LocalDateTime createTime;
    private LocalDateTime auditTime;
    private LocalDateTime pickTime;
    private LocalDateTime confirmTime;
    private String remark;
    private List<OutboundOrderItemDTO> items;

    public OutboundOrderDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTypeText() { return typeText; }
    public void setTypeText(String typeText) { this.typeText = typeText; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getAuditTime() { return auditTime; }
    public void setAuditTime(LocalDateTime auditTime) { this.auditTime = auditTime; }
    public LocalDateTime getPickTime() { return pickTime; }
    public void setPickTime(LocalDateTime pickTime) { this.pickTime = pickTime; }
    public LocalDateTime getConfirmTime() { return confirmTime; }
    public void setConfirmTime(LocalDateTime confirmTime) { this.confirmTime = confirmTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<OutboundOrderItemDTO> getItems() { return items; }
    public void setItems(List<OutboundOrderItemDTO> items) { this.items = items; }
}
