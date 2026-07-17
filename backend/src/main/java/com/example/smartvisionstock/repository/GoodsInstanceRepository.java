package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.GoodsInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsInstanceRepository extends JpaRepository<GoodsInstance, Long> {

    List<GoodsInstance> findByLocationId(Long locationId);
    Optional<GoodsInstance> findFirstByLocationId(Long locationId);

    List<GoodsInstance> findByGoodsId(Long goodsId);

    /** 按批次号查找（批次追溯用）*/
    List<GoodsInstance> findByBatchNo(String batchNo);

    /** 查找某商品的某批次（FIFO/LIFO/FEFO 策略用）*/
    List<GoodsInstance> findByGoodsIdAndFrozenFalseOrderByInTimeAsc(Long goodsId);

    List<GoodsInstance> findByGoodsIdAndFrozenFalseOrderByInTimeDesc(Long goodsId);

    List<GoodsInstance> findByGoodsIdAndFrozenFalseOrderByExpiryDateAsc(Long goodsId);

    /** 查找效期在指定时间之前的、库存大于0的实例（过期预警）*/
    @Query("SELECT gi FROM GoodsInstance gi WHERE gi.expiryDate IS NOT NULL " +
           "AND gi.expiryDate <= :threshold AND gi.quantity > 0 AND gi.frozen = false")
    List<GoodsInstance> findExpiringInstances(@Param("threshold") LocalDateTime threshold);

    /** 按商品ID聚合库存数量（库存统计用）*/
    @Query("SELECT gi.goodsId, SUM(gi.quantity) FROM GoodsInstance gi GROUP BY gi.goodsId")
    List<Object[]> sumQuantityByGoodsId();

    /** 统计未出库的库位实例数量（仓库删除前检查用）*/
    long countByLocationIdIsNotNull();

    /** 查找即将过期的实例（不过滤冻结状态，统计用）*/
    @Query("SELECT gi FROM GoodsInstance gi WHERE gi.expiryDate IS NOT NULL " +
           "AND gi.expiryDate <= :threshold AND gi.quantity > 0")
    List<GoodsInstance> findExpiringInstancesForStats(@Param("threshold") LocalDateTime threshold);

    /** 批量统计用 COUNT 查询 */
    @Query("SELECT COUNT(gi) FROM GoodsInstance gi WHERE gi.quantity > 0 AND gi.frozen = false")
    long countActiveBatches();

    @Query("SELECT COUNT(gi) FROM GoodsInstance gi WHERE gi.quantity = 0")
    long countDepletedBatches();

    @Query("SELECT COUNT(gi) FROM GoodsInstance gi WHERE gi.frozen = true")
    long countFrozenBatches();

    @Query("SELECT COALESCE(SUM(gi.quantity), 0) FROM GoodsInstance gi")
    long sumTotalQuantity();

    /** 悲观写锁读取实例，防止并发审核时对同一库存重复预占 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT gi FROM GoodsInstance gi WHERE gi.id = :id")
    Optional<GoodsInstance> findByIdForUpdate(@Param("id") Long id);
}
