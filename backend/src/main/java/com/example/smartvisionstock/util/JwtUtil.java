package com.example.smartvisionstock.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${app.jwt.secret:}")
    private String configSecret;

    @Value("${app.jwt.expiration:86400000}")
    private long accessTokenExpiration;

    private String secretKey;

    @PostConstruct
    public void init() {
        String envKey = System.getenv("JWT_SECRET");
        if (envKey != null && !envKey.trim().isEmpty()) {
            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(envKey.trim());
            } catch (IllegalArgumentException e) {
                decoded = envKey.trim().getBytes();
            }
            if (decoded.length >= 48) {
                this.secretKey = envKey.trim();
                log.info("JWT 密钥来源: 环境变量 JWT_SECRET");
                return;
            } else {
                log.error("JWT_SECRET 长度不足！HS384 要求至少 48 字节（Base64编码后约 64 字符）");
            }
        }

        if (this.configSecret != null && !this.configSecret.trim().isEmpty()) {
            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(this.configSecret.trim());
            } catch (IllegalArgumentException e) {
                decoded = this.configSecret.trim().getBytes();
            }
            if (decoded.length >= 48) {
                this.secretKey = this.configSecret.trim();
                log.info("JWT 密钥来源: 配置文件 app.jwt.secret");
                return;
            } else {
                log.error("app.jwt.secret 长度不足！HS384 要求至少 48 字节（Base64编码后约 64 字符）");
            }
        }

        String propKey = System.getProperty("jwt.secret");
        if (propKey != null && !propKey.trim().isEmpty()) {
            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(propKey.trim());
            } catch (IllegalArgumentException e) {
                decoded = propKey.trim().getBytes();
            }
            if (decoded.length >= 48) {
                this.secretKey = propKey.trim();
                log.info("JWT 密钥来源: 系统属性 jwt.secret");
                return;
            }
        }

        byte[] randomBytes = new byte[64];
        new SecureRandom().nextBytes(randomBytes);
        this.secretKey = Base64.getEncoder().encodeToString(randomBytes);
        log.warn("==============================================");
        log.warn("⚠ 未配置有效 JWT_SECRET！使用随机密钥（仅开发调试）");
        log.warn("⚠ 重启后所有 Token 失效");
        log.warn("⚠ 生产环境：设置 JWT_SECRET 环境变量（Base64编码后>=64字符）");
        log.warn("==============================================");
    }

    private String getSecretKey() {
        return this.secretKey;
    }

    private Key getSigningKey() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(getSecretKey());
            return new SecretKeySpec(keyBytes, "HmacSHA384");
        } catch (IllegalArgumentException e) {
            byte[] keyBytes = getSecretKey().getBytes();
            if (keyBytes.length < 48) {
                byte[] padded = new byte[48];
                System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
                keyBytes = padded;
            }
            return new SecretKeySpec(keyBytes, "HmacSHA384");
        }
    }

    public String generateToken(Long userId, String username) {
        return generateToken(userId, username, null);
    }

    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role != null ? role : "USER");
        claims.put("jti", UUID.randomUUID().toString());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        claims.put("jti", UUID.randomUUID().toString());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration * 7))
                .signWith(getSigningKey())
                .compact();
    }

    public Long getUserIdFromRefreshToken(String refreshToken) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();

            if (!"refresh".equals(claims.get("type"))) {
                return null;
            }

            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    public String getJtiFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return claims.get("jti", String.class);
        }
        return null;
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            if ("refresh".equals(claims.get("type"))) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return claims.get("userId", Long.class);
        }
        return null;
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return claims.get("username", String.class);
        }
        return null;
    }

    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return claims.get("role", String.class);
        }
        return null;
    }

    public boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return claims.getExpiration().before(new Date());
        }
        return true;
    }
}