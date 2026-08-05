package com.qinghe.life.config;

import com.qinghe.life.websocket.OrderWebSocketHandler;
import com.qinghe.life.websocket.OrderWebSocketHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class OrderWebSocketConfig implements WebSocketConfigurer {
    private final OrderWebSocketHandler handler;
    private final OrderWebSocketHandshakeInterceptor handshakeInterceptor;

    public OrderWebSocketConfig(OrderWebSocketHandler handler, OrderWebSocketHandshakeInterceptor handshakeInterceptor) {
        this.handler = handler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/orders/user", "/ws/orders/admin")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("http://localhost:5174");
    }
}
