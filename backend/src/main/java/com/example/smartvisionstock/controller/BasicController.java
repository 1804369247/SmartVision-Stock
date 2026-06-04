package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.entity.Goods;
import com.example.smartvisionstock.entity.Supplier;
import com.example.smartvisionstock.entity.Warehouse;
import com.example.smartvisionstock.repository.GoodsRepository;
import com.example.smartvisionstock.repository.SupplierRepository;
import com.example.smartvisionstock.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/basic")
@CrossOrigin(origins = "*")
public class BasicController {

    @Autowired
    private SupplierRepository supplierRepository;
    
    @Autowired
    private WarehouseRepository warehouseRepository;
    
    @Autowired
    private GoodsRepository goodsRepository;

    @GetMapping("/suppliers")
    public List<Supplier> getSuppliers() {
        return supplierRepository.findByEnabledTrue();
    }

    @PostMapping("/suppliers")
    public Map<String, Object> createSupplier(@RequestBody Supplier supplier) {
        try {
            supplier.setId(null);
            supplier.setEnabled(true);
            supplier.setSupplierCode(generateCode("SP"));
            supplierRepository.save(supplier);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @PutMapping("/suppliers/{id}")
    public Map<String, Object> updateSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        try {
            Supplier existing = supplierRepository.findById(id).orElseThrow(() -> new RuntimeException("供应商不存在"));
            existing.setName(supplier.getName());
            existing.setContact(supplier.getContact());
            existing.setPhone(supplier.getPhone());
            existing.setAddress(supplier.getAddress());
            existing.setRemark(supplier.getRemark());
            supplierRepository.save(existing);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @DeleteMapping("/suppliers/{id}")
    public Map<String, Object> deleteSupplier(@PathVariable Long id) {
        try {
            Supplier supplier = supplierRepository.findById(id).orElseThrow(() -> new RuntimeException("供应商不存在"));
            supplier.setEnabled(false);
            supplierRepository.save(supplier);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @GetMapping("/warehouses")
    public List<Warehouse> getWarehouses() {
        return warehouseRepository.findByEnabledTrue();
    }

    @PostMapping("/warehouses")
    public Map<String, Object> createWarehouse(@RequestBody Warehouse warehouse) {
        try {
            warehouse.setId(null);
            warehouse.setEnabled(true);
            warehouse.setWarehouseCode(generateCode("WH"));
            warehouseRepository.save(warehouse);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @PutMapping("/warehouses/{id}")
    public Map<String, Object> updateWarehouse(@PathVariable Long id, @RequestBody Warehouse warehouse) {
        try {
            Warehouse existing = warehouseRepository.findById(id).orElseThrow(() -> new RuntimeException("仓库不存在"));
            existing.setName(warehouse.getName());
            existing.setAddress(warehouse.getAddress());
            existing.setRemark(warehouse.getRemark());
            warehouseRepository.save(existing);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @DeleteMapping("/warehouses/{id}")
    public Map<String, Object> deleteWarehouse(@PathVariable Long id) {
        try {
            Warehouse warehouse = warehouseRepository.findById(id).orElseThrow(() -> new RuntimeException("仓库不存在"));
            warehouse.setEnabled(false);
            warehouseRepository.save(warehouse);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @GetMapping("/goods")
    public List<Goods> getGoodsList() {
        return goodsRepository.findAll();
    }

    @PostMapping("/goods")
    public Map<String, Object> createGoods(@RequestBody Goods goods) {
        try {
            goods.setId(null);
            goods.setCode(generateCode("GD"));
            goodsRepository.save(goods);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @PutMapping("/goods/{id}")
    public Map<String, Object> updateGoods(@PathVariable Long id, @RequestBody Goods goods) {
        try {
            Goods existing = goodsRepository.findById(id).orElseThrow(() -> new RuntimeException("货物不存在"));
            existing.setName(goods.getName());
            existing.setSpec(goods.getSpec());
            existing.setUnit(goods.getUnit());
            existing.setCategory(goods.getCategory());
            existing.setWarningQuantity(goods.getWarningQuantity());
            existing.setDefaultShelfLife(goods.getDefaultShelfLife());
            existing.setStorageRule(goods.getStorageRule());
            existing.setDefaultSupplier(goods.getDefaultSupplier());
            goodsRepository.save(existing);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @DeleteMapping("/goods/{id}")
    public Map<String, Object> deleteGoods(@PathVariable Long id) {
        try {
            goodsRepository.deleteById(id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    private String generateCode(String prefix) {
        return prefix + System.currentTimeMillis();
    }
}