package com.example.smartvisionstock.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "goods", indexes = {
    @Index(name = "idx_goods_barcode", columnList = "barcode"),
    @Index(name = "idx_goods_category", columnList = "category"),
    @Index(name = "idx_goods_enabled", columnList = "enabled")
})
public class Goods {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String code;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 100)
    private String spec;
    
    @Column(length = 20)
    private String unit;
    
    @Column(nullable = false)
    private Integer warningQuantity = 0;
    
    @Column(length = 50)
    private String category;
    
    @Column(length = 100)
    private String defaultSupplier;
    
    @Column(length = 50)
    private String storageRule;
    
    @Column(length = 50)
    private String barcode;
    
    @Column(nullable = false)
    private Integer defaultShelfLife = 0;

    @Column(nullable = true)
    private Boolean enabled = true;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    public Goods() {}

    public Goods(Long id, String code, String name, String spec, String unit, Integer warningQuantity, String category, String defaultSupplier, String storageRule, Integer defaultShelfLife) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.spec = spec;
        this.unit = unit;
        this.warningQuantity = warningQuantity;
        this.category = category;
        this.defaultSupplier = defaultSupplier;
        this.storageRule = storageRule;
        this.defaultShelfLife = defaultShelfLife;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Integer getWarningQuantity() { return warningQuantity; }
    public void setWarningQuantity(Integer warningQuantity) { this.warningQuantity = warningQuantity; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDefaultSupplier() { return defaultSupplier; }
    public void setDefaultSupplier(String defaultSupplier) { this.defaultSupplier = defaultSupplier; }
    public String getStorageRule() { return storageRule; }
    public void setStorageRule(String storageRule) { this.storageRule = storageRule; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public Integer getDefaultShelfLife() { return defaultShelfLife; }
    public void setDefaultShelfLife(Integer defaultShelfLife) { this.defaultShelfLife = defaultShelfLife; }
    public Boolean getEnabled() { return enabled; }
    public boolean isEnabled() { return Boolean.TRUE.equals(enabled); }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
