package com.qinghe.life.config;

import com.qinghe.life.utils.RedisKeys;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisNamespaceConfiguration {
    @Value("${qinghe.redis.namespace:qh:}") private String namespace;
    @PostConstruct public void configure() { RedisKeys.configureNamespace(namespace); }
}
