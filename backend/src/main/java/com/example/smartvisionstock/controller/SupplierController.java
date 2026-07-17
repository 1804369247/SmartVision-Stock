package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.entity.Supplier;
import com.example.smartvisionstock.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/basic/suppliers")
public class SupplierController {

    private static final AtomicLong SUPPLIER_SEQ = new AtomicLong(1);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private SupplierRepository supplierRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> getSuppliers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            List<Supplier> all = supplierRepository.findByEnabledTrue();
            List<Supplier> filtered = all.stream()
                    .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(kw))
                            || (s.getSupplierCode() != null && s.getSupplierCode().toLowerCase().contains(kw))
                            || (s.getContact() != null && s.getContact().toLowerCase().contains(kw))
                            || (s.getPhone() != null && s.getPhone().contains(kw)))
                    .toList();
            int start = page * size;
            int end = Math.min(start + size, filtered.size());
            List<Supplier> pageContent = start < filtered.size() ? filtered.subList(start, end) : List.of();
            
            Map<String, Object> result = new HashMap<>();
            result.put("content", pageContent);
            result.put("totalElements", filtered.size());
            result.put("totalPages", (int) Math.ceil((double) filtered.size() / size));
            result.put("currentPage", page);
            result.put("pageSize", size);
            return ApiResponse.success(result);
        }
        
        Page<Supplier> supplierPage = supplierRepository.findByEnabledTrue(pageable);
        Map<String, Object> result = new HashMap<>();
        result.put("content", supplierPage.getContent());
        result.put("totalElements", supplierPage.getTotalElements());
        result.put("totalPages", supplierPage.getTotalPages());
        result.put("currentPage", supplierPage.getNumber());
        result.put("pageSize", supplierPage.getSize());
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<Object> createSupplier(@RequestBody Supplier supplier) {
        try {
            supplier.setId(null);
            supplier.setEnabled(true);
            supplier.setSupplierCode(generateCode("SP"));
            supplierRepository.save(supplier);
            return ApiResponse.success("供应商创建成功");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Object> updateSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        try {
            Supplier existing = supplierRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("供应商不存在"));
            existing.setName(supplier.getName());
            existing.setContact(supplier.getContact());
            existing.setPhone(supplier.getPhone());
            existing.setAddress(supplier.getAddress());
            existing.setRemark(supplier.getRemark());
            supplierRepository.save(existing);
            return ApiResponse.success("供应商更新成功");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> deleteSupplier(@PathVariable Long id) {
        try {
            Supplier supplier = supplierRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("供应商不存在"));
            supplier.setEnabled(false);
            supplierRepository.save(supplier);
            return ApiResponse.success("供应商删除成功");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    private String generateCode(String prefix) {
        return prefix + DT_FMT.format(LocalDateTime.now()) + String.format("%04d", SUPPLIER_SEQ.getAndIncrement() % 9999 + 1);
    }
}