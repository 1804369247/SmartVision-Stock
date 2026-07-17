package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.entity.User;
import com.example.smartvisionstock.repository.UserRepository;
import com.example.smartvisionstock.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl 用户服务层单元测试
 * 测试登录、注册、登录限制、密码管理等核心业务逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl 用户服务单元测试")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private static final String TEST_USERNAME = "admin";
    private static final String TEST_PASSWORD = "admin123";
    private static final String ENCODED_PASSWORD = "$2a$10$dummy_encoded_hash_value";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(TEST_USERNAME);
        testUser.setPassword(ENCODED_PASSWORD);
        testUser.setRole("ADMIN");
        testUser.setRealName("管理员");
        testUser.setEmail("admin@test.com");
        testUser.setEnabled(true);
        testUser.setCreateTime(LocalDateTime.now());
        testUser.setLoginAttempts(0);
        testUser.setLockUntil(null);
    }

    // ==================== 登录测试 ====================
    @Nested
    @DisplayName("用户登录 login()")
    class LoginTests {

        @Test
        @DisplayName("登录成功 - 正确的用户名和密码")
        void testLoginSuccess() {
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

            User result = userService.login(TEST_USERNAME, TEST_PASSWORD);
            assertNotNull(result);
            assertEquals(TEST_USERNAME, result.getUsername());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("登录失败 - 用户不存在")
        void testLoginUserNotFound() {
            when(userRepository.findByUsername("nonexist")).thenReturn(Optional.empty());

            User result = userService.login("nonexist", "any_password");
            assertNull(result, "不存在的用户应返回null");
        }

        @Test
        @DisplayName("登录失败 - 用户被禁用")
        void testLoginDisabledUser() {
            testUser.setEnabled(false);
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

            User result = userService.login(TEST_USERNAME, TEST_PASSWORD);
            assertNull(result, "禁用的用户应返回null");
        }
    }

    // ==================== 登录限制测试 ====================
    @Nested
    @DisplayName("登录失败限制 checkLoginAttempts/increaseLoginAttempts")
    class LoginAttemptsTests {

        @Test
        @DisplayName("检查登录次数 - 无限制")
        void testCheckLoginAttemptsNoLock() {
            testUser.setLockUntil(null);
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

            String result = userService.checkLoginAttempts(TEST_USERNAME);
            assertNull(result, "未锁定的用户应返回null");
        }

        @Test
        @DisplayName("检查登录次数 - 账号已锁定")
        void testCheckLoginAttemptsLocked() {
            testUser.setLockUntil(LocalDateTime.now().plusMinutes(10));
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

            String result = userService.checkLoginAttempts(TEST_USERNAME);
            assertNotNull(result, "锁定的用户应返回锁定信息");
            assertTrue(result.contains("账号已锁定"), "应包含锁定提示");
        }

        @Test
        @DisplayName("检查登录次数 - 不存在的用户有延迟但不锁定")
        void testCheckLoginAttemptsNonexistent() {
            when(userRepository.findByUsername("hacker")).thenReturn(Optional.empty());

            long start = System.currentTimeMillis();
            String result = userService.checkLoginAttempts("hacker");
            long elapsed = System.currentTimeMillis() - start;

            assertNull(result, "不存在的用户应返回null");
            assertTrue(elapsed >= 50, "不存在的用户应有延迟（防止枚举）");
        }

        @Test
        @DisplayName("增加登录失败次数")
        void testIncreaseLoginAttempts() {
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            int remaining = userService.increaseLoginAttempts(TEST_USERNAME);
            assertEquals(4, remaining, "首次失败后剩余4次");
        }

        @Test
        @DisplayName("登录失败达上限后锁定")
        void testLoginAttemptsLockAccount() {
            testUser.setLoginAttempts(4); // 已失败4次
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            int remaining = userService.increaseLoginAttempts(TEST_USERNAME);
            assertEquals(0, remaining, "第5次失败后剩余0次");
        }

        @Test
        @DisplayName("重置登录失败次数")
        void testResetLoginAttempts() {
            testUser.setLoginAttempts(3);
            testUser.setLockUntil(LocalDateTime.now().plusMinutes(5));
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

            userService.resetLoginAttempts(TEST_USERNAME);

            assertEquals(0, testUser.getLoginAttempts());
            assertNull(testUser.getLockUntil());
            verify(userRepository).save(testUser);
        }
    }

    // ==================== 注册测试 ====================
    @Nested
    @DisplayName("用户注册 register()")
    class RegisterTests {

        @Test
        @DisplayName("注册成功")
        void testRegisterSuccess() {
            User newUser = new User();
            newUser.setUsername("newuser");
            newUser.setPassword("newPassword123");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(2L);
                return u;
            });

            User result = userService.register(newUser);
            assertNotNull(result);
            assertEquals("newuser", result.getUsername());
            assertEquals(ENCODED_PASSWORD, result.getPassword());
        }

        @Test
        @DisplayName("注册失败 - 用户名已存在")
        void testRegisterDuplicateUsername() {
            User newUser = new User();
            newUser.setUsername(TEST_USERNAME);
            newUser.setPassword("Password123");

            when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(true);

            assertThrows(RuntimeException.class, () -> userService.register(newUser),
                    "重复用户名应抛出异常");
        }

        @Test
        @DisplayName("注册默认角色为USER")
        void testRegisterDefaultRole() {
            User newUser = new User();
            newUser.setUsername("newuser");
            newUser.setPassword("Password123");
            newUser.setRole(null); // 未指定角色

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.register(newUser);
            assertEquals("USER", result.getRole(), "未指定角色时应默认为USER");
        }
    }

    // ==================== 用户查询测试 ====================
    @Nested
    @DisplayName("用户查询")
    class QueryTests {

        @Test
        @DisplayName("按用户名查找")
        void testFindByUsername() {
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

            User result = userService.findByUsername(TEST_USERNAME);
            assertNotNull(result);
            assertEquals(TEST_USERNAME, result.getUsername());
        }

        @Test
        @DisplayName("按ID查找")
        void testFindById() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            User result = userService.findById(1L);
            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("根据ID查找不存在用户")
        void testFindByIdNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            User result = userService.findById(999L);
            assertNull(result);
        }
    }

    // ==================== 密码管理测试 ====================
    @Nested
    @DisplayName("密码管理")
    class PasswordTests {

        @Test
        @DisplayName("修改密码 - 用户不存在")
        void testChangePasswordUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> userService.changePassword(999L, "old", "new"));
        }

        @Test
        @DisplayName("重置密码 - 用户不存在")
        void testResetPasswordUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> userService.resetPassword(999L));
        }
    }

    // ==================== 权限检查测试 ====================
    @Nested
    @DisplayName("权限检查 checkPermission()")
    class PermissionTests {

        @Test
        @DisplayName("ADMIN角色拥有所有权限")
        void testAdminHasAllPermissions() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            assertTrue(userService.checkPermission(1L, "delete"));
            assertTrue(userService.checkPermission(1L, "read"));
            assertTrue(userService.checkPermission(1L, "update"));
            assertTrue(userService.checkPermission(1L, "unknown"));
        }

        @Test
        @DisplayName("OPERATOR角色权限受限")
        void testOperatorLimitedPermissions() {
            User operator = new User();
            operator.setId(2L);
            operator.setUsername("operator");
            operator.setRole("OPERATOR");

            when(userRepository.findById(2L)).thenReturn(Optional.of(operator));

            assertTrue(userService.checkPermission(2L, "read"));
            assertTrue(userService.checkPermission(2L, "update"));
            assertFalse(userService.checkPermission(2L, "delete"), "OPERATOR无delete权限");
        }

        @Test
        @DisplayName("USER角色最小权限")
        void testUserMinimalPermissions() {
            User user = new User();
            user.setId(3L);
            user.setUsername("user");
            user.setRole("USER");

            when(userRepository.findById(3L)).thenReturn(Optional.of(user));

            assertTrue(userService.checkPermission(3L, "read"));
            assertTrue(userService.checkPermission(3L, "view"));
            assertFalse(userService.checkPermission(3L, "update"), "USER无update权限");
            assertFalse(userService.checkPermission(3L, "delete"), "USER无delete权限");
        }

        @Test
        @DisplayName("不存在用户的权限检查")
        void testNonExistentUserPermission() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            assertFalse(userService.checkPermission(999L, "read"));
        }
    }

    // ==================== 状态管理测试 ====================
    @Nested
    @DisplayName("用户状态管理")
    class StatusTests {

        @Test
        @DisplayName("启用用户")
        void testEnableUser() {
            testUser.setEnabled(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            userService.updateStatus(1L, 1);

            assertTrue(testUser.getEnabled());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("禁用用户")
        void testDisableUser() {
            testUser.setEnabled(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            userService.updateStatus(1L, 0);

            assertFalse(testUser.getEnabled());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("更新不存在的用户状态")
        void testUpdateStatusUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> userService.updateStatus(999L, 1));
        }
    }
}
