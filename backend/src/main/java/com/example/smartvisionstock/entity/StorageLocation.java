package com.example.smartvisionstock.entity;

import javax.persistence.*;

@Entity
@Table(name = "storage_location", indexes = {
    @Index(name = "idx_storage_location_barcode", columnList = "barcode"),
    @Index(name = "idx_storage_location_area", columnList = "area"),
    @Index(name = "idx_storage_location_status", columnList = "status"),
    @Index(name = "idx_storage_location_current_goods_instance_id", columnList = "current_goods_instance_id")
})
public class StorageLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 20)
    private String locationCode;
    
    @Column(nullable = false, length = 10)
    private String area;
    
    @Column(nullable = false)
    private Integer status = 0;
    
    @Column(nullable = false)
    private Double xCoord;
    
    @Column(nullable = false)
    private Double yCoord;
    
    @Column(nullable = false)
    private Double zCoord;
    
    @Column(name = "current_goods_instance_id")
    private Long currentGoodsInstanceId;
    
    @Column(length = 50)
    private String attribute = "NORMAL";
    
    @Column(length = 100)
    private String description;

    @Column(length = 50, unique = true)
    private String barcode;

    public StorageLocation() {}

    public StorageLocation(Long id, String locationCode, String area, Integer status, Double xCoord, Double yCoord, Double zCoord, Long currentGoodsInstanceId, String attribute, String description) {
        this.id = id;
        this.locationCode = locationCode;
        this.area = area;
        this.status = status;
        this.xCoord = xCoord;
        this.yCoord = yCoord;
        this.zCoord = zCoord;
        this.currentGoodsInstanceId = currentGoodsInstanceId;
        this.attribute = attribute;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Double getXCoord() { return xCoord; }
    public void setXCoord(Double xCoord) { this.xCoord = xCoord; }
    public Double getYCoord() { return yCoord; }
    public void setYCoord(Double yCoord) { this.yCoord = yCoord; }
    public Double getZCoord() { return zCoord; }
    public void setZCoord(Double zCoord) { this.zCoord = zCoord; }
    public Long getCurrentGoodsInstanceId() { return currentGoodsInstanceId; }
    public void setCurrentGoodsInstanceId(Long currentGoodsInstanceId) { this.currentGoodsInstanceId = currentGoodsInstanceId; }
    public String getAttribute() { return attribute; }
    public void setAttribute(String attribute) { this.attribute = attribute; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
}
