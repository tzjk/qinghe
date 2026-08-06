package com.qinghe.life.seckilltest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("local-seckill-test")
@ConditionalOnProperty(prefix = "seckill.test", name = "enabled", havingValue = "true")
public class LocalSeckillTestWebConfig implements WebMvcConfigurer {
    private final LocalSeckillTestInterceptor interceptor;
    public LocalSeckillTestWebConfig(LocalSeckillTestInterceptor interceptor) { this.interceptor = interceptor; }
    @Override public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(interceptor).addPathPatterns("/api/**").order(1); }
}
