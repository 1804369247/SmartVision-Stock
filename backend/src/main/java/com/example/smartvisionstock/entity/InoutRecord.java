package com.example.smartvisionstock.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inout_record")
public class InoutRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_no", nullable = false, length = 50)
    private String orderNo;
    
    @Column(nullable = false, length = 10)
    private String type;
    
    @Column(name = "goods_instance_id", nullable = false)
    private Long goodsInstanceId;
    
    @Column(name = "from_location_id")
    private Long fromLocationId;
    
    @Column(name = "to_location_id")
    private Long toLocationId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "operator_id")
    private Long operatorId;
    
    @Column(name = "operate_time", nullable = false)
    private LocalDateTime operateTime;

    public InoutRecord() {}

    public InoutRecord(Long id, String orderNo, String type, Long goodsInstanceId, Long fromLocationId, Long toLocationId, Integer quantity, Long operatorId, LocalDateTime operateTime) {
        this.id = id;
        this.orderNo = orderNo;
        this.type = type;
        this.goodsInstanceId = goodsInstanceId;
        this.fromLocationId = fromLocationId;
        this.toLocationId = toLocationId;
        this.quantity = quantity;
        this.operatorId = operatorId;
        this.operateTime = operateTime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getGoodsInstanceId() { return goodsInstanceId; }
    public void setGoodsInstanceId(Long goodsInstanceId) { this.goodsInstanceId = goodsInstanceId; }
    public Long getFromLocationId() { return fromLocationId; }
    public void setFromLocationId(Long fromLocationId) { this.fromLocationId = fromLocationId; }
    public Long getToLocationId() { return toLocationId; }
    public void setToLocationId(Long toLocationId) { this.toLocationId = toLocationId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public LocalDateTime getOperateTime() { return operateTime; }
    public void setOperateTime(LocalDateTime operateTime) { this.operateTime = operateTime; }
}
