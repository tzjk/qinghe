package com.qinghe.life;

import com.qinghe.life.entity.User;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.redis.RedisBusinessMetrics;
import com.qinghe.life.redis.SeckillStreamErrorClassifier;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.vo.UserDTO;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import static org.junit.jupiter.api.Assertions.*;

/** Pure offline tests: no Spring context, Redis connection, database or network is created. */
class RedisHardeningUnitTest {
    @AfterEach void restoreNamespace() { RedisKeys.configureNamespace("qh:"); }
    @Test void sessionHashExcludesRealNameStudentNumberAndPhone() {
        User user = new User(); user.setId(9L); user.setNickname("display"); user.setPhone("13812345678");
        UserDTO dto = UserDTO.fromUser(user); dto.setRealName("should-not-persist"); dto.setStudentNo("should-not-persist");
        Map<String, String> session = dto.toMap();
        assertFalse(session.containsKey("realName")); assertFalse(session.containsKey("studentNo")); assertFalse(session.containsKey("phoneMasked"));
        assertEquals(9L, UserDTO.fromMap((Map) session).getId().longValue());
    }
    @Test void namespaceIsCentralizedAndCanSeparateEnvironments() {
        RedisKeys.configureNamespace("qh:test"); assertEquals("qh:test:", RedisKeys.namespace());
        assertEquals("qh:test:stream:coupon:claim", RedisKeys.couponSeckillStream());
        assertEquals("qh:test:login:token:abc", RedisKeys.token("abc"));
    }
    @Test void metricsUseOnlyLowCardinalityDimensions() {
        RedisBusinessMetrics metrics = new RedisBusinessMetrics();
        metrics.count("stream_dlq_total", "seckill", "dlq", "success", "none");
        assertEquals(1L, metrics.value("stream_dlq_total", "seckill", "dlq", "success", "none"));
    }
    @Test void classifierMakesInvalidAndConfirmedBusinessFailuresNonRetryable() {
        SeckillStreamErrorClassifier classifier = new SeckillStreamErrorClassifier();
        assertFalse(classifier.classify(new IllegalArgumentException()).isRetryable());
        assertFalse(classifier.classify(new BusinessException(409, "confirmed")).isRetryable());
        assertTrue(classifier.classify(new IllegalStateException("temporary")).isRetryable());
    }
    @Test void claimLuaPreflightsTypesAndRollsBackAfterXaddFailure() throws Exception {
        String lua = resource("lua/coupon-seckill-claim.lua");
        assertTrue(lua.contains("redis.call('TYPE'")); assertTrue(lua.contains("redis.pcall('XADD'"));
        assertTrue(lua.contains("redis.call('SREM'")); assertTrue(lua.contains("redis.call('INCR'"));
        assertTrue(lua.contains("'messageStatus', 'ENQUEUED'"));
    }
    @Test void dlqLuaUsesOriginalMessageIdIndexBeforeAcknowledgementCanOccur() throws Exception {
        String lua = resource("lua/coupon-seckill-dlq.lua");
        assertTrue(lua.contains("originalMessageId")); assertTrue(lua.contains("local existing = redis.call('GET'"));
        assertTrue(lua.contains("redis.call('SET', KEYS[2], messageId"));
    }
    @Test void defaultConfigurationHasBoundedStreamPolicyButNoDestructiveTrim() throws Exception {
        String yaml = resource("application.yml");
        assertTrue(yaml.contains("stream-max-suggested-length")); assertTrue(yaml.contains("stream-alert-length"));
        assertFalse(yaml.contains("XTRIM")); assertFalse(yaml.contains("MAXLEN"));
    }
    private String resource(String name) throws Exception { java.io.InputStream input = new ClassPathResource(name).getInputStream(); try { byte[] bytes = new byte[input.available()]; int read = input.read(bytes); return new String(bytes, 0, read, StandardCharsets.UTF_8); } finally { input.close(); } }
}
