package com.example.smartvisionstock.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenBlacklist 内存黑名单单元测试
 */
@DisplayName("TokenBlacklist 黑名单测试")
class TokenBlacklistTest {

    private InMemoryTokenBlacklist tokenBlacklist;

    @BeforeEach
    void setUp() {
        tokenBlacklist = new InMemoryTokenBlacklist();
    }

    @Test
    @DisplayName("黑名单添加并检查 - 未过期Token应被拦截")
    void testAddAndCheckBlacklisted() {
        String jti = "test-jti-001";
        long futureExpiry = System.currentTimeMillis() + 3600000; // 1小时后过期

        tokenBlacklist.add(jti, futureExpiry);
        assertTrue(tokenBlacklist.isBlacklisted(jti), "未过期的黑名单Token应被拦截");
    }

    @Test
    @DisplayName("过期Token自动从黑名单移除")
    void testExpiredTokenAutoRemoved() {
        String jti = "test-jti-002";
        long pastExpiry = System.currentTimeMillis() - 1000; // 已过期

        tokenBlacklist.add(jti, pastExpiry);
        assertFalse(tokenBlacklist.isBlacklisted(jti), "已过期的Token应自动移除");
    }

    @Test
    @DisplayName("null jti 不应被视为黑名单")
    void testNullJtiNotBlacklisted() {
        assertFalse(tokenBlacklist.isBlacklisted(null), "null jti 不应被拦截");
    }

    @Test
    @DisplayName("不存在于黑名单中的Token不应被拦截")
    void testNonExistentToken() {
        assertFalse(tokenBlacklist.isBlacklisted("non-existent-jti"), "不在黑名单中的Token不应被拦截");
    }

    @Test
    @DisplayName("定时清理过期的黑名单条目")
    void testCleanExpiredEntries() {
        // 添加过期条目
        tokenBlacklist.add("expired-1", System.currentTimeMillis() - 5000);
        tokenBlacklist.add("expired-2", System.currentTimeMillis() - 1000);

        // 添加未过期条目
        tokenBlacklist.add("active-1", System.currentTimeMillis() + 3600000);

        // 执行清理
        tokenBlacklist.cleanExpired();

        // 过期条目应被清除
        assertFalse(tokenBlacklist.isBlacklisted("expired-1"), "过期条目应被清理");
        assertFalse(tokenBlacklist.isBlacklisted("expired-2"), "过期条目应被清理");

        // 未过期条目应保留
        assertTrue(tokenBlacklist.isBlacklisted("active-1"), "未过期条目应保留");
    }

    @Test
    @DisplayName("多个不同jti的Token独立管理")
    void testMultipleTokensIndependence() {
        long future = System.currentTimeMillis() + 3600000;

        tokenBlacklist.add("jti-a", future);
        tokenBlacklist.add("jti-b", future);

        assertTrue(tokenBlacklist.isBlacklisted("jti-a"), "jti-a 应被拦截");
        assertTrue(tokenBlacklist.isBlacklisted("jti-b"), "jti-b 应被拦截");
        assertFalse(tokenBlacklist.isBlacklisted("jti-c"), "jti-c 不受影响");

        // 过期 jti-a
        tokenBlacklist.add("jti-a", System.currentTimeMillis() - 1000);
        assertFalse(tokenBlacklist.isBlacklisted("jti-a"), "jti-a 应已过期");
        assertTrue(tokenBlacklist.isBlacklisted("jti-b"), "jti-b 仍应有效");
    }
}
