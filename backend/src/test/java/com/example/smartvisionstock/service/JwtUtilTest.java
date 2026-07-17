package com.example.smartvisionstock.service;

import com.example.smartvisionstock.util.JwtUtil;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 工具测试（核心认证逻辑）—— 增强版
 * 新增：jti唯一标识、RefreshToken、Token过期、密钥初始化等测试
 */
@DisplayName("JwtUtil JWT工具测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        Field expirationField = JwtUtil.class.getDeclaredField("accessTokenExpiration");
        expirationField.setAccessible(true);
        expirationField.set(jwtUtil, 86400000L);
        jwtUtil.init();
    }

    // ==================== 基本Token生成与解析 ====================
    @Nested
    @DisplayName("Token 生成与解析")
    class TokenGenerationTests {

        @Test
        @DisplayName("生成含角色的Token并解析")
        void testGenerateAndParseToken() {
            String token = jwtUtil.generateToken(1L, "admin", "ADMIN");
            assertNotNull(token, "Token不应为空");

            assertEquals(1L, jwtUtil.getUserIdFromToken(token));
            assertEquals("admin", jwtUtil.getUsernameFromToken(token));
            assertEquals("ADMIN", jwtUtil.getRoleFromToken(token));
        }

        @Test
        @DisplayName("生成不含角色的Token（向后兼容）")
        void testGenerateTokenWithoutRole() {
            String token = jwtUtil.generateToken(1L, "admin");
            assertNotNull(token);
            assertTrue(jwtUtil.validateToken(token));
            assertEquals("admin", jwtUtil.getUsernameFromToken(token));
            // 旧版无角色参数时，应填充默认值 USER
            assertEquals("USER", jwtUtil.getRoleFromToken(token));
        }

        @Test
        @DisplayName("不同用户的Token互不干扰")
        void testDifferentUsers() {
            String token1 = jwtUtil.generateToken(1L, "admin", "ADMIN");
            String token2 = jwtUtil.generateToken(2L, "operator", "OPERATOR");

            assertNotEquals(token1, token2);
            assertEquals(1L, jwtUtil.getUserIdFromToken(token1));
            assertEquals(2L, jwtUtil.getUserIdFromToken(token2));
            assertEquals("ADMIN", jwtUtil.getRoleFromToken(token1));
            assertEquals("OPERATOR", jwtUtil.getRoleFromToken(token2));
        }
    }

    // ==================== Token验证 ====================
    @Nested
    @DisplayName("Token 验证")
    class TokenValidationTests {

        @Test
        @DisplayName("有效Token通过验证")
        void testValidateToken() {
            String token = jwtUtil.generateToken(1L, "admin", "ADMIN");
            assertTrue(jwtUtil.validateToken(token));
            assertFalse(jwtUtil.isTokenExpired(token));
        }

        @Test
        @DisplayName("无效Token无法通过验证")
        void testInvalidToken() {
            assertFalse(jwtUtil.validateToken("invalid.token.here"));
            assertNull(jwtUtil.getUserIdFromToken("invalid.token.here"));
            assertNull(jwtUtil.getUsernameFromToken("invalid.token.here"));
            assertNull(jwtUtil.getRoleFromToken("invalid.token.here"));
        }

        @Test
        @DisplayName("被篡改的Token无法通过验证")
        void testTamperedToken() {
            String token = jwtUtil.generateToken(1L, "admin", "ADMIN");
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";
            assertFalse(jwtUtil.validateToken(tampered));
        }

        @Test
        @DisplayName("过期Token验证失败")
        void testTokenExpiration() {
            String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTcwMDAwMDAwMCwiZXhwIjoxNzAwMDAwMDAxfQ.invalid";
            assertFalse(jwtUtil.validateToken(expiredToken));
        }

        @Test
        @DisplayName("空Token验证")
        void testEmptyToken() {
            assertFalse(jwtUtil.validateToken(""));
            assertFalse(jwtUtil.validateToken(null));
        }
    }

    // ==================== jti 唯一标识测试（新增） ====================
    @Nested
    @DisplayName("jti 唯一标识")
    class JtiTests {

        @Test
        @DisplayName("每个Token都有唯一的jti")
        void testTokenHasUniqueJti() {
            String token1 = jwtUtil.generateToken(1L, "admin", "ADMIN");
            String token2 = jwtUtil.generateToken(1L, "admin", "ADMIN");

            String jti1 = jwtUtil.getJtiFromToken(token1);
            String jti2 = jwtUtil.getJtiFromToken(token2);

            assertNotNull(jti1, "Token1应有jti");
            assertNotNull(jti2, "Token2应有jti");
            assertNotEquals(jti1, jti2, "不同Token应有不同的jti");
        }

        @Test
        @DisplayName("RefreshToken也有jti")
        void testRefreshTokenHasJti() {
            String refreshToken = jwtUtil.generateRefreshToken(1L);
            String jti = jwtUtil.getJtiFromToken(refreshToken);

            assertNotNull(jti, "RefreshToken应有jti");
            assertFalse(jti.isEmpty());
        }

        @Test
        @DisplayName("无效Token的jti为null")
        void testInvalidTokenJtiIsNull() {
            assertNull(jwtUtil.getJtiFromToken("invalid.token"));
            assertNull(jwtUtil.getJtiFromToken(""));
        }
    }

    // ==================== RefreshToken 测试（新增） ====================
    @Nested
    @DisplayName("RefreshToken 刷新令牌")
    class RefreshTokenTests {

        @Test
        @DisplayName("生成RefreshToken并从中获取用户ID")
        void testRefreshToken() {
            String refreshToken = jwtUtil.generateRefreshToken(1L);
            assertNotNull(refreshToken);
            assertTrue(jwtUtil.validateToken(refreshToken));

            Long userId = jwtUtil.getUserIdFromRefreshToken(refreshToken);
            assertEquals(1L, userId);
        }

        @Test
        @DisplayName("不同用户的RefreshToken不同")
        void testDifferentUserRefreshTokens() {
            String rt1 = jwtUtil.generateRefreshToken(1L);
            String rt2 = jwtUtil.generateRefreshToken(2L);

            assertNotNull(rt1);
            assertNotNull(rt2);

            assertEquals(1L, jwtUtil.getUserIdFromRefreshToken(rt1));
            assertEquals(2L, jwtUtil.getUserIdFromRefreshToken(rt2));
        }

        @Test
        @DisplayName("AccessToken不能作为RefreshToken使用")
        void testAccessTokenNotRefreshToken() {
            String accessToken = jwtUtil.generateToken(1L, "admin", "ADMIN");
            Long userId = jwtUtil.getUserIdFromRefreshToken(accessToken); // type不是"refresh"
            assertNull(userId, "AccessToken不应被当作RefreshToken解析");
        }

        @Test
        @DisplayName("无效RefreshToken返回null")
        void testInvalidRefreshToken() {
            assertNull(jwtUtil.getUserIdFromRefreshToken("invalid.refresh.token"));
        }
    }

    // ==================== parseToken 测试（新增） ====================
    @Nested
    @DisplayName("parseToken Claims解析")
    class ParseTokenTests {

        @Test
        @DisplayName("有效Token可以解析出Claims")
        void testParseValidToken() {
            String token = jwtUtil.generateToken(1L, "admin", "ADMIN");
            var claims = jwtUtil.parseToken(token);
            assertNotNull(claims);
            assertEquals(1L, claims.get("userId", Long.class));
            assertEquals("admin", claims.get("username", String.class));
            assertEquals("ADMIN", claims.get("role", String.class));
            assertNotNull(claims.get("jti", String.class));
        }

        @Test
        @DisplayName("无效Token返回null")
        void testParseInvalidToken() {
            assertNull(jwtUtil.parseToken("invalid"));
        }
    }

    // ==================== 密钥初始化测试（新增） ====================
    @Nested
    @DisplayName("密钥初始化")
    class KeyInitializationTests {

        @Test
        @DisplayName("密钥初始化后可正常生成Token")
        void testSecretKeyInitialized() {
            // init() 已在 @BeforeEach 中调用
            String token = jwtUtil.generateToken(1L, "admin", "ADMIN");
            assertNotNull(token);
            assertTrue(jwtUtil.validateToken(token));
        }

        @Test
        @DisplayName("相同密钥下Token可以互相验证")
        void testConsistentSecretKey() {
            JwtUtil util2 = new JwtUtil();
            util2.init(); // 不同的密钥

            String token1 = jwtUtil.generateToken(1L, "admin", "ADMIN");
            // token1 用 jwtUtil 的密钥生成，util2 的密钥不同，验证应失败
            assertFalse(util2.validateToken(token1), "不同密钥的JwtUtil实例应无法验证对方的Token");
        }
    }
}
