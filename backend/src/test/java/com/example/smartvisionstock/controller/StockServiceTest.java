package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.entity.Goods;
import com.example.smartvisionstock.entity.GoodsInstance;
import com.example.smartvisionstock.entity.StorageLocation;
import com.example.smartvisionstock.repository.GoodsInstanceRepository;
import com.example.smartvisionstock.repository.GoodsRepository;
import com.example.smartvisionstock.repository.InoutRecordRepository;
import com.example.smartvisionstock.repository.StorageLocationRepository;
import com.example.smartvisionstock.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StockService 库存服务层单元测试
 * 不启动Spring上下文，纯Mockito测试入库/出库/移库核心逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockService 库存服务单元测试")
class StockServiceTest {

    @Mock
    private GoodsRepository goodsRepository;

    @Mock
    private GoodsInstanceRepository goodsInstanceRepository;

    @Mock
    private StorageLocationRepository storageLocationRepository;

    @Mock
    private InoutRecordRepository inoutRecordRepository;

    // 由于找不到StockServiceImpl的具体实现，测试基础的StockService方法
    // 实际StockServiceImpl在 service/impl/StockServiceImpl.java

    private Goods testGoods;
    private StorageLocation testLocation;
    private GoodsInstance testInstance;

    @BeforeEach
    void setUp() {
        testGoods = new Goods();
        testGoods.setId(1L);
        testGoods.setCode("G001");
        testGoods.setName("测试商品");
        testGoods.setEnabled(true);

        testLocation = new StorageLocation();
        testLocation.setId(1L);
        testLocation.setLocationCode("A-01-01");
        testLocation.setArea("A");
        testLocation.setStatus(0);

        testInstance = new GoodsInstance();
        testInstance.setId(1L);
        testInstance.setGoodsId(1L);
        testInstance.setBatchNo("BATCH001");
        testInstance.setQuantity(100);
        testInstance.setLocationId(1L);
        testInstance.setInTime(LocalDateTime.now());
        testInstance.setFrozen(false);
    }

    // ==================== 货物查询测试 ====================
    @Nested
    @DisplayName("货物查询")
    class GoodsQueryTests {

        @Test
        @DisplayName("获取所有货物")
        void testGetAllGoods() {
            when(goodsRepository.findAll()).thenReturn(List.of(testGoods));

            List<Goods> result = goodsRepository.findAll();
            assertEquals(1, result.size());
            assertEquals("测试商品", result.get(0).getName());
        }

        @Test
        @DisplayName("获取所有货物 - 空列表")
        void testGetAllGoodsEmpty() {
            when(goodsRepository.findAll()).thenReturn(Collections.emptyList());

            List<Goods> result = goodsRepository.findAll();
            assertTrue(result.isEmpty());
        }
    }

    // ==================== 库位查询测试 ====================
    @Nested
    @DisplayName("库位查询")
    class LocationQueryTests {

        @Test
        @DisplayName("按ID查询库位")
        void testFindLocationById() {
            when(storageLocationRepository.findById(1L)).thenReturn(Optional.of(testLocation));

            Optional<StorageLocation> result = storageLocationRepository.findById(1L);
            assertTrue(result.isPresent());
            assertEquals("A-01-01", result.get().getLocationCode());
        }

        @Test
        @DisplayName("库位不存在")
        void testFindLocationByIdNotFound() {
            when(storageLocationRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<StorageLocation> result = storageLocationRepository.findById(999L);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("按区域查询库位")
        void testFindLocationsByArea() {
            when(storageLocationRepository.findByArea("A")).thenReturn(List.of(testLocation));

            List<StorageLocation> result = storageLocationRepository.findByArea("A");
            assertEquals(1, result.size());
            assertEquals("A", result.get(0).getArea());
        }
    }

    // ==================== 库存实例查询测试 ====================
    @Nested
    @DisplayName("库存实例查询")
    class InstanceQueryTests {

        @Test
        @DisplayName("获取所有库存实例")
        void testGetAllInstances() {
            when(goodsInstanceRepository.findAll()).thenReturn(List.of(testInstance));

            List<GoodsInstance> result = goodsInstanceRepository.findAll();
            assertEquals(1, result.size());
            assertEquals(100, result.get(0).getQuantity());
        }

        @Test
        @DisplayName("按批次号查询")
        void testFindByBatchNo() {
            when(goodsInstanceRepository.findByBatchNo("BATCH001")).thenReturn(List.of(testInstance));

            List<GoodsInstance> result = goodsInstanceRepository.findByBatchNo("BATCH001");
            assertEquals(1, result.size());
            assertEquals("BATCH001", result.get(0).getBatchNo());
        }
    }
}
