package com.example.smartvisionstock.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "goods_instance")
public class GoodsInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "goods_id", nullable = false)
    private Long goodsId;
    
    @Column(name = "batch_no", nullable = false, length = 50)
    private String batchNo;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "location_id")
    private Long locationId;
    
    @Column(name = "in_time", nullable = false)
    private LocalDateTime inTime;
    
    @Column(name = "out_time")
    private LocalDateTime outTime;
    
    @Column(nullable = false, length = 50)
    private String operator;
    
    @Column(name = "supplier", length = 100)
    private String supplier;
    
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;
    
    @Column(name = "frozen", nullable = false)
    private Boolean frozen = false;
    
    @Column(name = "frozen_reason")
    private String frozenReason;

    public GoodsInstance() {}

    public GoodsInstance(Long id, Long goodsId, String batchNo, Integer quantity, Long locationId, LocalDateTime inTime, LocalDateTime outTime, String operator, String supplier, LocalDateTime expiryDate, Boolean frozen, String frozenReason) {
        this.id = id;
        this.goodsId = goodsId;
        this.batchNo = batchNo;
        this.quantity = quantity;
        this.locationId = locationId;
        this.inTime = inTime;
        this.outTime = outTime;
        this.operator = operator;
        this.supplier = supplier;
        this.expiryDate = expiryDate;
        this.frozen = frozen;
        this.frozenReason = frozenReason;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGoodsId() { return goodsId; }
    public void setGoodsId(Long goodsId) { this.goodsId = goodsId; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public LocalDateTime getInTime() { return inTime; }
    public void setInTime(LocalDateTime inTime) { this.inTime = inTime; }
    public LocalDateTime getOutTime() { return outTime; }
    public void setOutTime(LocalDateTime outTime) { this.outTime = outTime; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    public Boolean getFrozen() { return frozen; }
    public void setFrozen(Boolean frozen) { this.frozen = frozen; }
    public String getFrozenReason() { return frozenReason; }
    public void setFrozenReason(String frozenReason) { this.frozenReason = frozenReason; }
}
