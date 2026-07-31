package com.qinghe.life;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Guarded entry point for a separately approved integration suite. It never creates a Spring
 * context by itself, so default Maven tests cannot connect to Redis or MySQL.
 */
@Tag("real-social")
@EnabledIfEnvironmentVariable(named = "QINGHE_REAL_SOCIAL_TESTS", matches = "true")
class RealSocialIntegrationReservedTest {
    @Test
    void requiresDedicatedPerRunRedisNamespace() {
        String namespace = System.getenv("QINGHE_TEST_REDIS_NAMESPACE");
        assertTrue(namespace != null && namespace.matches("qh:test:social:[A-Za-z0-9_-]+:"), "QINGHE_TEST_REDIS_NAMESPACE must be a per-run qh:test:social:{runId}: namespace");
        System.out.println("Real social tests are enabled for namespace: " + namespace);
    }
}
