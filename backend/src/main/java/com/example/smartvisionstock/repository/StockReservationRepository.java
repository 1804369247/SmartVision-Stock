package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    /**
     * 查询某货物实例上"生效中"的预占（用于计算可用库存）。
     */
    List<StockReservation> findByGoodsInstanceIdAndStatus(Long goodsInstanceId, String status);

    /**
     * 查询某订单关联的所有预占。
     */
    List<StockReservation> findByOrderId(Long orderId);

    /**
     * 查询某订单在某货物实例上的"生效中"预占（排除自身，计算他人占用）。
     */
    @Query("SELECT r FROM StockReservation r WHERE r.goodsInstanceId = :instanceId " +
           "AND r.status = 'ACTIVE' AND r.orderId <> :orderId")
    List<StockReservation> findActiveByInstanceExcludeOrder(@Param("instanceId") Long goodsInstanceId,
                                                            @Param("orderId") Long orderId);

    /**
     * 查询所有已过期且仍然生效的预占，交给定时任务释放。
     */
    List<StockReservation> findByStatusAndExpireTimeBefore(String status, LocalDateTime expireTime);

    /**
     * 批量将某订单的生效预占置为指定状态并写入释放时间。
     */
    @Modifying
    @Query("UPDATE StockReservation r SET r.status = :status, r.releaseTime = :now " +
           "WHERE r.orderId = :orderId AND r.status = 'ACTIVE'")
    int releaseByOrderId(@Param("orderId") Long orderId,
                         @Param("status") String status,
                         @Param("now") LocalDateTime now);
}
