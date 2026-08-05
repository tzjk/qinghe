package com.qinghe.life.redis;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.stereotype.Component;

/**
 * Dependency-free low-cardinality metric seam. An exporter can later poll this bean; no token,
 * user, order, coupon or message identifiers are accepted as dimensions.
 */
@Component
public class RedisBusinessMetrics {
    private final ConcurrentMap<String, LongAdder> counters = new ConcurrentHashMap<String, LongAdder>();
    private final ConcurrentMap<String, LongAdder> lockWaitMillis = new ConcurrentHashMap<String, LongAdder>();
    public void count(String name, String module, String operation, String result, String errorType) { add(counters, key(name, module, operation, result, errorType), 1L); }
    public void lockWait(String module, String operation, long millis) { add(lockWaitMillis, key("redis_lock_wait_duration", module, operation, "observed", "none"), Math.max(0L, millis)); }
    public long value(String name, String module, String operation, String result, String errorType) { LongAdder value = counters.get(key(name, module, operation, result, errorType)); return value == null ? 0L : value.sum(); }
    private void add(ConcurrentMap<String, LongAdder> target, String key, long value) { LongAdder adder = target.get(key); if (adder == null) { LongAdder created = new LongAdder(); LongAdder existing = target.putIfAbsent(key, created); adder = existing == null ? created : existing; } adder.add(value); }
    private String key(String name, String module, String operation, String result, String errorType) { return safe(name) + "|" + safe(module) + "|" + safe(operation) + "|" + safe(result) + "|" + safe(errorType); }
    private String safe(String value) { return value == null || value.trim().isEmpty() ? "none" : value; }
}
