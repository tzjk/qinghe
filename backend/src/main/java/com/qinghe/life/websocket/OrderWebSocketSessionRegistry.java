package com.qinghe.life.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

/** Thread-safe registry which keeps every connection for a user, not just the last one. */
@Component
public class OrderWebSocketSessionRegistry {
    private static final int SEND_TIME_LIMIT_MILLIS = 10_000;
    private static final int SEND_BUFFER_BYTES = 512 * 1024;
    private final Map<Long, Map<String, WebSocketSession>> userSessions = new ConcurrentHashMap<Long, Map<String, WebSocketSession>>();
    private final Map<String, WebSocketSession> adminSessions = new ConcurrentHashMap<String, WebSocketSession>();

    public void registerUser(Long userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, key -> new ConcurrentHashMap<String, WebSocketSession>())
                .put(session.getId(), decorate(session));
    }

    public void registerAdmin(WebSocketSession session) {
        adminSessions.put(session.getId(), decorate(session));
    }

    public Map<String, WebSocketSession> userSessions(Long userId) {
        return userSessions.get(userId);
    }

    public Map<String, WebSocketSession> adminSessions() {
        return adminSessions;
    }

    public void removeUserSession(Long userId, String sessionId) {
        Map<String, WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(sessionId);
        if (sessions.isEmpty()) {
            userSessions.remove(userId, sessions);
        }
    }

    public void removeAdminSession(String sessionId) {
        adminSessions.remove(sessionId);
    }

    private WebSocketSession decorate(WebSocketSession session) {
        return new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MILLIS, SEND_BUFFER_BYTES);
    }
}
