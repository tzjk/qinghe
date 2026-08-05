package com.qinghe.life.websocket;

import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** Connections are bound to the principal established during handshake; clients cannot subscribe by user id. */
@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {
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
        } else if (OrderWebSocketHandshakeInterceptor.ADMIN_ROLE.equals(role) && principalId != null) {
            sessionRegistry.registerAdmin(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
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
