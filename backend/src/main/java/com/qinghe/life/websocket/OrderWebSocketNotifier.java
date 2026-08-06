package com.qinghe.life.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinghe.life.vo.OrderWebSocketMessageVO;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/** Best-effort delivery: a failed send never affects the committed order transaction. */
@Service
public class OrderWebSocketNotifier {
    private static final Logger log = LoggerFactory.getLogger(OrderWebSocketNotifier.class);
    private final OrderWebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public OrderWebSocketNotifier(OrderWebSocketSessionRegistry sessionRegistry, ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    public void notifyUser(Long userId, OrderWebSocketMessageVO message) {
        Map<String, WebSocketSession> sessions = sessionRegistry.userSessions(userId);
        if (sessions != null) {
            send(sessions, message, false, userId);
        }
    }

    public void notifyAdmins(OrderWebSocketMessageVO message) {
        send(sessionRegistry.adminSessions(), message, true, null);
    }

    private void send(Map<String, WebSocketSession> sessions, OrderWebSocketMessageVO message, boolean admin, Long userId) {
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            log.warn("订单 WebSocket 消息序列化失败，type={}, orderId={}", message.getMessageType(), message.getOrderId(), exception);
            return;
        }
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            WebSocketSession session = entry.getValue();
            if (!session.isOpen()) {
                remove(admin, userId, entry.getKey());
                continue;
            }
            try {
                session.sendMessage(new TextMessage(payload));
                log.info("订单 WebSocket 消息已发送，target={}, sessionId={}, type={}, orderId={}, orderNo={}, summary={}",
                        admin ? "ADMIN" : "USER", entry.getKey(), message.getMessageType(), message.getOrderId(),
                        message.getOrderNo(), message.getSummary());
            } catch (IOException | RuntimeException exception) {
                log.warn("订单 WebSocket 发送失败，sessionId={}, type={}, orderId={}, error={}",
                        entry.getKey(), message.getMessageType(), message.getOrderId(), exception.getClass().getSimpleName());
                remove(admin, userId, entry.getKey());
            }
        }
    }

    private void remove(boolean admin, Long userId, String sessionId) {
        if (admin) {
            sessionRegistry.removeAdminSession(sessionId);
        } else {
            sessionRegistry.removeUserSession(userId, sessionId);
        }
    }
}
