package com.qinghe.life.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinghe.life.common.Result;
import com.qinghe.life.utils.AdminContext;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.vo.AdminInfoVO;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    public AdminAuthInterceptor(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) { this.redisTemplate = redisTemplate; this.objectMapper = objectMapper; }
    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String token = token(request);
        if (token == null) return reject(response, 401, "未登录或登录已过期");
        Map<Object, Object> session = redisTemplate.opsForHash().entries(RedisKeys.adminToken(token));
        if (session == null || session.isEmpty() || session.get("adminId") == null) return reject(response, 401, "未登录或登录已过期");
        try {
            AdminContext.setAdmin(AdminInfoVO.fromMap(session));
            AdminContext.setToken(token);
        } catch (RuntimeException exception) {
            AdminContext.clear();
            return reject(response, 401, "未登录或登录已过期");
        }
        redisTemplate.expire(RedisKeys.adminToken(token), RedisKeys.ADMIN_TOKEN_TTL_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
        return true;
    }
    @Override public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) { AdminContext.clear(); }
    private boolean reject(HttpServletResponse response, int code, String message) throws Exception { response.setStatus(code); response.setContentType("application/json;charset=UTF-8"); objectMapper.writeValue(response.getWriter(), Result.fail(code, message)); return false; }
    private String token(HttpServletRequest request) { String value=request.getHeader("Authorization"); if(value==null || !value.startsWith("Bearer ") || value.length()<=7) return null; String token=value.substring(7).trim(); return token.isEmpty()?null:token; }
}
