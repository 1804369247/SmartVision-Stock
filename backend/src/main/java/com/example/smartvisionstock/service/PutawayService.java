package com.example.smartvisionstock.service;

import com.example.smartvisionstock.entity.GoodsInstance;
import com.example.smartvisionstock.entity.InoutRecord;
import com.example.smartvisionstock.entity.StorageLocation;
import com.example.smartvisionstock.event.StockChangeEvent;
import com.example.smartvisionstock.repository.GoodsInstanceRepository;
import com.example.smartvisionstock.repository.InoutRecordRepository;
import com.example.smartvisionstock.repository.StorageLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 入库上架：把新建的库存实例放到一个空闲库位，并补登记入库流水 + 广播库存变更，
 * 避免生成 locationId=null 的孤儿库存（前后端状态脱节）。
 * 若没有任何空闲库位，则保留未上架并记录流水（仅容量不足时的兜底）。
 */
@Service
public class PutawayService {

    private static final Logger log = LoggerFactory.getLogger(PutawayService.class);

    @Autowired
    private StorageLocationRepository storageLocationRepository;

    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;

    @Autowired
    private InoutRecordRepository inoutRecordRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long putaway(GoodsInstance instance) {
        // 先持久化拿到 id，便于回填库位的 currentGoodsInstanceId
        GoodsInstance saved = goodsInstanceRepository.save(instance);

        StorageLocation loc = storageLocationRepository.findByStatus(0).stream().findFirst().orElse(null);
        if (loc == null) {
            log.warn("无空闲库位，库存实例 {} 暂未上架（孤儿库存兜底）", saved.getId());
            InoutRecord rec = buildRecord(saved, null);
            inoutRecordRepository.save(rec);
            return null;
        }

        loc.setStatus(1);
        loc.setCurrentGoodsInstanceId(saved.getId());
        storageLocationRepository.save(loc);

        saved.setLocationId(loc.getId());
        goodsInstanceRepository.save(saved);

        InoutRecord rec = buildRecord(saved, loc.getId());
        inoutRecordRepository.save(rec);

        eventPublisher.publishEvent(new StockChangeEvent(loc.getId(), 1, saved.getId()));
        return loc.getId();
    }

    private InoutRecord buildRecord(GoodsInstance instance, Long toLocationId) {
        InoutRecord rec = new InoutRecord();
        rec.setOrderNo("IN-" + System.currentTimeMillis());
        rec.setType("IN");
        rec.setGoodsInstanceId(instance.getId());
        rec.setToLocationId(toLocationId);
        rec.setQuantity(instance.getQuantity());
        rec.setOperateTime(LocalDateTime.now());
        return rec;
    }
}
