package com.example.smartvisionstock.service;

import com.example.smartvisionstock.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface UserService {

    User login(String username, String password);

    User register(User user);

    User findByUsername(String username);

    User findById(Long id);

    List<User> findAll();

    User update(User user);

    void deleteById(Long id);

    boolean checkPermission(Long userId, String permission);

    Page<User> getUserList(int page, int size, String username, Long roleId);

    List<User> getAllUsers();

    User getUserById(Long id);

    User createUser(User user);

    User updateUser(Long id, User user);

    void deleteUser(Long id);

    void changePassword(Long id, String oldPassword, String newPassword);

    String resetPassword(Long id);

    void updateStatus(Long id, Integer status);

    // 登录失败限制相关方法
    String checkLoginAttempts(String username);
    
    int increaseLoginAttempts(String username);
    
    void resetLoginAttempts(String username);
}