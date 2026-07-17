package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.annotation.RequiresPermission;
import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.entity.Role;
import com.example.smartvisionstock.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping("/roles")
    public ApiResponse<Map<String, Object>> getRoleList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name) {
        try {
            Page<Role> rolePage = roleService.getRoleList(page, size, name);
            Map<String, Object> result = new HashMap<>();
            result.put("data", rolePage.getContent());
            result.put("total", rolePage.getTotalElements());
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/roles/all")
    public ApiResponse<List<Role>> getAllRoles() {
        try {
            List<Role> roles = roleService.getAllRoles();
            return ApiResponse.success(roles);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/roles/{id}")
    public ApiResponse<Role> getRoleById(@PathVariable Long id) {
        try {
            Role role = roleService.getRoleById(id);
            if (role != null) {
                return ApiResponse.success(role);
            } else {
                return ApiResponse.error(400, "角色不存在");
            }
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @RequiresPermission("create")
    @PostMapping("/roles")
    public ApiResponse<Role> createRole(@RequestBody Role role) {
        try {
            Role created = roleService.createRole(role);
            return ApiResponse.success("角色创建成功", created);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @RequiresPermission("update")
    @PutMapping("/roles/{id}")
    public ApiResponse<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        try {
            Role updated = roleService.updateRole(id, role);
            return ApiResponse.success("角色更新成功", updated);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @RequiresPermission("delete")
    @DeleteMapping("/roles/{id}")
    public ApiResponse<Object> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return ApiResponse.success("角色删除成功");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/roles/{id}/status")
    public ApiResponse<Object> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        try {
            roleService.updateStatus(id, request.get("status"));
            return ApiResponse.success("角色状态更新成功");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}