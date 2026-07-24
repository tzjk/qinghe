package com.qinghe.life.websocket;

import com.qinghe.life.utils.RedisKeys;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/** Validates the existing Redis login session before accepting a WebSocket connection. */
@Component
public class OrderWebSocketHandshakeInterceptor implements HandshakeInterceptor {
    static final String ROLE_ATTRIBUTE = "orderWebSocketRole";
    static final String USER_ID_ATTRIBUTE = "orderWebSocketUserId";
    static final String USER_ROLE = "USER";
    static final String ADMIN_ROLE = "ADMIN";
    private static final String USER_PROTOCOL_PREFIX = "qh-user.";
    private static final String ADMIN_PROTOCOL_PREFIX = "qh-admin.";
    private final StringRedisTemplate redisTemplate;

    public OrderWebSocketHandshakeInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                                   Map<String, Object> attributes) {
        String path = request.getURI().getPath();
        boolean userEndpoint = "/ws/orders/user".equals(path);
        boolean adminEndpoint = "/ws/orders/admin".equals(path);
        if (!userEndpoint && !adminEndpoint) {
            return false;
        }
        String token = token(request.getHeaders().get("Sec-WebSocket-Protocol"), userEndpoint ? USER_PROTOCOL_PREFIX : ADMIN_PROTOCOL_PREFIX);
        if (token == null) {
            return false;
        }
        Map<Object, Object> session = redisTemplate.opsForHash().entries(userEndpoint ? RedisKeys.token(token) : RedisKeys.adminToken(token));
        String idKey = userEndpoint ? "id" : "adminId";
        if (session == null || session.isEmpty() || session.get(idKey) == null) {
            return false;
        }
        try {
            attributes.put(ROLE_ATTRIBUTE, userEndpoint ? USER_ROLE : ADMIN_ROLE);
            attributes.put(USER_ID_ATTRIBUTE, Long.valueOf(String.valueOf(session.get(idKey))));
            redisTemplate.expire(userEndpoint ? RedisKeys.token(token) : RedisKeys.adminToken(token),
                    userEndpoint ? RedisKeys.LOGIN_TOKEN_TTL_MINUTES : RedisKeys.ADMIN_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Exception exception) {
        // Authentication attributes are retained by the WebSocket session only; tokens are never retained or logged.
    }

    private String token(List<String> protocolHeaders, String prefix) {
        if (protocolHeaders == null) {
            return null;
        }
        for (String header : protocolHeaders) {
            for (String value : header.split(",")) {
                String candidate = value.trim();
                if (candidate.startsWith(prefix) && candidate.length() > prefix.length()) {
                    return candidate.substring(prefix.length());
                }
            }
        }
        return null;
    }
}
