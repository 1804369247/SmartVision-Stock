package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.example.smartvisionstock.entity.User;
import com.example.smartvisionstock.repository.UserRepository;
import com.example.smartvisionstock.service.UserService;
import com.example.smartvisionstock.util.JwtUtil;
import com.example.smartvisionstock.util.TokenBlacklist;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证接口", description = "用户登录、注册、退出等认证操作")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户名密码登录，返回 JWT Token")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, Object> request) {

        log.debug("登录请求: {}", request);

        String username = (String) request.get("username");
        String password = (String) request.get("password");
        Boolean rememberMe = request.get("rememberMe") instanceof Boolean
                ? (Boolean) request.get("rememberMe")
                : false;

        if (username == null || username.trim().isEmpty()) {
            return ApiResponse.error(400, "请输入用户名");
        }
        if (password == null || password.isEmpty()) {
            return ApiResponse.error(400, "请输入密码");
        }

        username = username.trim();

        String lockReason = userService.checkLoginAttempts(username);
        if (lockReason != null) {
            log.warn("登录被拒绝（账号锁定）: username={}", username);
            return ApiResponse.error(423, lockReason);
        }

        User user = userService.login(username, password);
        if (user == null) {
            int remaining = userService.increaseLoginAttempts(username);
            String msg = remaining > 0 
                    ? String.format("用户名或密码错误，还可尝试 %d 次", remaining)
                    : "登录失败次数过多，账号已锁定 15 分钟";
            log.warn("登录失败: username={}, remainingAttempts={}", username, Math.max(0, remaining));
            return ApiResponse.error(400, msg);
        }

        userService.resetLoginAttempts(username);

        String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = rememberMe ? jwtUtil.generateRefreshToken(user.getId()) : null;

        user.setLastLoginTime(LocalDateTime.now());

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("role", user.getRole());
        userInfo.put("realName", user.getRealName() != null ? user.getRealName() : user.getUsername());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", accessToken);
        data.put("refreshToken", refreshToken);
        data.put("expiresIn", 86400);
        data.put("tokenType", "Bearer");
        data.put("user", userInfo);

        log.info("登录成功: username={}, role={}, rememberMe={}", username, user.getRole(), rememberMe);

        return ApiResponse.success("登录成功", data);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建新用户账户")
    public ApiResponse<Map<String, Object>> register(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().trim().length() < 3) {
            return ApiResponse.error(400, "用户名至少 3 个字符");
        }
        if (user.getPassword() == null || user.getPassword().length() < 8) {
            return ApiResponse.error(400, "密码至少 8 个字符，且需包含字母和数字");
        }

        try {
            User registered = userService.register(user);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userId", registered.getId());
            data.put("username", registered.getUsername());

            return ApiResponse.success("注册成功", data);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "使用 RefreshToken 换取新的 AccessToken（Rotation 机制）")
    public ApiResponse<Map<String, Object>> refreshToken(@RequestBody Map<String, Object> request) {
        String refreshToken = (String) request.get("refreshToken");

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ApiResponse.error(400, "刷新令牌不能为空");
        }

        String refreshJti = jwtUtil.getJtiFromToken(refreshToken);
        if (tokenBlacklist.isBlacklisted(refreshJti)) {
            log.warn("RefreshToken 重复使用（可能被盗用）: jti={}", refreshJti);
            return ApiResponse.error(401, "刷新令牌已被使用，请重新登录");
        }

        Long userId = jwtUtil.getUserIdFromRefreshToken(refreshToken);
        if (userId == null) {
            return ApiResponse.error(401, "刷新令牌无效或已过期");
        }

        User user = userService.getUserById(userId);
        if (user == null || !user.getEnabled()) {
            return ApiResponse.error(401, "用户不存在或已被禁用");
        }

        if (refreshJti != null) {
            Claims claims = jwtUtil.parseToken(refreshToken);
            long expiration = claims != null && claims.getExpiration() != null
                    ? claims.getExpiration().getTime()
                    : System.currentTimeMillis() + 604800000L;
            tokenBlacklist.add(refreshJti, expiration);
        }

        String newAccessToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", newAccessToken);
        data.put("refreshToken", newRefreshToken);
        data.put("expiresIn", 86400);
        data.put("tokenType", "Bearer");

        return ApiResponse.success("Token 刷新成功", data);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户", description = "根据 Token 获取当前登录用户信息")
    public ApiResponse<Map<String, Object>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未登录");
        }

        String token = authHeader.substring(7);

        String jti = jwtUtil.getJtiFromToken(token);
        if (tokenBlacklist.isBlacklisted(jti)) {
            return ApiResponse.error(401, "Token 已失效，请重新登录");
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return ApiResponse.error(401, "Token 无效");
        }

        User user = userService.getUserById(userId);
        if (user == null) {
            return ApiResponse.error(401, "用户不存在");
        }

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("role", user.getRole());
        userInfo.put("realName", user.getRealName());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", userInfo);

        return ApiResponse.success("ok", data);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码", description = "重置指定用户的密码为默认值（仅用于开发环境）")
    public ApiResponse<Object> resetPassword(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String newPassword = (String) request.get("password");
        
        if (username == null || username.trim().isEmpty()) {
            return ApiResponse.error(400, "请输入用户名");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return ApiResponse.error(400, "请输入新密码");
        }
        
        User user = userService.findByUsername(username.trim());
        if (user == null) {
            return ApiResponse.error(400, "用户不存在");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLoginAttempts(0);
        user.setLockUntil(null);
        userRepository.save(user);
        
        log.info("密码已重置: username={}", username);
        return ApiResponse.success("密码已重置");
    }

    @PostMapping("/logout")
    @Operation(summary = "用户退出", description = "退出登录，将当前 Token 加入黑名单")
    public ApiResponse<Object> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String jti = jwtUtil.getJtiFromToken(token);
            if (jti != null) {
                Claims claims = jwtUtil.parseToken(token);
                long expiration = claims != null && claims.getExpiration() != null
                        ? claims.getExpiration().getTime()
                        : System.currentTimeMillis() + 86400000L;
                tokenBlacklist.add(jti, expiration);
                log.debug("Token 已加入黑名单: jti={}", jti);
            }
        }

        return ApiResponse.success("已退出登录");
    }
}
