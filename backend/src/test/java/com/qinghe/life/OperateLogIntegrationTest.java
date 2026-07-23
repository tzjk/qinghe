package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.OperateLog;
import com.qinghe.life.mapper.OperateLogMapper;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(OperateLogIntegrationTest.TestConfig.class)
class OperateLogIntegrationTest {
    private static final Long TEST_USER_ID = 909001L;
    private static final String ACTION_PREFIX = "OPERATE_LOG_TEST_";

    @Autowired
    private OperateLogMapper operateLogMapper;

    @Autowired
    private TestOperateLogFacade facade;

    @BeforeEach
    void setUp() {
        cleanupLogs();
        UserContext.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        cleanupLogs();
        UserContext.clear();
        RequestContextHolder.resetRequestAttributes();
        assertNull(UserContext.getUser());
    }

    @Test
    void successfulOperationPersistsMaskedAndTruncatedLogFromUserContext() {
        bindRequest("POST", "/api/test/operate-log", "198.51.100.11, 10.0.0.1");
        setCurrentUser(TEST_USER_ID);
        Map<String, Object> payload = sensitivePayload();

        Map<String, Object> result = facade.success(payload);

        assertEquals("ok", result.get("status"));
        OperateLog log = findLog(ACTION_PREFIX + "成功");
        assertNotNull(log);
        assertEquals(TEST_USER_ID, log.getUserId());
        assertEquals("操作日志测试", log.getModule());
        assertEquals(TestOperateLogFacade.class.getSimpleName(), log.getControllerClass());
        assertEquals("success", log.getControllerMethod());
        assertEquals("/api/test/operate-log", log.getRequestPath());
        assertEquals("POST", log.getHttpMethod());
        assertEquals("198.51.100.11", log.getIp());
        assertEquals(Integer.valueOf(1), log.getSuccess());
        assertNull(log.getExceptionSummary());
        assertTrue(log.getDurationMs() >= 0L);
        assertNotNull(log.getOperateTime());
        assertTrue(log.getRequestSummary().length() <= 2000);
        assertTrue(log.getResponseSummary().length() <= 2000);

        String combined = log.getRequestSummary() + log.getResponseSummary();
        assertTrue(combined.contains("139****9999"));
        assertFalse(combined.contains("13900009999"));
        assertFalse(combined.contains("plain-password"));
        assertFalse(combined.contains("confirm-secret"));
        assertFalse(combined.contains("hash-secret"));
        assertFalse(combined.contains("123456"));
        assertFalse(combined.contains("Bearer request-secret"));
        assertFalse(combined.contains("token-secret"));
        assertFalse(combined.contains("redis-secret"));
        assertFalse(combined.contains("database-secret"));
        assertFalse(combined.contains("AQID"));
        assertEquals(TEST_USER_ID, UserContext.getUserId());
    }

    @Test
    void failurePersistsFailureLogAndRethrowsOriginalException() {
        bindRequest("PUT", "/api/test/operate-log/fail", null);
        setCurrentUser(TEST_USER_ID);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> facade.fail(sensitivePayload()));

        assertEquals("expected-business-failure", exception.getMessage());
        OperateLog log = findLog(ACTION_PREFIX + "失败");
        assertNotNull(log);
        assertEquals(Integer.valueOf(0), log.getSuccess());
        assertEquals("IllegalStateException", log.getExceptionSummary());
        assertNull(log.getResponseSummary());
        assertEquals(TEST_USER_ID, log.getUserId());
    }

    @Test
    void loggingDoesNotCreateUserContextWhenNoUserIsPresent() {
        bindRequest("POST", "/api/test/operate-log/no-user", null);

        facade.noUser();

        OperateLog log = findLog(ACTION_PREFIX + "无用户");
        assertNotNull(log);
        assertNull(log.getUserId());
        assertNull(UserContext.getUser());
    }

    private Map<String, Object> sensitivePayload() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("phone", "13900009999");
        payload.put("password", "plain-password");
        payload.put("confirmPassword", "confirm-secret");
        payload.put("passwordHash", "hash-secret");
        payload.put("code", "123456");
        payload.put("Authorization", "Bearer request-secret");
        payload.put("token", "token-secret");
        payload.put("redisPassword", "redis-secret");
        payload.put("databasePassword", "database-secret");
        payload.put("binary", new byte[]{1, 2, 3});
        payload.put("longText", repeat("a", 2600));
        return payload;
    }

    private void bindRequest(String method, String uri, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr("127.0.0.1");
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void setCurrentUser(Long userId) {
        UserDTO user = new UserDTO();
        user.setId(userId);
        user.setNickname(ACTION_PREFIX + "USER");
        UserContext.setUser(user);
    }

    private OperateLog findLog(String action) {
        return operateLogMapper.selectOne(Wrappers.<OperateLog>lambdaQuery()
                .eq(OperateLog::getAction, action)
                .orderByDesc(OperateLog::getId)
                .last("LIMIT 1"));
    }

    private void cleanupLogs() {
        operateLogMapper.delete(Wrappers.<OperateLog>lambdaQuery().likeRight(OperateLog::getAction, ACTION_PREFIX));
    }

    private String repeat(String value, int count) {
        char[] chars = new char[value.length() * count];
        Arrays.fill(chars, value.charAt(0));
        return new String(chars);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        TestOperateLogFacade testOperateLogFacade() {
            return new TestOperateLogFacade();
        }
    }

    static class TestOperateLogFacade {
        @com.qinghe.life.annotation.OperateLog(module = "操作日志测试", action = ACTION_PREFIX + "成功")
        public Map<String, Object> success(Map<String, Object> payload) {
            Map<String, Object> response = new LinkedHashMap<String, Object>();
            response.put("status", "ok");
            response.put("phone", "13900009999");
            response.put("token", "response-token-secret");
            response.put("longText", repeatStatic("b", 2600));
            return response;
        }

        @com.qinghe.life.annotation.OperateLog(module = "操作日志测试", action = ACTION_PREFIX + "失败")
        public void fail(Map<String, Object> payload) {
            throw new IllegalStateException("expected-business-failure");
        }

        @com.qinghe.life.annotation.OperateLog(module = "操作日志测试", action = ACTION_PREFIX + "无用户")
        public void noUser() {
        }

        private static String repeatStatic(String value, int count) {
            char[] chars = new char[value.length() * count];
            Arrays.fill(chars, value.charAt(0));
            return new String(chars);
        }
    }
}
