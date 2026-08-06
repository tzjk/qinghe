package com.qinghe.life.websocket;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** Connections are bound to the principal established during handshake; clients cannot subscribe by user id. */
@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(OrderWebSocketHandler.class);
    private final OrderWebSocketSessionRegistry sessionRegistry;

    public OrderWebSocketHandler(OrderWebSocketSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        String role = (String) attributes.get(OrderWebSocketHandshakeInterceptor.ROLE_ATTRIBUTE);
        Long principalId = (Long) attributes.get(OrderWebSocketHandshakeInterceptor.USER_ID_ATTRIBUTE);
        if (OrderWebSocketHandshakeInterceptor.USER_ROLE.equals(role) && principalId != null) {
            sessionRegistry.registerUser(principalId, session);
            log.info("订单 WebSocket 连接已建立，role={}, principalId={}, sessionId={}", role, principalId, session.getId());
        } else if (OrderWebSocketHandshakeInterceptor.ADMIN_ROLE.equals(role) && principalId != null) {
            sessionRegistry.registerAdmin(session);
            log.info("订单 WebSocket 连接已建立，role={}, principalId={}, sessionId={}", role, principalId, session.getId());
        } else {
            log.warn("订单 WebSocket 连接缺少经过认证的会话属性，sessionId={}", session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("订单 WebSocket 连接已断开，sessionId={}, code={}, reason={}", session.getId(), status.getCode(), status.getReason());
        remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("订单 WebSocket 连接异常，sessionId={}, error={}", session.getId(), exception.getClass().getSimpleName());
        remove(session);
    }

    private void remove(WebSocketSession session) {
        Object role = session.getAttributes().get(OrderWebSocketHandshakeInterceptor.ROLE_ATTRIBUTE);
        Object principalId = session.getAttributes().get(OrderWebSocketHandshakeInterceptor.USER_ID_ATTRIBUTE);
        if (OrderWebSocketHandshakeInterceptor.USER_ROLE.equals(role) && principalId instanceof Long) {
            sessionRegistry.removeUserSession((Long) principalId, session.getId());
        } else if (OrderWebSocketHandshakeInterceptor.ADMIN_ROLE.equals(role)) {
            sessionRegistry.removeAdminSession(session.getId());
        }
    }
}
