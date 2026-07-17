package com.example.smartvisionstock.websocket;

import com.example.smartvisionstock.event.StockChangeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StockWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(StockWebSocketHandler.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("WebSocket connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        log.info("WebSocket disconnected: {}, status: {}", session.getId(), status);
    }

    public void broadcastStockChange(Long locationId, Integer newStatus, Long goodsInstanceId) {
        try {
            Map<String, Object> message = Map.of(
                    "type", "stockChange",
                    "locationId", locationId,
                    "newStatus", newStatus,
                    "goodsInstanceId", goodsInstanceId
            );
            String jsonMessage = objectMapper.writeValueAsString(message);
            
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(jsonMessage));
                }
            }
        } catch (IOException e) {
            log.error("Failed to broadcast stock change: locationId={}, newStatus={}", locationId, newStatus, e);
        }
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * 事务提交后再广播库存变更，避免事务回滚后仍通知客户端，
     * 也避免广播（网络 IO）占用数据库事务连接。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockChangeCommitted(StockChangeEvent event) {
        broadcastStockChange(event.getLocationId(), event.getNewStatus(), event.getGoodsInstanceId());
    }
}
