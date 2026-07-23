package com.qinghe.life.config;

import com.qinghe.life.interceptor.LoginInterceptor;
import com.qinghe.life.interceptor.RefreshTokenInterceptor;
import com.qinghe.life.interceptor.AdminAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final LoginInterceptor loginInterceptor;
    private final RefreshTokenInterceptor refreshTokenInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

    public WebConfig(LoginInterceptor loginInterceptor, RefreshTokenInterceptor refreshTokenInterceptor, AdminAuthInterceptor adminAuthInterceptor) {
        this.loginInterceptor = loginInterceptor;
        this.refreshTokenInterceptor = refreshTokenInterceptor;
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:5174")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(refreshTokenInterceptor)
                .addPathPatterns("/api/**")
                .order(0);
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/user/code", "/api/auth/register", "/api/user/login", "/api/user/login/password", "/api/admin/auth/login", "/api/categories", "/api/home/**",
                        "/api/shops/**", "/api/goods/**", "/api/coupons", "/api/blogs/**", "/api/campuses/**", "/api/admin/**")
                .order(2);
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login")
                .order(1);
    }
}
