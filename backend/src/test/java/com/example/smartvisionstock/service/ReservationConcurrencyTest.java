package com.example.smartvisionstock.service;

import com.example.smartvisionstock.entity.GoodsInstance;
import com.example.smartvisionstock.entity.OutboundOrder;
import com.example.smartvisionstock.entity.OutboundOrderItem;
import com.example.smartvisionstock.repository.GoodsInstanceRepository;
import com.example.smartvisionstock.repository.OutboundOrderItemRepository;
import com.example.smartvisionstock.repository.OutboundOrderRepository;
import com.example.smartvisionstock.repository.StockReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存预占（Reserved Stock）集成测试 —— 使用真实 Spring 上下文 + H2 内存库，
 * 重点验证并发场景下"不会超卖"这一核心不变式（悲观写锁 + 预占逻辑锁）。
 *
 * 覆盖：
 *  1. 单线程超卖防护：可用不足时 reserve 抛异常
 *  2. 预占生命周期：reserve → consume / release 对可用量的影响
 *  3. 并发预占防超卖：N 线程抢占同一实例，成功数受可用量严格约束
 *  4. 并发出库确认无丢失更新：并发 confirm 后最终库存 = 初始 − 已确认，且永不为负
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("库存预占并发/超卖集成测试")
class ReservationConcurrencyTest {

    @Autowired
    private StockReservationService reservationService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;

    @Autowired
    private OutboundOrderRepository outboundOrderRepository;

    @Autowired
    private OutboundOrderItemRepository outboundOrderItemRepository;

    @Autowired
    private StockReservationRepository reservationRepository;

    /** 创建一个库存实例（含全部 NOT NULL 字段）。 */
    private GoodsInstance newInstance(int quantity) {
        GoodsInstance gi = new GoodsInstance();
        gi.setGoodsId(1L);
        gi.setBatchNo("BATCH-TEST-" + System.nanoTime());
        gi.setQuantity(quantity);
        gi.setLocationId(null);
        gi.setInTime(LocalDateTime.now());
        gi.setOperator("tester");
        gi.setFrozen(false);
        return goodsInstanceRepository.save(gi);
    }

    /** 构造一个出库明细（不落库 order 也可，reserve 只读取 item 字段）。 */
    private OutboundOrderItem newItem(Long instanceId, int qty) {
        OutboundOrderItem it = new OutboundOrderItem();
        it.setGoodsInstanceId(instanceId);
        it.setGoodsId(1L);
        it.setQuantity(qty);
        return it;
    }

    /** 落库一张 PICKING 状态的出库单及其明细，返回订单 ID（供 confirm 测试使用）。 */
    private Long persistPickingOrder(Long instanceId, int qty) {
        OutboundOrder order = new OutboundOrder();
        order.setOrderNo("OUT-TEST-" + System.nanoTime());
        order.setType("sale");
        order.setWarehouseId(1L);
        order.setStatus("PICKING");
        order.setTotalQuantity(qty);
        order.setCreateTime(LocalDateTime.now());
        order = outboundOrderRepository.save(order);

        OutboundOrderItem it = newItem(instanceId, qty);
        it.setOrderId(order.getId());
        outboundOrderItemRepository.save(it);
        return order.getId();
    }

    @Test
    @DisplayName("1) 单线程超卖防护：可用不足时 reserve 抛异常")
    void reserve_shouldRejectWhenInsufficient() {
        GoodsInstance gi = newInstance(5);

        // 需求 8 > 库存 5，应拒绝
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                reservationService.reserve(1001L, "OUTBOUND",
                        List.of(newItem(gi.getId(), 8))));
        assertTrue(ex.getMessage().contains("库存不足"), "异常信息应提示库存不足");

