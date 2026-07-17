package com.example.smartvisionstock.config;

import com.example.smartvisionstock.dto.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LoginRateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    private static final int MAX_REQUESTS = 5;
    private static final long TIME_WINDOW_MS = 60000;

    private final Map<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!"/api/auth/login".equals(httpRequest.getRequestURI())
                || !"POST".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(httpRequest);

        RateLimitInfo info = rateLimitMap.computeIfAbsent(clientIp, k -> new RateLimitInfo());

        synchronized (info) {
            long now = System.currentTimeMillis();

            if (now - info.getLastResetTime() > TIME_WINDOW_MS) {
                info.setCount(1);
                info.setLastResetTime(now);
            } else {
                if (info.getCount() >= MAX_REQUESTS) {
                    log.warn("登录请求限流: IP={}, 超过{}秒内{}次限制", clientIp, TIME_WINDOW_MS / 1000, MAX_REQUESTS);
                    httpResponse.setStatus(429);
                    httpResponse.setContentType("application/json;charset=UTF-8");
                    httpResponse.getWriter().write(objectMapper.writeValueAsString(
                            ApiResponse.error(429, "登录请求过于频繁，请稍后再试")
                    ));
                    return;
                }
                info.incrementCount();
            }
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private static class RateLimitInfo {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long lastResetTime = System.currentTimeMillis();

        public int getCount() {
            return count.get();
        }

        public void incrementCount() {
            count.incrementAndGet();
        }

        public void setCount(int count) {
            this.count.set(count);
        }

        public long getLastResetTime() {
            return lastResetTime;
        }

        public void setLastResetTime(long lastResetTime) {
            this.lastResetTime = lastResetTime;
        }
    }
}