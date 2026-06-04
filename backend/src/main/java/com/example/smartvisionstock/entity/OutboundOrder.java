package com.example.smartvisionstock.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbound_order")
public class OutboundOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_no", nullable = false, length = 50)
    private String orderNo;
    
    @Column(nullable = false, length = 20)
    private String type;
    
    @Column(name = "customer_id")
    private Long customerId;
    
    @Column(name = "warehouse_id")
    private Long warehouseId;
    
    @Column(nullable = false, length = 20)
    private String status;
    
    @Column(name = "total_quantity")
    private Integer totalQuantity;
    
    @Column(name = "operator_id")
    private Long operatorId;
    
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    @Column(name = "audit_time")
    private LocalDateTime auditTime;
    
    @Column(name = "pick_time")
    private LocalDateTime pickTime;
    
    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;
    
    @Column(length = 500)
    private String remark;

    public OutboundOrder() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
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
}
