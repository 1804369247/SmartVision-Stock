package com.example.smartvisionstock.service;

import com.example.smartvisionstock.entity.OutboundOrderItem;
import com.example.smartvisionstock.entity.StockReservation;

import java.util.List;

/**
 * 库存预占（Reserved Stock）服务。
 * 通过逻辑锁（预占记录）在出库审核阶段锁定库存，避免并发超卖；
 * 出库确认时消耗预占，订单取消 / 超时未处理时自动释放。
 */
public interface StockReservationService {

    /** 为出库单审核创建预占（应在 @Transactional 内调用） */
    void reserve(Long orderId, String orderType, List<OutboundOrderItem> items);

    /** 出库确认：消耗订单的预占 */
    void consumeByOrder(Long orderId);

    /** 订单取消：释放订单的预占 */
    void releaseByOrder(Long orderId);

    /** 释放所有过期的生效预占（定时任务调用） */
    void releaseExpired();

    /** 某货物实例当前生效中的预占总量 */
    int getReservedQuantity(Long goodsInstanceId);

    /** 排除指定订单后，某货物实例当前生效中的预占总量（即"他人占用"） */
    int getReservedQuantityExcludeOrder(Long goodsInstanceId, Long orderId);

    /** 可用库存 = 货物实例库存 − 生效中预占总量 */
    int getAvailableQuantity(Long goodsInstanceId);

    /** 查询某订单关联的所有预占（前端展示用） */
    List<StockReservation> findByOrderId(Long orderId);
}
