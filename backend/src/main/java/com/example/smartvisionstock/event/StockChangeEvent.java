package com.example.smartvisionstock.event;

/**
 * 库存变更事件。
 * 由库存写操作（入库/出库/调整/移库）在事务内发布，
 * 由 {@code @TransactionalEventListener(AFTER_COMMIT)} 监听并在事务提交后广播，
 * 避免事务回滚后仍通知客户端、以及广播期间占用数据库连接。
 */
public class StockChangeEvent {

    private final Long locationId;
    private final Integer newStatus;
    private final Long goodsInstanceId;

    public StockChangeEvent(Long locationId, Integer newStatus, Long goodsInstanceId) {
        this.locationId = locationId;
        this.newStatus = newStatus;
        this.goodsInstanceId = goodsInstanceId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public Integer getNewStatus() {
        return newStatus;
    }

    public Long getGoodsInstanceId() {
        return goodsInstanceId;
    }
}
