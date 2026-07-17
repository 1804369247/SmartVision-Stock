package com.example.smartvisionstock.entity;

import javax.persistence.*;
import java.time.LocalDateTime;;

/**
 * 拣货任务 —— 每个波次下包含若干拣货任务，每个任务对应一个出库单。
 * 数据持久化到数据库，不再使用内存 Map。
 */
@Entity
@Table(name = "pick_task")
public class PickTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任务编号 */
    @Column(unique = true, nullable = false)
    private String taskId;

    /** 所属波次编号 */
    @Column(nullable = false)
    private String waveId;

    /** 关联的出库单ID */
    private Long orderId;

    /** 出库单号 */
    private String orderNo;

    /** 状态：PENDING / COMPLETED / CANCELLED */
    @Column(nullable = false)
    private String status;

    /** 优先级：HIGH / NORMAL */
    private String priority;

    /** 拣货明细（JSON 数组字符串） */
    @Column(columnDefinition = "TEXT")
    private String itemsJson;

    /** 库位路径（JSON 数组字符串） */
    @Column(columnDefinition = "TEXT")
    private String locationsJson;

    /** 总数量 */
    private Integer quantity;

    /** 仓库ID */
    private Long warehouseId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 操作人 */
    private String operator;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
    }

    // ---- Getters / Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getWaveId() { return waveId; }
    public void setWaveId(String waveId) { this.waveId = waveId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getItemsJson() { return itemsJson; }
    public void setItemsJson(String itemsJson) { this.itemsJson = itemsJson; }

    public String getLocationsJson() { return locationsJson; }
    public void setLocationsJson(String locationsJson) { this.locationsJson = locationsJson; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getCompleteTime() { return completeTime; }
    public void setCompleteTime(LocalDateTime completeTime) { this.completeTime = completeTime; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
