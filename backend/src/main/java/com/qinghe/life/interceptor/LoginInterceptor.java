package com.qinghe.life.interceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinghe.life.common.Result;
import com.qinghe.life.utils.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@Component public class LoginInterceptor implements HandlerInterceptor {
 private final ObjectMapper objectMapper;
 public LoginInterceptor(ObjectMapper objectMapper){this.objectMapper=objectMapper;}
 public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler)throws Exception{ if("OPTIONS".equalsIgnoreCase(request.getMethod())||UserContext.getUser()!=null) return true; return unauthorized(response); }
 private boolean unauthorized(HttpServletResponse response)throws Exception{response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);response.setContentType("application/json;charset=UTF-8");objectMapper.writeValue(response.getWriter(),Result.fail(401,"未登录或登录已过期"));return false;}
}
