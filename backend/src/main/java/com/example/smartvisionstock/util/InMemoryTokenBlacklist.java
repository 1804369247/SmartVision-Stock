package com.example.smartvisionstock.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 黑名单（内存实现，重启后清空）。
 * 仅在未激活 redis profile 时生效，用于本地开发与演示。
 * 生产多节点部署请激活 redis profile 使用 RedisTokenBlacklist。
 */
@Component
@Profile("!redis")
public class InMemoryTokenBlacklist implements TokenBlacklist {

    private static final Logger log = LoggerFactory.getLogger(InMemoryTokenBlacklist.class);

    // key: token jti, value: 过期时间戳(ms)
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    @Override
    public void add(String jti, long expirationMillis) {
        blacklist.put(jti, expirationMillis);
        log.debug("Token 已加入内存黑名单: jti={}, expires={}", jti, expirationMillis);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        Long expires = blacklist.get(jti);
        if (expires == null) {
            return false;
        }
        // 已过期则自动移除
        if (System.currentTimeMillis() > expires) {
            blacklist.remove(jti);
            return false;
        }
        return true;
    }

    /**
     * 定时清理过期黑名单条目（每5分钟执行一次）
     */
    @Scheduled(fixedRate = 300000)
    public void cleanExpired() {
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> {
            if (now > entry.getValue()) {
                log.debug("清理过期黑名单: jti={}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}
