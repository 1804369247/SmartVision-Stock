package com.example.smartvisionstock.controller;

import com.example.smartvisionstock.entity.User;
import com.example.smartvisionstock.service.UserService;
import com.example.smartvisionstock.util.JwtUtil;
import com.example.smartvisionstock.util.TokenBlacklist;
import com.example.smartvisionstock.util.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 用户管理接口测试
 * 测试用户 CRUD、密码管理、状态管理全流程
 */
@WebMvcTest(value = UserController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController 用户管理接口测试")
class UserControllerTest {

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

    private User testUser;
    private List<User> userList;

    @BeforeEach
    void setUp() {
        UserContext.setCurrentUser(1L, "admin", "ADMIN");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setPassword(null);
        testUser.setRole("ADMIN");
        testUser.setRealName("管理员");
        testUser.setEmail("admin@test.com");
        testUser.setPhone("13800138000");
        testUser.setEnabled(true);
        testUser.setCreateTime(LocalDateTime.now());
        testUser.setLoginAttempts(0);

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("operator");
        user2.setPassword(null);
        user2.setRole("OPERATOR");
        user2.setRealName("操作员");
        user2.setEnabled(true);
        user2.setCreateTime(LocalDateTime.now());

        userList = Arrays.asList(testUser, user2);
    }

    // ==================== 用户列表查询 ====================
    @Nested
    @DisplayName("GET /api/system/users 用户列表")
    class GetUserListTests {

        @Test
        @DisplayName("分页获取用户列表")
        void testGetUserList() throws Exception {
            Page<User> page = new PageImpl<>(userList);
            when(userService.getUserList(0, 10, null, null)).thenReturn(page);

            mockMvc.perform(get("/api/system/users")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.data", hasSize(2)))
                    .andExpect(jsonPath("$.data.total").value(2))
                    .andExpect(jsonPath("$.data.data[0].username").value("admin"));
        }

        @Test
        @DisplayName("按用户名搜索")
        void testGetUserListByUsername() throws Exception {
            Page<User> page = new PageImpl<>(List.of(testUser));
            when(userService.getUserList(0, 10, "admin", null)).thenReturn(page);

            mockMvc.perform(get("/api/system/users")
                            .param("page", "0")
                            .param("size", "10")
                            .param("username", "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.data", hasSize(1)))
                    .andExpect(jsonPath("$.data.data[0].username").value("admin"));
        }

        @Test
        @DisplayName("空用户列表")
        void testGetUserListEmpty() throws Exception {
            Page<User> emptyPage = new PageImpl<>(Collections.emptyList());
            when(userService.getUserList(0, 10, null, null)).thenReturn(emptyPage);

            mockMvc.perform(get("/api/system/users")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.data", hasSize(0)))
                    .andExpect(jsonPath("$.data.total").value(0));
        }
    }

    // ==================== 获取所有用户 ====================
    @Nested
    @DisplayName("GET /api/system/users/all 所有用户")
    class GetAllUsersTests {

        @Test
        @DisplayName("获取所有用户")
        void testGetAllUsers() throws Exception {
            when(userService.getAllUsers()).thenReturn(userList);

            mockMvc.perform(get("/api/system/users/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }
    }

    // ==================== 获取单个用户 ====================
    @Nested
    @DisplayName("GET /api/system/users/{id} 单个用户")
    class GetUserByIdTests {

        @Test
        @DisplayName("获取存在的用户")
        void testGetUserById() throws Exception {
            when(userService.getUserById(1L)).thenReturn(testUser);

            mockMvc.perform(get("/api/system/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.username").value("admin"))
                    .andExpect(jsonPath("$.data.role").value("ADMIN"));
        }

        @Test
        @DisplayName("获取不存在的用户")
        void testGetUserByIdNotFound() throws Exception {
            when(userService.getUserById(999L)).thenReturn(null);

            mockMvc.perform(get("/api/system/users/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("用户不存在"));
        }
    }

    // ==================== 创建用户 ====================
    @Nested
    @DisplayName("POST /api/system/users 创建用户")
    class CreateUserTests {

        @Test
        @DisplayName("创建用户成功")
        void testCreateUser() throws Exception {
            User newUser = new User();
            newUser.setUsername("newuser");
            newUser.setPassword("NewPass123");
            newUser.setRole("OPERATOR");
            newUser.setRealName("新用户");

            User saved = new User();
            saved.setId(3L);
            saved.setUsername("newuser");
            saved.setRole("OPERATOR");
            saved.setRealName("新用户");

            when(userService.createUser(any(User.class))).thenReturn(saved);

            mockMvc.perform(post("/api/system/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("用户创建成功"))
                    .andExpect(jsonPath("$.data.username").value("newuser"));
        }

        @Test
        @DisplayName("创建用户失败 - 用户名已存在")
        void testCreateUserDuplicate() throws Exception {
            User newUser = new User();
            newUser.setUsername("admin");
            newUser.setPassword("Pass12345");

            when(userService.createUser(any(User.class)))
                    .thenThrow(new IllegalArgumentException("用户名已存在"));

            mockMvc.perform(post("/api/system/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("用户名已存在"));
        }
    }

    // ==================== 更新用户 ====================
    @Nested
    @DisplayName("PUT /api/system/users/{id} 更新用户")
    class UpdateUserTests {

        @Test
        @DisplayName("更新用户成功")
        void testUpdateUser() throws Exception {
            User update = new User();
            update.setUsername("admin");
            update.setRole("MANAGER");
            update.setRealName("管理员更新");

            User updated = new User();
            updated.setId(1L);
            updated.setUsername("admin");
            updated.setRole("MANAGER");
            updated.setRealName("管理员更新");

            when(userService.updateUser(eq(1L), any(User.class))).thenReturn(updated);

            mockMvc.perform(put("/api/system/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("用户更新成功"));
        }

        @Test
        @DisplayName("更新用户失败 - 用户不存在")
        void testUpdateUserNotFound() throws Exception {
            User update = new User();
            update.setUsername("nonexist");

            when(userService.updateUser(eq(999L), any(User.class)))
                    .thenThrow(new IllegalArgumentException("用户不存在"));

            mockMvc.perform(put("/api/system/users/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }
    }

    // ==================== 删除用户 ====================
    @Nested
    @DisplayName("DELETE /api/system/users/{id} 删除用户")
    class DeleteUserTests {

        @Test
        @DisplayName("删除用户成功")
        void testDeleteUser() throws Exception {
            mockMvc.perform(delete("/api/system/users/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("用户删除成功"));

            verify(userService).deleteUser(2L);
        }

        @Test
        @DisplayName("删除用户失败 - 用户不存在")
        void testDeleteUserNotFound() throws Exception {
            doThrow(new IllegalArgumentException("用户不存在")).when(userService).deleteUser(999L);

            mockMvc.perform(delete("/api/system/users/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }
    }

    // ==================== 修改密码 ====================
    @Nested
    @DisplayName("POST /api/system/users/{id}/password/change 修改密码")
    class ChangePasswordTests {

        @Test
        @DisplayName("修改密码成功")
        void testChangePassword() throws Exception {
            Map<String, String> body = new HashMap<>();
            body.put("oldPassword", "OldPass123");
            body.put("newPassword", "NewPass456");

            mockMvc.perform(post("/api/system/users/1/password/change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("密码修改成功"));

            verify(userService).changePassword(1L, "OldPass123", "NewPass456");
        }

        @Test
        @DisplayName("修改密码失败 - 旧密码错误")
        void testChangePasswordWrongOld() throws Exception {
            doThrow(new IllegalArgumentException("旧密码错误"))
                    .when(userService).changePassword(eq(1L), eq("WrongOld"), anyString());

            Map<String, String> body = new HashMap<>();
            body.put("oldPassword", "WrongOld");
            body.put("newPassword", "NewPass456");

            mockMvc.perform(post("/api/system/users/1/password/change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("旧密码错误"));
        }

        @Test
        @DisplayName("修改密码失败 - 用户不存在")
        void testChangePasswordUserNotFound() throws Exception {
            doThrow(new IllegalArgumentException("用户不存在"))
                    .when(userService).changePassword(eq(999L), anyString(), anyString());

            Map<String, String> body = new HashMap<>();
            body.put("oldPassword", "OldPass123");
            body.put("newPassword", "NewPass456");

            mockMvc.perform(post("/api/system/users/999/password/change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }
    }

    // ==================== 重置密码 ====================
    @Nested
    @DisplayName("POST /api/system/users/{id}/password/reset 重置密码")
    class ResetPasswordTests {

        @Test
        @DisplayName("重置密码成功")
        void testResetPassword() throws Exception {
            mockMvc.perform(post("/api/system/users/1/password/reset"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(userService).resetPassword(1L);
        }
    }

    // ==================== 更新用户状态 ====================
    @Nested
    @DisplayName("PUT /api/system/users/{id}/status 更新状态")
    class UpdateStatusTests {

        @Test
        @DisplayName("启用用户")
        void testEnableUser() throws Exception {
            Map<String, Integer> body = new HashMap<>();
            body.put("status", 1);

            mockMvc.perform(put("/api/system/users/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(userService).updateStatus(1L, 1);
        }

        @Test
        @DisplayName("禁用用户")
        void testDisableUser() throws Exception {
            Map<String, Integer> body = new HashMap<>();
            body.put("status", 0);

            mockMvc.perform(put("/api/system/users/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(userService).updateStatus(1L, 0);
        }
    }
}
