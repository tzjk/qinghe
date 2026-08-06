package com.qinghe.life.seckilltest;

import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.SeckillTestContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Active only in local-seckill-test; protects test control endpoints and tags test sessions. */
@Component
@Profile("local-seckill-test")
@ConditionalOnProperty(prefix = "seckill.test", name = "enabled", havingValue = "true")
public class LocalSeckillTestInterceptor implements HandlerInterceptor {
    public static final String SECRET_HEADER = "X-Seckill-Test-Secret";
    private final StringRedisTemplate redis;
    @Value("${SECKILL_TEST_SECRET:}") private String secret;
    public LocalSeckillTestInterceptor(StringRedisTemplate redis) { this.redis = redis; }
    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (path.startsWith("/api/local-seckill-test/")) {
            if (!localhost(request) || !sameSecret(request.getHeader(SECRET_HEADER))) { response.sendError(403); return false; }
        }
        String token = token(request);
        if (token != null) {
            Map<Object, Object> session = redis.opsForHash().entries(RedisKeys.token(token));
            Object runId = session.get("seckillTestRunId");
            if (runId != null && !String.valueOf(runId).trim().isEmpty()) SeckillTestContext.setTestRunId(String.valueOf(runId));
        }
        return true;
    }
    @Override public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) { SeckillTestContext.clear(); }
    private boolean localhost(HttpServletRequest request) { String value = request.getRemoteAddr(); return "127.0.0.1".equals(value) || "::1".equals(value) || "0:0:0:0:0:0:0:1".equals(value); }
    private boolean sameSecret(String supplied) { return supplied != null && !secret.isEmpty() && MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8)); }
    private String token(HttpServletRequest request) { String value = request.getHeader("Authorization"); if (value == null || !value.startsWith("Bearer ") || value.length() <= 7) return null; String token = value.substring(7).trim(); return token.isEmpty() ? null : token; }
}