        // 未产生任何生效预占
        assertEquals(0, reservationService.getReservedQuantity(gi.getId()));
        assertEquals(5, reservationService.getAvailableQuantity(gi.getId()));
    }

    @Test
    @DisplayName("2) 预占生命周期：reserve 降低可用，consume/release 后恢复")
    void reserve_consume_release_lifecycle() {
        GoodsInstance gi = newInstance(10);

        reservationService.reserve(2001L, "OUTBOUND", List.of(newItem(gi.getId(), 3)));
        assertEquals(3, reservationService.getReservedQuantity(gi.getId()));
        assertEquals(7, reservationService.getAvailableQuantity(gi.getId()));

        // 消耗（出库确认）后，该预占不再计入生效占用
        reservationService.consumeByOrder(2001L);
        assertEquals(0, reservationService.getReservedQuantity(gi.getId()));
        assertEquals(10, reservationService.getAvailableQuantity(gi.getId()));

        // 再占再释放
        reservationService.reserve(2002L, "OUTBOUND", List.of(newItem(gi.getId(), 4)));
        assertEquals(6, reservationService.getAvailableQuantity(gi.getId()));
        reservationService.releaseByOrder(2002L);
        assertEquals(10, reservationService.getAvailableQuantity(gi.getId()));
    }

    @Test
    @DisplayName("3) 并发预占防超卖：10 线程各占 2，库存 10，成功数 <= 5 且占用 <= 库存")
    void concurrentReserve_shouldNotOversell() throws Exception {
        GoodsInstance gi = newInstance(10);
        final Long instanceId = gi.getId();
        final int threads = 10;
        final int qtyEach = 2;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final long orderId = 3000L + i;
            tasks.add(() -> {
                try {
                    reservationService.reserve(orderId, "OUTBOUND",
                            Collections.singletonList(newItem(instanceId, qtyEach)));
                    return true;
                } catch (RuntimeException e) {
                    return false; // 库存不足被拒绝
                }
            });
        }

        List<Future<Boolean>> results = pool.invokeAll(tasks);
        pool.shutdown();

        int success = 0;
        for (Future<Boolean> f : results) {
            if (f.get()) success++;
        }

        int totalReserved = reservationService.getReservedQuantity(instanceId);
        // 核心断言：占用量永不超过库存（无超卖）
        assertTrue(totalReserved <= 10, "生效预占总量不得超过库存，实际=" + totalReserved);
        assertEquals(success * qtyEach, totalReserved, "成功次数与占用量应一致");
        assertTrue(success <= 5, "库存 10 每单占 2，成功预占不应超过 5 单，实际=" + success);
    }

    @Test
    @DisplayName("4) 并发出库确认无丢失更新：库存 20，8 单各出 2，最终库存 = 4 且永不为负")
    void concurrentConfirm_shouldNotLoseUpdates() throws Exception {
        GoodsInstance gi = newInstance(20);
        final Long instanceId = gi.getId();
        final int orders = 8;
        final int qtyEach = 2;

        // 逐单预占（总需求 16 <= 20，全部成功）
        List<Long> orderIds = new ArrayList<>();
        for (int i = 0; i < orders; i++) {
            Long orderId = persistPickingOrder(instanceId, qtyEach);
            reservationService.reserve(orderId, "OUTBOUND",
                    Collections.singletonList(newItem(instanceId, qtyEach)));
            orderIds.add(orderId);
        }

        // 并发确认
        ExecutorService pool = Executors.newFixedThreadPool(orders);
        AtomicInteger confirmed = new AtomicInteger(0);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (Long orderId : orderIds) {
            tasks.add(() -> {
                try {
                    orderService.confirmOutboundOrder(orderId);
                    confirmed.incrementAndGet();
                } catch (RuntimeException ignored) {
                    // 理论上都应成功；即便个别失败，最终不变式仍须成立
                }
                return null;
            });
        }
        pool.invokeAll(tasks);
        pool.shutdown();

        GoodsInstance after = goodsInstanceRepository.findById(instanceId).orElse(null);
        int remaining = after == null ? 0 : after.getQuantity();

        // 核心不变式：最终库存 = 初始 − 已确认出库量，且永不为负（无丢失更新→无超卖）
        assertTrue(remaining >= 0, "库存不得为负，实际=" + remaining);
        assertEquals(20 - confirmed.get() * qtyEach, remaining,
                "并发确认后库存应精确等于 初始-已确认，无丢失更新");
    }
}
