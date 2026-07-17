package com.example.smartvisionstock.util;

/**
 * Token 黑名单抽象。
 * 默认（非 redis profile）使用内存实现 InMemoryTokenBlacklist；
 * 当激活 redis profile 时切换到 RedisTokenBlacklist，实现多节点共享、分布式一致。
 */
public interface TokenBlacklist {

    /**
     * 将 Token 加入黑名单
     * @param jti Token 唯一标识
     * @param expirationMillis Token 原生过期时间（毫秒时间戳），用于设置黑名单存活时长
     */
    void add(String jti, long expirationMillis);

    /**
     * 检查 Token 是否在黑名单中
     */
    boolean isBlacklisted(String jti);
}
