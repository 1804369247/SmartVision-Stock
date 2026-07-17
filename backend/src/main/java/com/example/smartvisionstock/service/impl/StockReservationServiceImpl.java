package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.entity.GoodsInstance;
import com.example.smartvisionstock.entity.OutboundOrderItem;
import com.example.smartvisionstock.entity.StockReservation;
import com.example.smartvisionstock.repository.GoodsInstanceRepository;
import com.example.smartvisionstock.repository.StockReservationRepository;
import com.example.smartvisionstock.service.StockReservationService;
import com.example.smartvisionstock.util.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockReservationServiceImpl implements StockReservationService {

    private static final Logger log = LoggerFactory.getLogger(StockReservationServiceImpl.class);

    @Autowired
    private StockReservationRepository reservationRepository;

    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;

    /** 预占有效期（分钟），默认 30 分钟 */
    @Value("${app.reservation.ttl-minutes:30}")
    private int ttlMinutes;

    @Override
    @Transactional
    public void reserve(Long orderId, String orderType, List<OutboundOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String operator = UserContext.getCurrentUsernameOrDefault("admin");

        for (OutboundOrderItem item : items) {
            // 悲观写锁读取实例，防止并发审核时重复占用同一库存
            GoodsInstance instance = goodsInstanceRepository.findByIdForUpdate(item.getGoodsInstanceId())
                    .orElseThrow(() -> new RuntimeException(
                            "出库明细对应的货物实例不存在，instanceId=" + item.getGoodsInstanceId()));

            if (instance.isFrozen()) {
                throw new RuntimeException("货物实例已被冻结，无法预占，instanceId=" + instance.getId());
            }

            // 可用库存 = 实例库存 − 他人（排除本订单）生效中的预占
            int reservedByOthers = getReservedQuantityExcludeOrder(instance.getId(), orderId);
            int available = instance.getQuantity() - reservedByOthers;
            if (available < item.getQuantity()) {
                throw new RuntimeException(String.format(
                        "库存不足：货物实例[%d] 实际可用 %d，本单需预占 %d（含他人已预占 %d）",
                        instance.getId(), available, item.getQuantity(), reservedByOthers));
            }

            StockReservation reservation = new StockReservation();
            reservation.setGoodsInstanceId(instance.getId());
            reservation.setOrderId(orderId);
            reservation.setOrderType(orderType);
            reservation.setGoodsId(item.getGoodsId());
            reservation.setQuantity(item.getQuantity());
            reservation.setStatus(StockReservation.STATUS_ACTIVE);
            reservation.setExpireTime(now.plusMinutes(ttlMinutes));
            reservation.setCreateTime(now);
            reservation.setOperator(operator);
            reservationRepository.save(reservation);

            log.info("库存预占创建: orderId={}, instanceId={}, qty={}, expireAt={}",
                    orderId, instance.getId(), item.getQuantity(), reservation.getExpireTime());
        }
    }

    @Override
    @Transactional
    public void consumeByOrder(Long orderId) {
        int n = reservationRepository.releaseByOrderId(
                orderId, StockReservation.STATUS_CONSUMED, LocalDateTime.now());
        log.info("库存预占已消耗: orderId={}, count={}", orderId, n);
    }

    @Override
    @Transactional
    public void releaseByOrder(Long orderId) {
        int n = reservationRepository.releaseByOrderId(
                orderId, StockReservation.STATUS_RELEASED, LocalDateTime.now());
        log.info("库存预占已释放: orderId={}, count={}", orderId, n);
    }

    @Override
    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void releaseExpired() {
        LocalDateTime now = LocalDateTime.now();
        List<StockReservation> expired =
                reservationRepository.findByStatusAndExpireTimeBefore(StockReservation.STATUS_ACTIVE, now);
        if (expired.isEmpty()) {
            return;
        }
        for (StockReservation r : expired) {
            r.setStatus(StockReservation.STATUS_EXPIRED);
            r.setReleaseTime(now);
            reservationRepository.save(r);
            log.info("库存预占已过期释放: reservationId={}, orderId={}, instanceId={}",
                    r.getId(), r.getOrderId(), r.getGoodsInstanceId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int getReservedQuantity(Long goodsInstanceId) {
        return reservationRepository
                .findByGoodsInstanceIdAndStatus(goodsInstanceId, StockReservation.STATUS_ACTIVE)
                .stream()
                .mapToInt(StockReservation::getQuantity)
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public int getReservedQuantityExcludeOrder(Long goodsInstanceId, Long orderId) {
        return reservationRepository.findActiveByInstanceExcludeOrder(goodsInstanceId, orderId)
                .stream()
                .mapToInt(StockReservation::getQuantity)
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public int getAvailableQuantity(Long goodsInstanceId) {
        GoodsInstance instance = goodsInstanceRepository.findById(goodsInstanceId).orElse(null);
        if (instance == null) {
            return 0;
        }
        return instance.getQuantity() - getReservedQuantity(goodsInstanceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockReservation> findByOrderId(Long orderId) {
        return reservationRepository.findByOrderId(orderId);
    }
}
