package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.annotation.RequiresPermission;
import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.entity.User;
import com.example.smartvisionstock.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class UserController {

    @Autowired
    private UserService userService;

    @RequiresPermission("read")
    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> getUserList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long roleId) {
        try {
            Page<User> userPage = userService.getUserList(page, size, username, roleId);
            Map<String, Object> result = new HashMap<>();
            result.put("data", userPage.getContent());
            result.put("total", userPage.getTotalElements());
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/users/all")
    public ApiResponse<List<User>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ApiResponse.success(users);
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @GetMapping("/users/{id}")
    public ApiResponse<User> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            if (user != null) {
                return ApiResponse.success(user);
            } else {
                return ApiResponse.error(400, "用户不存在");
            }
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @RequiresPermission("create")
    @PostMapping("/users")
    public ApiResponse<User> createUser(@RequestBody User user) {
        try {
            User created = userService.createUser(user);
            return ApiResponse.success("用户创建成功", created);
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @RequiresPermission("update")
    @PutMapping("/users/{id}")
    public ApiResponse<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            User updated = userService.updateUser(id, user);
            return ApiResponse.success("用户更新成功", updated);
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @PutMapping("/users/{id}/profile")
    public ApiResponse<Object> updateProfile(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            User user = userService.getUserById(id);
            if (user == null) {
                return ApiResponse.error(404, "用户不存在");
            }
            if (request.containsKey("realName")) user.setRealName(request.get("realName"));
            if (request.containsKey("email")) user.setEmail(request.get("email"));
            if (request.containsKey("phone")) user.setPhone(request.get("phone"));
            userService.update(user);
            return ApiResponse.success("个人信息更新成功");
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @RequiresPermission("delete")
    @DeleteMapping("/users/{id}")
    public ApiResponse<Object> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ApiResponse.success("用户删除成功");
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @PostMapping("/users/{id}/password/change")
    public ApiResponse<Object> changePassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            userService.changePassword(id, request.get("oldPassword"), request.get("newPassword"));
            return ApiResponse.success("密码修改成功");
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @RequiresPermission("ADMIN")
    @PostMapping("/users/{id}/password/reset")
    public ApiResponse<Object> resetPassword(@PathVariable Long id) {
        try {
            String newPassword = userService.resetPassword(id);
            return ApiResponse.success("密码已重置，新密码为：" + newPassword + "（请告知用户及时修改）");
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

    @RequiresPermission("update")
    @PutMapping("/users/{id}/status")
    public ApiResponse<Object> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        try {
            userService.updateStatus(id, request.get("status"));
            return ApiResponse.success("用户状态更新成功");
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }
}