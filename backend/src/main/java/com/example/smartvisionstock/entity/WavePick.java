package com.example.smartvisionstock.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 波次拣货 —— 将多个出库单合并为一个波次，统一拣货。
 * 数据持久化到数据库，不再使用内存 Map。
 */
@Entity
@Table(name = "wave_pick")
public class WavePick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 波次编号 */
    @Column(unique = true, nullable = false)
    private String waveId;

    /** 关联的出库单ID列表（JSON 数组字符串） */
    @Column(columnDefinition = "TEXT")
    private String orderIdsJson;

    /** 状态：CREATED / IN_PROGRESS / COMPLETED / CANCELLED */
    @Column(nullable = false)
    private String status;

    /** 创建时间 */
    @Column(nullable = false)
    private LocalDateTime createTime;

    /** 指派给谁（可选，关联 User.username） */
    private String assignedTo;

    /** 总拣货数量 */
    private Integer totalQuantity;

    /** 总拣货品项数 */
    private Integer totalItems;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    // ---- Getters / Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWaveId() { return waveId; }
    public void setWaveId(String waveId) { this.waveId = waveId; }

    public String getOrderIdsJson() { return orderIdsJson; }
    public void setOrderIdsJson(String orderIdsJson) { this.orderIdsJson = orderIdsJson; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    public Integer getTotalItems() { return totalItems; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }

    public LocalDateTime getCancelTime() { return cancelTime; }
    public void setCancelTime(LocalDateTime cancelTime) { this.cancelTime = cancelTime; }
}
