package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.entity.User;
import com.example.smartvisionstock.repository.UserRepository;
import com.example.smartvisionstock.service.UserService;
import com.example.smartvisionstock.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Value("${app.default-reset-password:reset1234}")
    private String defaultResetPassword;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 登录失败限制常量
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    @Override
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null && user.getEnabled() && passwordEncoder.matches(password, user.getPassword())) {
            // 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);
            return user;
        }
        return null;
    }

    @Override
    public String checkLoginAttempts(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            // 对不存在的用户也加入少量延迟，防止用户名枚举攻击
            try {
                Thread.sleep(100 + (long)(Math.random() * 200));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return null; // 用户不存在，不阻止登录尝试
        }
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            long minutesLeft = java.time.Duration.between(LocalDateTime.now(), user.getLockUntil()).toMinutes();
            return "账号已锁定，请在 " + minutesLeft + " 分钟后重试";
        }
        return null; // 没有锁定，可以登录
    }

    @Override
    public int increaseLoginAttempts(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return MAX_LOGIN_ATTEMPTS - 1;
        }
        
        int attempts = user.getLoginAttempts() == null ? 1 : user.getLoginAttempts() + 1;
        user.setLoginAttempts(attempts);
        
        // 如果达到最大尝试次数，锁定账户
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            user.setLockUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
        }
        
        userRepository.save(user);
        return MAX_LOGIN_ATTEMPTS - attempts;
    }

    @Override
    public void resetLoginAttempts(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            user.setLoginAttempts(0);
            user.setLockUntil(null);
            userRepository.save(user);
        }
    }

    @Override
    public User register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }
        user.setCreateTime(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User update(User user) {
        User existing = userRepository.findById(user.getId()).orElse(null);
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getRole() != null) {
            existing.setRole(user.getRole());
        }
        if (user.getEnabled() != null) {
            existing.setEnabled(user.getEnabled());
        }
        return userRepository.save(existing);
    }

    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public boolean checkPermission(Long userId, String permission) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        String role = user.getRole();
        if ("ADMIN".equals(role)) {
            return true;
        }
        Map<String, List<String>> rolePermissions = new HashMap<>();
        rolePermissions.put("USER", List.of("read", "view"));
        rolePermissions.put("OPERATOR", List.of("read", "view", "update"));
        rolePermissions.put("MANAGER", List.of("read", "view", "update", "delete"));

        List<String> permissions = rolePermissions.get(role);
        return permissions != null && permissions.contains(permission);
    }

    @Override
    public Page<User> getUserList(int page, int size, String username, Long roleId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<User> result;
        if (username != null && !username.isEmpty()) {
            result = userRepository.findByUsernameContainingIgnoreCase(username, pageable);
        } else {
            result = userRepository.findAll(pageable);
        }
        // 清空密码字段，防止BCrypt哈希值泄露给前端
        result.getContent().forEach(u -> u.setPassword(null));
        return result;
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createTime"));
        // 清空密码字段
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    @Override
    public User getUserById(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setPassword(null);  // 禁止密码哈希泄露
        }
        return user;
    }

    @Override
    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole("USER");
        }
        user.setEnabled(true);
        user.setCreateTime(LocalDateTime.now());
        User saved = userRepository.save(user);
        saved.setPassword(null);  // 禁止密码哈希泄露
        return saved;
    }

    @Override
    public User updateUser(Long id, User user) {
        User existing = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (!existing.getUsername().equals(user.getUsername()) && userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        existing.setUsername(user.getUsername());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        existing.setRole(user.getRole());
        existing.setEnabled(user.getEnabled());
        User saved = userRepository.save(existing);
        saved.setPassword(null);  // 禁止密码哈希泄露
        return saved;
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("用户不存在");
        }
        userRepository.deleteById(id);
    }

    @Override
    public void changePassword(Long id, String oldPassword, String newPassword) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public String resetPassword(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setPassword(passwordEncoder.encode(defaultResetPassword));
        userRepository.save(user);
        return defaultResetPassword;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setEnabled(status == 1);
        userRepository.save(user);
    }
}