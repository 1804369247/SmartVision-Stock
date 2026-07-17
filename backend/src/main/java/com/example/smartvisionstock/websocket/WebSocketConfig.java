package com.example.smartvisionstock.websocket;

import com.example.smartvisionstock.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    @Autowired
    private StockWebSocketHandler stockWebSocketHandler;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${app.cors.origins:http://localhost:8081,http://localhost:3000,http://localhost:5173}")
    private String corsOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 解析 CORS 白名单
        String[] origins = Arrays.stream(corsOrigins.split("\\s*,\\s*"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        if (origins.length == 0) {
            log.warn("WebSocket 未配置 CORS 允许来源（app.cors.origins 为空），将拒绝所有跨域握手");
        }
        registry.addHandler(stockWebSocketHandler, "/topic/stock")
                .addInterceptors(new JwtHandshakeInterceptor(jwtUtil))
                .setAllowedOrigins(origins);
    }

    /**
     * WebSocket 握手拦截器：从 URL 参数中提取 token 并验证
     */
    private static class JwtHandshakeInterceptor implements HandshakeInterceptor {

        private final JwtUtil jwtUtil;

        JwtHandshakeInterceptor(JwtUtil jwtUtil) {
            this.jwtUtil = jwtUtil;
        }

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            URI uri = request.getURI();
            String query = uri.getQuery();

            log.info("WebSocket 握手请求: URI={}, Origin={}",
                    uri,
                    request.getHeaders().getFirst("Origin"));

            // 优先从请求头 / 子协议提取 token（避免 token 明文出现在 URL 中被代理日志记录）
            String token = null;
            String tokenSource = null;

            // 1) Authorization: Bearer <token>（非浏览器客户端，如服务端间调用）
            String authz = request.getHeaders().getFirst("Authorization");
            if (authz != null && authz.startsWith("Bearer ")) {
                token = authz.substring(7).trim();
                tokenSource = "header";
            }

            // 2) Sec-WebSocket-Protocol 子协议（浏览器唯一可携带自定义信息的通道）
            if (token == null) {
                List<String> subprotocols = request.getHeaders().get("Sec-WebSocket-Protocol");
                if (subprotocols != null) {
                    outer:
                    for (String entry : subprotocols) {
                        for (String candidate : entry.split(",")) {
                            candidate = candidate.trim();
                            if (!candidate.isEmpty() && !"null".equalsIgnoreCase(candidate)) {
                                token = candidate;
                                tokenSource = "subprotocol";
                                break outer;
                            }
                        }
                    }
                }
            }

            // 3) 回退：URL 查询参数（保留以便演示与旧客户端兼容）
            if (token == null && query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2 && "token".equals(kv[0])) {
                        try {
                            token = java.net.URLDecoder.decode(kv[1], "UTF-8");
                        } catch (Exception e) {
                            token = kv[1];
                        }
                        tokenSource = "query";
                        break;
                    }
                }
            }

            if (token != null && !token.isEmpty()) {
                if (!jwtUtil.validateToken(token)) {
                    log.warn("WebSocket 握手失败: Token 无效（可能是后端重启导致密钥变更）");
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                }
                // 若 token 来自子协议，必须回显 Sec-WebSocket-Protocol，否则浏览器会断开连接
                if ("subprotocol".equals(tokenSource)) {
                    response.getHeaders().set("Sec-WebSocket-Protocol", token);
                }
            } else {
                log.info("WebSocket 匿名连接（无 token）");
            }

            // 将用户信息存入 attributes，供 Handler 使用（匿名连接时为 null）
            Long userId = token != null ? jwtUtil.getUserIdFromToken(token) : null;
            String username = token != null ? jwtUtil.getUsernameFromToken(token) : "anonymous";
            attributes.put("userId", userId);
            attributes.put("username", username);
            log.info("WebSocket 连接认证成功(source={}): userId={}, username={}", tokenSource, userId, username);
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
            // nothing to do
        }
    }
}
