package com.example.smartvisionstock.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 Spring 声明式缓存（@Cacheable / @CacheEvict）。
 * 未激活 redis profile 时，Spring Boot 自动使用内存 ConcurrentMapCacheManager；
 * 激活 redis profile 时，由 RedisCacheConfig 提供 RedisCacheManager。
 */
@Configuration
@EnableCaching
public class CachingConfig {
}
