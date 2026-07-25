package com.qinghe.life;

import com.qinghe.life.entity.Order;
import com.qinghe.life.enums.OrderNotificationReason;
import com.qinghe.life.enums.OrderStatus;
import com.qinghe.life.service.OrderNotificationPublisher;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.websocket.OrderWebSocketHandler;
import com.qinghe.life.websocket.OrderWebSocketHandshakeInterceptor;
import com.qinghe.life.websocket.OrderWebSocketNotifier;
import com.qinghe.life.websocket.OrderWebSocketSessionRegistry;
import com.qinghe.life.vo.OrderWebSocketMessageVO;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class OrderWebSocketIntegrationTest {
    private static final AtomicLong IDS = new AtomicLong(9_000_000L);

    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private OrderWebSocketHandshakeInterceptor handshakeInterceptor;
    @Autowired private OrderWebSocketHandler handler;
    @Autowired private OrderWebSocketSessionRegistry sessionRegistry;
    @Autowired private OrderWebSocketNotifier notifier;
    @Autowired private OrderNotificationPublisher notificationPublisher;
    @Autowired private TransactionTemplate transactionTemplate;

    private String userToken;
    private String adminToken;

    @AfterEach
    void tearDown() {
        if (userToken != null) redisTemplate.delete(RedisKeys.token(userToken));
        if (adminToken != null) redisTemplate.delete(RedisKeys.adminToken(adminToken));
    }

    @Test
    void anonymousConnectionIsRejected() {
        assertFalse(handshake("/ws/orders/user", null, new HashMap<String, Object>()));
    }

    @Test
    void invalidTokenConnectionIsRejected() {
        assertFalse(handshake("/ws/orders/user", "qh-user.invalid", new HashMap<String, Object>()));
    }

    @Test
    void userConnectionIsAuthenticatedFromRedisToken() {
        long userId = IDS.incrementAndGet();
        String token = userToken(userId);
        Map<String, Object> attributes = new HashMap<String, Object>();
        assertTrue(handshake("/ws/orders/user", "qh-user." + token, attributes));
        assertTrue("USER".equals(attributes.get("orderWebSocketRole")));
        assertTrue(Long.valueOf(userId).equals(attributes.get("orderWebSocketUserId")));
    }

    @Test
    void adminConnectionIsAuthenticatedFromRedisToken() {
        long adminId = IDS.incrementAndGet();
        String token = adminToken(adminId);
        Map<String, Object> attributes = new HashMap<String, Object>();
        assertTrue(handshake("/ws/orders/admin", "qh-admin." + token, attributes));
        assertTrue("ADMIN".equals(attributes.get("orderWebSocketRole")));
        assertTrue(Long.valueOf(adminId).equals(attributes.get("orderWebSocketUserId")));
    }

    @Test
    void normalUserCannotSubscribeToAdminChannel() {
        String token = userToken(IDS.incrementAndGet());
        assertFalse(handshake("/ws/orders/admin", "qh-user." + token, new HashMap<String, Object>()));
    }

    @Test
    void userReceivesOnlyOwnOrderNotifications() throws Exception {
        long owner = IDS.incrementAndGet();
        WebSocketSession own = session("OWN", "USER", owner);
        WebSocketSession other = session("OTHER", "USER", owner + 1);
        handler.afterConnectionEstablished(own);
        handler.afterConnectionEstablished(other);
        notifier.notifyUser(owner, messageOrder(owner, OrderStatus.PAID));
        verify(own, times(1)).sendMessage(any(TextMessage.class));
        verify(other, never()).sendMessage(any(TextMessage.class));
        handler.afterConnectionClosed(own, CloseStatus.NORMAL);
        handler.afterConnectionClosed(other, CloseStatus.NORMAL);
    }

    @Test
    void everySessionForSameUserReceivesNotification() throws Exception {
        long owner = IDS.incrementAndGet();
        WebSocketSession first = session("FIRST", "USER", owner);
        WebSocketSession second = session("SECOND", "USER", owner);
        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(second);
        notifier.notifyUser(owner, messageOrder(owner, OrderStatus.PAID));
        verify(first).sendMessage(any(TextMessage.class));
        verify(second).sendMessage(any(TextMessage.class));
        handler.afterConnectionClosed(first, CloseStatus.NORMAL);
        handler.afterConnectionClosed(second, CloseStatus.NORMAL);
    }

    @Test
    void orderCreationNotificationIsSentAfterCommit() throws Exception {
        assertCommittedMessage(OrderNotificationReason.CREATED, OrderStatus.PENDING_PAY, "ORDER_CREATED");
    }

    @Test
    void paymentNotificationIsSentAfterCommit() throws Exception {
        assertCommittedMessage(OrderNotificationReason.PAID, OrderStatus.PAID, "ORDER_PAID");
    }

    @Test
    void userCancellationNotificationIsSentAfterCommit() throws Exception {
        assertCommittedMessage(OrderNotificationReason.USER_CANCELLED, OrderStatus.CANCELLED, "ORDER_CANCELLED");
    }

    @Test
    void timeoutCancellationNotificationIsSentAfterCommit() throws Exception {
        assertCommittedMessage(OrderNotificationReason.TIMEOUT_CANCELLED, OrderStatus.CANCELLED, "ORDER_TIMEOUT_CANCELLED");
    }

    @Test
    void administratorTransitionsNotifyTheOwnerAfterCommit() throws Exception {
        assertCommittedMessage(OrderNotificationReason.ACCEPTED, OrderStatus.ACCEPTED, "ORDER_ACCEPTED");
        assertCommittedMessage(OrderNotificationReason.DELIVERING, OrderStatus.DELIVERING, "ORDER_DELIVERING");
        assertCommittedMessage(OrderNotificationReason.COMPLETED, OrderStatus.COMPLETED, "ORDER_COMPLETED");
    }

    @Test
    void rolledBackTransactionDoesNotSendNotification() throws Exception {
        long owner = IDS.incrementAndGet();
        WebSocketSession target = session("ROLLBACK", "USER", owner);
        handler.afterConnectionEstablished(target);
        transactionTemplate.executeWithoutResult(status -> {
            notificationPublisher.publishAfterCommit(order(owner, OrderStatus.PAID), OrderNotificationReason.PAID);
            status.setRollbackOnly();
        });
        verify(target, never()).sendMessage(any(TextMessage.class));
        handler.afterConnectionClosed(target, CloseStatus.NORMAL);
    }

    @Test
    void sendFailureDoesNotEscapeToCommittedOrderFlow() throws Exception {
        long owner = IDS.incrementAndGet();
        WebSocketSession failed = session("FAILED", "USER", owner);
        doThrow(new IOException("connection closed")).when(failed).sendMessage(any(TextMessage.class));
        handler.afterConnectionEstablished(failed);
        assertDoesNotThrow(() -> notifier.notifyUser(owner, messageOrder(owner, OrderStatus.PAID)));
        handler.afterConnectionClosed(failed, CloseStatus.NORMAL);
    }

    @Test
    void closedConnectionIsRemovedFromRegistry() throws Exception {
        long owner = IDS.incrementAndGet();
        WebSocketSession target = session("CLOSED", "USER", owner);
        handler.afterConnectionEstablished(target);
        handler.afterConnectionClosed(target, CloseStatus.NORMAL);
        notifier.notifyUser(owner, messageOrder(owner, OrderStatus.PAID));
        verify(target, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void olderStatusMessageDoesNotNeedToOverwriteNewerStatePayload() {
        Order newer = order(IDS.incrementAndGet(), OrderStatus.DELIVERING);
        Order older = order(newer.getUserId(), OrderStatus.PAID);
        assertTrue(statusRank(newer.getStatus()) > statusRank(older.getStatus()));
    }

    private void assertCommittedMessage(OrderNotificationReason reason, OrderStatus status, String expectedType) throws Exception {
        long owner = IDS.incrementAndGet();
        WebSocketSession target = session("COMMIT_" + owner, "USER", owner);
        handler.afterConnectionEstablished(target);
        transactionTemplate.executeWithoutResult(ignored -> notificationPublisher.publishAfterCommit(order(owner, status), reason));
        ArgumentCaptor<TextMessage> payload = ArgumentCaptor.forClass(TextMessage.class);
        verify(target).sendMessage(payload.capture());
        assertTrue(payload.getValue().getPayload().contains("\"messageType\":\"" + expectedType + "\""));
        handler.afterConnectionClosed(target, CloseStatus.NORMAL);
    }

    private boolean handshake(String path, String protocol, Map<String, Object> attributes) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (protocol != null) request.addHeader("Sec-WebSocket-Protocol", protocol);
        return handshakeInterceptor.beforeHandshake(new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(new MockHttpServletResponse()), mock(WebSocketHandler.class), attributes);
    }

    private WebSocketSession session(String id, String role, long principalId) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("orderWebSocketRole", role);
        attributes.put("orderWebSocketUserId", principalId);
        when(session.getId()).thenReturn(id);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private String userToken(long userId) {
        userToken = "ws-user-" + userId;
        redisTemplate.opsForHash().put(RedisKeys.token(userToken), "id", String.valueOf(userId));
        return userToken;
    }

    private String adminToken(long adminId) {
        adminToken = "ws-admin-" + adminId;
        redisTemplate.opsForHash().put(RedisKeys.adminToken(adminToken), "adminId", String.valueOf(adminId));
        return adminToken;
    }

    private OrderWebSocketMessageVO messageOrder(long owner, OrderStatus status) {
        Order order = order(owner, status);
        OrderWebSocketMessageVO message = new OrderWebSocketMessageVO();
        message.setMessageType("TEST");
        message.setOrderId(order.getId());
        message.setOrderNo(order.getOrderNo());
        message.setOrderStatus(status.getCode());
        message.setStatusText(status.getDisplayName());
        message.setOccurredAt(LocalDateTime.now());
        message.setSummary("test");
        return message;
    }

    private Order order(long owner, OrderStatus status) {
        Order order = new Order();
        order.setId(IDS.incrementAndGet());
        order.setUserId(owner);
        order.setOrderNo("QHWS" + order.getId());
        order.setStatus(status.getCode());
        order.setCreateTime(LocalDateTime.now());
        return order;
    }

    private int statusRank(String status) {
        if (OrderStatus.PENDING_PAY.getCode().equals(status)) return 10;
        if (OrderStatus.PAID.getCode().equals(status)) return 20;
        if (OrderStatus.ACCEPTED.getCode().equals(status)) return 30;
        if (OrderStatus.DELIVERING.getCode().equals(status)) return 40;
        if (OrderStatus.COMPLETED.getCode().equals(status)) return 50;
        return 60;
    }
}
