package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.dto.request.InoutRecordQueryRequest;
import com.example.smartvisionstock.dto.response.InoutRecordDTO;
import com.example.smartvisionstock.dto.response.LocationInfoDTO;
import com.example.smartvisionstock.dto.response.ReplayRecordDTO;
import com.example.smartvisionstock.entity.Goods;
import com.example.smartvisionstock.entity.GoodsInstance;
import com.example.smartvisionstock.entity.InoutRecord;
import com.example.smartvisionstock.entity.StorageLocation;
import com.example.smartvisionstock.repository.GoodsInstanceRepository;
import com.example.smartvisionstock.repository.GoodsRepository;
import com.example.smartvisionstock.repository.InoutRecordRepository;
import com.example.smartvisionstock.repository.StorageLocationRepository;
import com.example.smartvisionstock.service.StockService;
import com.example.smartvisionstock.websocket.StockWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StockServiceImpl implements StockService {

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private StorageLocationRepository storageLocationRepository;

    @Autowired
    private GoodsInstanceRepository goodsInstanceRepository;

    @Autowired
    private InoutRecordRepository inoutRecordRepository;

    @Autowired
    private StockWebSocketHandler webSocketHandler;

    @PersistenceContext
    private EntityManager entityManager;

    private String generateOrderNo() {
        return "RK" + System.currentTimeMillis();
    }

    @Override
    public List<Goods> getAllGoods() {
        return goodsRepository.findAll();
    }

    @Override
    public Goods getGoodsById(Long id) {
        return goodsRepository.findById(id).orElse(null);
    }

    @Override
    public List<LocationInfoDTO> getAllLocations() {
        return storageLocationRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public StorageLocation getLocationById(Long id) {
        return storageLocationRepository.findById(id).orElse(null);
    }

    @Override
    public StorageLocation getLocationByCode(String code) {
        return storageLocationRepository.findByLocationCode(code).orElse(null);
    }

    @Override
    @Transactional
    public GoodsInstance createGoodsInstance(Long goodsId, String batchNo, Integer quantity, Long locationId, String operator) {
        GoodsInstance instance = new GoodsInstance();
        instance.setGoodsId(goodsId);
        instance.setBatchNo(batchNo);
        instance.setQuantity(quantity);
        instance.setLocationId(locationId);
        instance.setInTime(LocalDateTime.now());
        instance.setOperator(operator);
        return goodsInstanceRepository.save(instance);
    }

    @Override
    @Transactional
    public GoodsInstance updateGoodsInstance(Long id, Integer quantity) {
        Optional<GoodsInstance> opt = goodsInstanceRepository.findById(id);
        if (opt.isPresent()) {
            GoodsInstance instance = opt.get();
            instance.setQuantity(quantity);
            if (quantity <= 0) {
                instance.setOutTime(LocalDateTime.now());
                instance.setLocationId(null);
            }
            return goodsInstanceRepository.save(instance);
        }
        return null;
    }

    @Override
    public GoodsInstance getGoodsInstanceById(Long id) {
        return goodsInstanceRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public InoutRecord createInboundRecord(Long goodsId, String batchNo, Integer quantity, Long locationId, String operator) {
        InoutRecord record = new InoutRecord();
        record.setOrderNo("IN-" + System.currentTimeMillis());
        record.setType("IN");
        record.setGoodsInstanceId(goodsInstanceRepository.findByLocationId(locationId).stream().findFirst().map(GoodsInstance::getId).orElse(null));
        record.setToLocationId(locationId);
        record.setQuantity(quantity);
        record.setOperateTime(LocalDateTime.now());
        return inoutRecordRepository.save(record);
    }

    @Override
    @Transactional
    public InoutRecord createOutboundRecord(Long goodsInstanceId, Integer quantity, String operator) {
        GoodsInstance instance = goodsInstanceRepository.findById(goodsInstanceId).orElse(null);
        if (instance == null) return null;

        InoutRecord record = new InoutRecord();
        record.setOrderNo("OUT-" + System.currentTimeMillis());
        record.setType("OUT");
        record.setGoodsInstanceId(goodsInstanceId);
        record.setFromLocationId(instance.getLocationId());
        record.setQuantity(quantity);
        record.setOperateTime(LocalDateTime.now());
        return inoutRecordRepository.save(record);
    }

    @Override
    @Transactional
    public Map<String, Object> inbound(Long goodsId, String batchNo, Integer quantity, Long locationId) {
        Map<String, Object> result = new HashMap<>();
        
        StorageLocation location = storageLocationRepository.findById(locationId).orElse(null);
        if (location == null) {
            result.put("success", false);
            result.put("message", "仓位不存在");
            return result;
        }
        
        if (location.getStatus() == 1) {
            result.put("success", false);
            result.put("message", "该仓位已有货物");
            return result;
        }

        Goods goods = goodsRepository.findById(goodsId).orElse(null);
        if (goods == null) {
            result.put("success", false);
            result.put("message", "货物不存在");
            return result;
        }

        GoodsInstance instance = createGoodsInstance(goodsId, batchNo, quantity, locationId, "admin");
        
        location.setStatus(1);
        location.setCurrentGoodsInstanceId(instance.getId());
        storageLocationRepository.save(location);

        createInboundRecord(goodsId, batchNo, quantity, locationId, "admin");

        result.put("success", true);
        result.put("message", "入库成功");
        result.put("locationId", locationId);
        result.put("newStatus", 1);
        result.put("goodsInstanceId", instance.getId());

        webSocketHandler.broadcastStockChange(locationId, 1, instance.getId());

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> outbound(Long goodsInstanceId, Integer quantity) {
        Map<String, Object> result = new HashMap<>();
        
        GoodsInstance instance = goodsInstanceRepository.findById(goodsInstanceId).orElse(null);
        if (instance == null) {
            result.put("success", false);
            result.put("message", "货物实例不存在");
            return result;
        }

        if (instance.getQuantity() < quantity) {
            result.put("success", false);
            result.put("message", "库存不足");
            return result;
        }

        Long locationId = instance.getLocationId();
        int newQuantity = instance.getQuantity() - quantity;
        
        if (newQuantity <= 0) {
            StorageLocation location = storageLocationRepository.findById(locationId).orElse(null);
            if (location != null) {
                location.setStatus(0);
                location.setCurrentGoodsInstanceId(null);
                storageLocationRepository.save(location);
            }
            updateGoodsInstance(goodsInstanceId, 0);
            webSocketHandler.broadcastStockChange(locationId, 0, null);
        } else {
            updateGoodsInstance(goodsInstanceId, newQuantity);
            webSocketHandler.broadcastStockChange(locationId, 1, goodsInstanceId);
        }

        createOutboundRecord(goodsInstanceId, quantity, "admin");

        result.put("success", true);
        result.put("message", "出库成功");
        result.put("locationId", locationId);
        result.put("newStatus", newQuantity > 0 ? 1 : 0);
        result.put("goodsInstanceId", newQuantity > 0 ? goodsInstanceId : null);

        return result;
    }

    @Override
    public Map<String, Object> move(Long goodsInstanceId, Long targetLocationId) {
        Map<String, Object> result = new HashMap<>();

        GoodsInstance instance = goodsInstanceRepository.findById(goodsInstanceId).orElse(null);
        if (instance == null) {
            result.put("success", false);
            result.put("message", "货物实例不存在");
            return result;
        }

        StorageLocation targetLocation = storageLocationRepository.findById(targetLocationId).orElse(null);
        if (targetLocation == null) {
            result.put("success", false);
            result.put("message", "目标库位不存在");
            return result;
        }

        if (targetLocation.getStatus() != 0) {
            result.put("success", false);
            result.put("message", "目标库位已被占用");
            return result;
        }

        Long sourceLocationId = instance.getLocationId();

        StorageLocation sourceLocation = storageLocationRepository.findById(sourceLocationId).orElse(null);
        if (sourceLocation != null) {
            sourceLocation.setStatus(0);
            sourceLocation.setCurrentGoodsInstanceId(null);
            storageLocationRepository.save(sourceLocation);
            webSocketHandler.broadcastStockChange(sourceLocationId, 0, null);
        }

        targetLocation.setStatus(1);
        targetLocation.setCurrentGoodsInstanceId(goodsInstanceId);
        storageLocationRepository.save(targetLocation);
        webSocketHandler.broadcastStockChange(targetLocationId, 1, goodsInstanceId);

        instance.setLocationId(targetLocationId);
        goodsInstanceRepository.save(instance);

        InoutRecord record = new InoutRecord();
        record.setOrderNo(generateOrderNo());
        record.setType("MOVE");
        record.setGoodsInstanceId(goodsInstanceId);
        record.setFromLocationId(sourceLocationId);
        record.setToLocationId(targetLocationId);
        record.setQuantity(instance.getQuantity());
        record.setOperatorId(1L);
        record.setOperateTime(LocalDateTime.now());
        inoutRecordRepository.save(record);

        result.put("success", true);
        result.put("message", "移库成功");
        result.put("sourceLocationId", sourceLocationId);
        result.put("targetLocationId", targetLocationId);

        return result;
    }

    private LocationInfoDTO convertToDTO(StorageLocation location) {
        LocationInfoDTO dto = new LocationInfoDTO();
        dto.setId(location.getId());
        dto.setLocationCode(location.getLocationCode());
        dto.setArea(location.getArea());
        dto.setStatus(location.getStatus());
        dto.setXCoord(location.getXCoord());
        dto.setYCoord(location.getYCoord());
        dto.setZCoord(location.getZCoord());
        dto.setCurrentGoodsInstanceId(location.getCurrentGoodsInstanceId());
        dto.setAttribute(location.getAttribute() != null ? location.getAttribute() : "NORMAL");
        dto.setDescription(location.getDescription());

        if (location.getCurrentGoodsInstanceId() != null) {
            GoodsInstance instance = goodsInstanceRepository.findById(location.getCurrentGoodsInstanceId()).orElse(null);
            if (instance != null) {
                dto.setBatchNo(instance.getBatchNo());
                dto.setQuantity(instance.getQuantity());
                dto.setSupplier(instance.getSupplier());
                dto.setExpiryDate(instance.getExpiryDate());
                dto.setInTime(instance.getInTime());
                dto.setFrozen(instance.getFrozen() != null ? instance.getFrozen() : false);
                dto.setFrozenReason(instance.getFrozenReason());
                Goods goods = goodsRepository.findById(instance.getGoodsId()).orElse(null);
                if (goods != null) {
                    dto.setGoodsName(goods.getName());
                    dto.setGoodsCode(goods.getCode());
                    dto.setGoodsCategory(goods.getCategory());
                    dto.setStorageRule(goods.getStorageRule());
                }
            }
        }

        return dto;
    }

    @Override
    public Page<InoutRecordDTO> getInoutRecords(InoutRecordQueryRequest request) {
        int pageNum = request.getPage() != null ? request.getPage() : 0;
        int pageSize = request.getSize() != null ? request.getSize() : 10;
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "operateTime"));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<InoutRecord> cq = cb.createQuery(InoutRecord.class);
        Root<InoutRecord> root = cq.from(InoutRecord.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (request.getType() != null && !request.getType().isEmpty()) {
            predicates.add(cb.equal(root.get("type"), request.getType()));
        }
        
        if (request.getStartTime() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("operateTime"), request.getStartTime()));
        }
        
        if (request.getEndTime() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("operateTime"), request.getEndTime()));
        }
        
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("operateTime")));

        TypedQuery<InoutRecord> query = entityManager.createQuery(cq);
        query.setFirstResult(pageNum * pageSize);
        query.setMaxResults(pageSize);
        
        List<InoutRecord> records = query.getResultList();
        
        if (request.getGoodsName() != null && !request.getGoodsName().isEmpty()) {
            records = records.stream().filter(r -> {
                GoodsInstance gi = goodsInstanceRepository.findById(r.getGoodsInstanceId()).orElse(null);
                if (gi != null) {
                    Goods g = goodsRepository.findById(gi.getGoodsId()).orElse(null);
                    return g != null && g.getName() != null && g.getName().contains(request.getGoodsName());
                }
                return false;
            }).collect(Collectors.toList());
        }
        
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        countQuery.select(cb.count(countQuery.from(InoutRecord.class)));
        countQuery.where(predicates.toArray(new Predicate[0]));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        Page<InoutRecord> recordPage = new PageImpl<>(records, pageable, total);
        
        return recordPage.map(this::convertToRecordDTO);
    }

    @Override
    public ReplayRecordDTO getReplayRecord(Long recordId) {
        InoutRecord record = inoutRecordRepository.findById(recordId).orElse(null);
        if (record == null) {
            return null;
        }

        ReplayRecordDTO dto = new ReplayRecordDTO();
        dto.setId(record.getId());
        dto.setOrderNo(record.getOrderNo());
        dto.setType(record.getType());
        dto.setQuantity(record.getQuantity());
        dto.setOperateTime(record.getOperateTime());
        dto.setFromLocationId(record.getFromLocationId());
        dto.setToLocationId(record.getToLocationId());

        GoodsInstance instance = goodsInstanceRepository.findById(record.getGoodsInstanceId()).orElse(null);
        if (instance != null) {
            dto.setBatchNo(instance.getBatchNo());
            Goods goods = goodsRepository.findById(instance.getGoodsId()).orElse(null);
            if (goods != null) {
                dto.setGoodsName(goods.getName());
            }
        }

        if (record.getFromLocationId() != null) {
            StorageLocation fromLoc = storageLocationRepository.findById(record.getFromLocationId()).orElse(null);
            if (fromLoc != null) {
                dto.setFromLocationCode(fromLoc.getLocationCode());
                dto.setFromX(fromLoc.getXCoord());
                dto.setFromY(fromLoc.getYCoord());
                dto.setFromZ(fromLoc.getZCoord());
            }
        }

        if (record.getToLocationId() != null) {
            StorageLocation toLoc = storageLocationRepository.findById(record.getToLocationId()).orElse(null);
            if (toLoc != null) {
                dto.setToLocationCode(toLoc.getLocationCode());
                dto.setToX(toLoc.getXCoord());
                dto.setToY(toLoc.getYCoord());
                dto.setToZ(toLoc.getZCoord());
            }
        }

        return dto;
    }

    private InoutRecordDTO convertToRecordDTO(InoutRecord record) {
        InoutRecordDTO dto = new InoutRecordDTO();
        dto.setId(record.getId());
        dto.setOrderNo(record.getOrderNo());
        dto.setType(record.getType());
        dto.setQuantity(record.getQuantity());
        dto.setOperateTime(record.getOperateTime());
        dto.setGoodsInstanceId(record.getGoodsInstanceId());
        dto.setFromLocationId(record.getFromLocationId());
        dto.setToLocationId(record.getToLocationId());

        GoodsInstance instance = goodsInstanceRepository.findById(record.getGoodsInstanceId()).orElse(null);
        if (instance != null) {
            dto.setBatchNo(instance.getBatchNo());
            dto.setOperator(instance.getOperator());
            Goods goods = goodsRepository.findById(instance.getGoodsId()).orElse(null);
            if (goods != null) {
                dto.setGoodsName(goods.getName());
                dto.setGoodsCode(goods.getCode());
            }
        }

        if (record.getFromLocationId() != null) {
            StorageLocation fromLoc = storageLocationRepository.findById(record.getFromLocationId()).orElse(null);
            if (fromLoc != null) {
                dto.setFromLocationCode(fromLoc.getLocationCode());
            }
        }

        if (record.getToLocationId() != null) {
            StorageLocation toLoc = storageLocationRepository.findById(record.getToLocationId()).orElse(null);
            if (toLoc != null) {
                dto.setToLocationCode(toLoc.getLocationCode());
            }
        }

        return dto;
    }
}
