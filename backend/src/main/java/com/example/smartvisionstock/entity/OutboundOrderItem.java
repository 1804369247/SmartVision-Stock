package com.example.smartvisionstock.entity;

import javax.persistence.*;

@Entity
@Table(name = "outbound_order_item")
public class OutboundOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Column(name = "goods_instance_id", nullable = false)
    private Long goodsInstanceId;
    
    @Column(name = "goods_id", nullable = false)
    private Long goodsId;
    
    @Column(name = "batch_no", length = 50)
    private String batchNo;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "location_id")
    private Long locationId;
    
    @Column(name = "actual_quantity")
    private Integer actualQuantity;
    
    @Column(length = 200)
    private String remark;

    public OutboundOrderItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getGoodsInstanceId() { return goodsInstanceId; }
    public void setGoodsInstanceId(Long goodsInstanceId) { this.goodsInstanceId = goodsInstanceId; }
    public Long getGoodsId() { return goodsId; }
    public void setGoodsId(Long goodsId) { this.goodsId = goodsId; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public Integer getActualQuantity() { return actualQuantity; }
    public void setActualQuantity(Integer actualQuantity) { this.actualQuantity = actualQuantity; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
