package com.example.smartvisionstock.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单（Redis 实现）。
 * 仅在激活 redis profile 时生效。所有后端节点共享同一份黑名单，
 * 解决多节点部署下「节点A登出、节点B仍认为Token有效」的分布式一致性问题。
 */
@Component
@Profile("redis")
public class RedisTokenBlacklist implements TokenBlacklist {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenBlacklist.class);

    private static final String KEY_PREFIX = "svs:token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public RedisTokenBlacklist(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void add(String jti, long expirationMillis) {
        if (jti == null) {
            return;
        }
        long ttlSeconds = Math.max(1, (expirationMillis - System.currentTimeMillis()) / 1000);
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
        log.debug("Token 已加入 Redis 黑名单: jti={}, ttl={}s", jti, ttlSeconds);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        // 依赖 Redis 的 TTL 自动过期，无需手动清理
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
