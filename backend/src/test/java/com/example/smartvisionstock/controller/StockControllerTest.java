package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.request.MoveRequest;
import com.example.smartvisionstock.dto.response.LocationInfoDTO;
import com.example.smartvisionstock.entity.StorageLocation;
import com.example.smartvisionstock.repository.StorageLocationRepository;
import com.example.smartvisionstock.service.StockService;
import com.example.smartvisionstock.util.JwtUtil;
import com.example.smartvisionstock.util.TokenBlacklist;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StockController 库存核心接口测试
 * 测试货物查询、库位管理、入库、出库、移库全流程
 */
@WebMvcTest(value = StockController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@DisplayName("StockController 库存接口测试")
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StockService stockService;

    @MockBean
    private StorageLocationRepository storageLocationRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private TokenBlacklist tokenBlacklist;

    private StorageLocation testLocation;

    @BeforeEach
    void setUp() {
        testLocation = new StorageLocation();
        testLocation.setId(1L);
        testLocation.setLocationCode("A-01-01");
        testLocation.setArea("A");
        testLocation.setStatus(0); // 空闲
        testLocation.setXCoord(0.0);
        testLocation.setYCoord(0.0);
        testLocation.setZCoord(0.0);
        testLocation.setAttribute("NORMAL");
    }

    // ==================== 库位管理测试 ====================
    @Nested
    @DisplayName("库位 CRUD 操作")
    class LocationTests {

        @Test
        @DisplayName("GET /api/locations - 获取所有库位")
        void testGetAllLocations() throws Exception {
            LocationInfoDTO dto = new LocationInfoDTO();
            dto.setId(1L);
            dto.setLocationCode("A-01-01");
            dto.setArea("A");
            dto.setStatus(0);
            dto.setAttribute("NORMAL");
            when(stockService.getAllLocations()).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/locations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].locationCode").value("A-01-01"))
                    .andExpect(jsonPath("$.data.content[0].area").value("A"));
        }

        @Test
        @DisplayName("GET /api/locations - 空库位列表")
        void testGetAllLocationsEmpty() throws Exception {
            when(stockService.getAllLocations()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/locations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("GET /api/locations/{id} - 获取单个库位")
        void testGetLocationById() throws Exception {
            when(storageLocationRepository.findById(1L)).thenReturn(Optional.of(testLocation));

            mockMvc.perform(get("/api/locations/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.locationCode").value("A-01-01"))
                    .andExpect(jsonPath("$.data.area").value("A"));
        }

        @Test
        @DisplayName("GET /api/locations/{id} - 库位不存在")
        void testGetLocationByIdNotFound() throws Exception {
            when(storageLocationRepository.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/locations/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("库位不存在"));
        }

        @Test
        @DisplayName("POST /api/locations - 创建库位")
        void testCreateLocation() throws Exception {
            StorageLocation newLocation = new StorageLocation();
            newLocation.setLocationCode("B-02-03");
            newLocation.setArea("B");
            newLocation.setAttribute("COLD");

            when(storageLocationRepository.save(any(StorageLocation.class))).thenReturn(testLocation);

            mockMvc.perform(post("/api/locations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newLocation)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("PUT /api/locations/{id} - 更新库位")
        void testUpdateLocation() throws Exception {
            StorageLocation updated = new StorageLocation();
            updated.setLocationCode("A-01-01-UPDATED");
            updated.setArea("A");
            updated.setAttribute("NORMAL");

            when(storageLocationRepository.findById(1L)).thenReturn(Optional.of(testLocation));
            when(storageLocationRepository.save(any(StorageLocation.class))).thenReturn(testLocation);

            mockMvc.perform(put("/api/locations/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updated)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("PUT /api/locations/{id} - 库位不存在")
        void testUpdateLocationNotFound() throws Exception {
            StorageLocation updated = new StorageLocation();
            updated.setLocationCode("X-X-X");
            updated.setArea("X");

            when(storageLocationRepository.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/locations/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updated)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("DELETE /api/locations/{id} - 删除空闲库位")
        void testDeleteLocation() throws Exception {
            testLocation.setStatus(0); // 空闲
            when(storageLocationRepository.findById(1L)).thenReturn(Optional.of(testLocation));

            mockMvc.perform(delete("/api/locations/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(storageLocationRepository).deleteById(1L);
        }

        @Test
        @DisplayName("DELETE /api/locations/{id} - 不能删除非空闲库位")
        void testDeleteOccupiedLocationFails() throws Exception {
            testLocation.setStatus(1); // 已占用
            when(storageLocationRepository.findById(1L)).thenReturn(Optional.of(testLocation));

            mockMvc.perform(delete("/api/locations/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("只能删除空闲库位"));

            verify(storageLocationRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("GET /api/locations/area - 按区域查询库位")
        void testGetLocationsByArea() throws Exception {
            when(storageLocationRepository.findByArea("A")).thenReturn(List.of(testLocation));

            mockMvc.perform(get("/api/locations/area").param("area", "A"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }
    }

    // ==================== 移库测试 ====================
    @Nested
    @DisplayName("POST /api/move 移库操作")
    class MoveTests {

        @Test
        @DisplayName("移库成功")
        void testMoveSuccess() throws Exception {
            Map<String, Object> svcResult = new HashMap<>();
            svcResult.put("code", 200);
            svcResult.put("message", "移库成功");
            when(stockService.move(1L, 2L)).thenReturn(svcResult);

            MoveRequest request = new MoveRequest();
            request.setGoodsInstanceId(1L);
            request.setTargetLocationId(2L);

            mockMvc.perform(post("/api/move")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("移库失败 - 目标库位已占用")
        void testMoveFailTargetOccupied() throws Exception {
            Map<String, Object> svcResult = new HashMap<>();
            svcResult.put("code", 400);
            svcResult.put("message", "目标库位已被占用");
            when(stockService.move(1L, 2L)).thenReturn(svcResult);

            MoveRequest request = new MoveRequest();
            request.setGoodsInstanceId(1L);
            request.setTargetLocationId(2L);

            mockMvc.perform(post("/api/move")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }
}
