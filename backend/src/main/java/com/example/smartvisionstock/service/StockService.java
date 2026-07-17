package com.example.smartvisionstock.service;

import com.example.smartvisionstock.dto.request.InoutRecordQueryRequest;
import com.example.smartvisionstock.dto.response.InoutRecordDTO;
import com.example.smartvisionstock.dto.response.LocationInfoDTO;
import com.example.smartvisionstock.dto.response.ReplayRecordDTO;
import com.example.smartvisionstock.entity.Goods;
import com.example.smartvisionstock.entity.GoodsInstance;
import com.example.smartvisionstock.entity.InoutRecord;
import com.example.smartvisionstock.entity.StorageLocation;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface StockService {
    List<Goods> getAllGoods();
    Goods getGoodsById(Long id);
    
    List<LocationInfoDTO> getAllLocations();
    StorageLocation getLocationById(Long id);
    StorageLocation getLocationByCode(String code);
    
    GoodsInstance createGoodsInstance(Long goodsId, String batchNo, Integer quantity, Long locationId, String operator);
    GoodsInstance updateGoodsInstance(Long id, Integer quantity);
    GoodsInstance getGoodsInstanceById(Long id);
    Page<GoodsInstance> getAllInstances(int page, int size);
    
    InoutRecord createInboundRecord(Long goodsInstanceId, Long goodsId, String batchNo, Integer quantity, Long locationId, String operator);
    InoutRecord createOutboundRecord(Long goodsInstanceId, Integer quantity, String operator);
    
    Map<String, Object> inbound(Long goodsId, String batchNo, Integer quantity, Long locationId);
    Map<String, Object> outbound(Long goodsInstanceId, Integer quantity);
    Map<String, Object> move(Long goodsInstanceId, Long targetLocationId);
    
    Map<String, Object> adjustInventory(Long goodsInstanceId, Integer quantity);
    
    Page<InoutRecordDTO> getInoutRecords(InoutRecordQueryRequest request);
    ReplayRecordDTO getReplayRecord(Long recordId);
}
