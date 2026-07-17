package com.example.smartvisionstock.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 库存预占（Reserved Stock）记录。
 * 出库单审核通过（AUDITING）时锁定对应货物实例的库存，防止并发超卖；
 * 出库确认（COMPLETED）时消耗预占，订单取消/超时未处理时释放预占。
 */
@Entity
@Table(name = "stock_reservation", indexes = {
    @Index(name = "idx_sr_instance_id", columnList = "goods_instance_id"),
    @Index(name = "idx_sr_order_id", columnList = "order_id"),
    @Index(name = "idx_sr_status", columnList = "status"),
    @Index(name = "idx_sr_expire_time", columnList = "expire_time")
})
public class StockReservation {

    /** 预占状态：生效中 / 已消耗（出库） / 已释放（取消） / 已过期 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CONSUMED = "CONSUMED";
    public static final String STATUS_RELEASED = "RELEASED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "goods_instance_id", nullable = false)
    private Long goodsInstanceId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** 关联单据类型，当前仅 OUTBOUND */
    @Column(name = "order_type", nullable = false, length = 20)
    private String orderType;

    @Column(name = "goods_id", nullable = false)
    private Long goodsId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 20)
    private String status = STATUS_ACTIVE;

    /** 预占过期时间；超过该时间且仍为 ACTIVE 会被定时任务释放 */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "release_time")
    private LocalDateTime releaseTime;

    @Column(nullable = false, length = 50)
    private String operator;

    // 预占记录的并发由"审核时锁定货物实例(悲观写锁)"保证，此处不启用 @Version，
    // 避免 @Modifying 批量更新触发 Hibernate 版本号自增时对 null 版本空指针。
    @Column(name = "version")
    private Long version;

    public StockReservation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGoodsInstanceId() { return goodsInstanceId; }
    public void setGoodsInstanceId(Long goodsInstanceId) { this.goodsInstanceId = goodsInstanceId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public Long getGoodsId() { return goodsId; }
    public void setGoodsId(Long goodsId) { this.goodsId = goodsId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getReleaseTime() { return releaseTime; }
    public void setReleaseTime(LocalDateTime releaseTime) { this.releaseTime = releaseTime; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
