package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.entity.User;
import com.example.smartvisionstock.repository.UserRepository;
import com.example.smartvisionstock.service.UserService;
import com.example.smartvisionstock.util.JwtUtil;
import com.example.smartvisionstock.util.TokenBlacklist;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 集成测试（新响应格式）
 * 响应格式: { code: number, message: string, ...data }
 * - code 200 = 成功
 * - code 400 = 参数错误/密码错误
 * - code 423 = 账号锁定
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController 认证接口测试")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private TokenBlacklist tokenBlacklist;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "Password123";
    private static final String TEST_TOKEN = "test-access-token";
    private static final String TEST_REFRESH = "test-refresh-token";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(TEST_USERNAME);
        testUser.setPassword("$2a$10$hash");
        testUser.setRole("ADMIN");
        testUser.setRealName("测试管理员");
        testUser.setEnabled(true);
        testUser.setLoginAttempts(0);
    }

    // ==================== 登录测试 ====================
    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("登录成功 → code=200, 返回 token 和 user")
        void testLoginSuccess() throws Exception {
            when(userService.checkLoginAttempts(TEST_USERNAME)).thenReturn(null);
            when(userService.login(TEST_USERNAME, TEST_PASSWORD)).thenReturn(testUser);
            when(jwtUtil.generateToken(1L, TEST_USERNAME, "ADMIN")).thenReturn(TEST_TOKEN);
            when(jwtUtil.generateRefreshToken(1L)).thenReturn(TEST_REFRESH);

            Map<String, Object> body = new HashMap<>();
            body.put("username", TEST_USERNAME);
            body.put("password", TEST_PASSWORD);
            body.put("rememberMe", true);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("登录成功"))
                    .andExpect(jsonPath("$.data.token").value(TEST_TOKEN))
                    .andExpect(jsonPath("$.data.refreshToken").value(TEST_REFRESH))
                    .andExpect(jsonPath("$.data.expiresIn").value(86400))
                    .andExpect(jsonPath("$.data.user.username").value(TEST_USERNAME))
                    .andExpect(jsonPath("$.data.user.role").value("ADMIN"));

            verify(userService).resetLoginAttempts(TEST_USERNAME);
        }

        @Test
        @DisplayName("登录失败（密码错误）→ code=400")
        void testLoginFailWrongPassword() throws Exception {
            when(userService.checkLoginAttempts(TEST_USERNAME)).thenReturn(null);
            when(userService.login(TEST_USERNAME, "wrong")).thenReturn(null);
            when(userService.increaseLoginAttempts(TEST_USERNAME)).thenReturn(4);

            Map<String, Object> body = new HashMap<>();
            body.put("username", TEST_USERNAME);
            body.put("password", "wrong");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("还可尝试 4 次")));
        }

        @Test
        @DisplayName("登录失败（锁定）→ code=423")
        void testLoginFailLocked() throws Exception {
            when(userService.checkLoginAttempts(TEST_USERNAME))
                    .thenReturn("账号已锁定，请在 10 分钟后重试");

            Map<String, Object> body = new HashMap<>();
            body.put("username", TEST_USERNAME);
            body.put("password", TEST_PASSWORD);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(423))
                    .andExpect(jsonPath("$.message").value(containsString("锁定")));

            verify(userService, never()).login(anyString(), anyString());
        }

        @Test
        @DisplayName("登录失败（缺用户名）→ code=400")
        void testLoginFailNoUsername() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("password", TEST_PASSWORD);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("请输入用户名"));
        }

        @Test
        @DisplayName("登录失败（空密码）→ code=400")
        void testLoginFailNoPassword() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("username", TEST_USERNAME);
            body.put("password", "");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("请输入密码"));
        }

        @Test
        @DisplayName("不记住我 → refreshToken 为 null")
        void testLoginWithoutRememberMe() throws Exception {
            when(userService.checkLoginAttempts(TEST_USERNAME)).thenReturn(null);
            when(userService.login(TEST_USERNAME, TEST_PASSWORD)).thenReturn(testUser);
            when(jwtUtil.generateToken(anyLong(), anyString(), anyString())).thenReturn(TEST_TOKEN);

            Map<String, Object> body = new HashMap<>();
            body.put("username", TEST_USERNAME);
            body.put("password", TEST_PASSWORD);
            body.put("rememberMe", false);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
        }

        @Test
        @DisplayName("连续 5 次失败后锁定 → code=400, 提示锁定")
        void testLoginFailAfterMaxAttempts() throws Exception {
            when(userService.checkLoginAttempts(TEST_USERNAME)).thenReturn(null);
            when(userService.login(TEST_USERNAME, TEST_PASSWORD)).thenReturn(null);
            when(userService.increaseLoginAttempts(TEST_USERNAME)).thenReturn(0);

            Map<String, Object> body = new HashMap<>();
            body.put("username", TEST_USERNAME);
            body.put("password", TEST_PASSWORD);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("锁定")));
        }
    }

    // ==================== 注册测试 ====================
    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("注册成功 → code=200")
        void testRegisterSuccess() throws Exception {
            User newUser = new User();
            newUser.setUsername("newuser");
            newUser.setPassword("NewPass123");

            User saved = new User();
            saved.setId(2L);
            saved.setUsername("newuser");

            when(userService.register(any(User.class))).thenReturn(saved);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.userId").value(2))
                    .andExpect(jsonPath("$.data.username").value("newuser"));
        }

        @Test
        @DisplayName("注册失败（用户名过短）→ code=400")
        void testRegisterFailShortUsername() throws Exception {
            User newUser = new User();
            newUser.setUsername("ab");
            newUser.setPassword("Password123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("注册失败（密码过短）→ code=400")
        void testRegisterFailShortPassword() throws Exception {
            User newUser = new User();
            newUser.setUsername("validuser");
            newUser.setPassword("short");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    // ==================== Token 刷新测试 ====================
    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshTests {

        @Test
        @DisplayName("刷新成功 → code=200, 返回新 Token")
        void testRefreshSuccess() throws Exception {
            when(tokenBlacklist.isBlacklisted(anyString())).thenReturn(false);
            when(jwtUtil.getJtiFromToken(TEST_REFRESH)).thenReturn("jti-001");
            when(jwtUtil.getUserIdFromRefreshToken(TEST_REFRESH)).thenReturn(1L);
            when(userService.getUserById(1L)).thenReturn(testUser);
            when(jwtUtil.parseToken(TEST_REFRESH)).thenReturn(null);
            when(jwtUtil.generateToken(1L, TEST_USERNAME, "ADMIN")).thenReturn(TEST_TOKEN);
            when(jwtUtil.generateRefreshToken(1L)).thenReturn("new-refresh");

            Map<String, Object> body = new HashMap<>();
            body.put("refreshToken", TEST_REFRESH);

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.token").value(TEST_TOKEN))
                    .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"))
                    .andExpect(jsonPath("$.data.expiresIn").value(86400));
        }

        @Test
        @DisplayName("刷新失败（RefreshToken 已被使用）→ code=401")
        void testRefreshFailReuse() throws Exception {
            when(tokenBlacklist.isBlacklisted("jti-001")).thenReturn(true);
            when(jwtUtil.getJtiFromToken(TEST_REFRESH)).thenReturn("jti-001");

            Map<String, Object> body = new HashMap<>();
            body.put("refreshToken", TEST_REFRESH);

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }

        @Test
        @DisplayName("刷新失败（RefreshToken 为空）→ code=400")
        void testRefreshFailEmpty() throws Exception {
            Map<String, Object> body = new HashMap<>();
            body.put("refreshToken", "");

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    // ==================== 退出测试 ====================
    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("无 token 退出仍返回成功")
        void testLogoutWithoutToken() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("已退出登录"));
        }
    }
}
