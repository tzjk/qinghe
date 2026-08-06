package com.qinghe.life.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qinghe.life.config.CatalogCacheProperties;
import com.qinghe.life.redis.RedisBusinessMetrics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class CatalogCache {
    private static final Logger log = LoggerFactory.getLogger(CatalogCache.class);
    private static final String NULL_MARKER = "__QH_CATALOG_CACHE_NULL__";

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final CatalogCacheProperties properties;
    private final RedisBusinessMetrics metrics;
    private final Executor logicalRebuildExecutor;

    @Autowired
    public CatalogCache(StringRedisTemplate redisTemplate, RedissonClient redissonClient,
                        ObjectMapper objectMapper, CatalogCacheProperties properties, RedisBusinessMetrics metrics,
                        @Qualifier("catalogCacheLogicalRebuildExecutor") Executor logicalRebuildExecutor) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.metrics = metrics;
        this.logicalRebuildExecutor = logicalRebuildExecutor;
    }

    /** Compatibility constructor for narrow unit tests that do not exercise logical-expiry rebuilds. */
    public CatalogCache(StringRedisTemplate redisTemplate, RedissonClient redissonClient,
                        ObjectMapper objectMapper, CatalogCacheProperties properties, RedisBusinessMetrics metrics) {
        this(redisTemplate, redissonClient, objectMapper, properties, metrics, new Executor() {
            @Override public void execute(Runnable command) { command.run(); }
        });
    }

    public <T> T getObject(String key, String lockKey, Class<T> type, long ttlMinutes, Supplier<T> databaseLoader) {
        return get(key, lockKey, () -> readObject(key, type), value -> writeObject(key, value, ttlMinutes), databaseLoader);
    }

    public <T> List<T> getList(String key, String lockKey, Class<T> elementType, long ttlMinutes, Supplier<List<T>> databaseLoader) {
        List<T> result = get(key, lockKey, () -> readList(key, elementType), value -> writeList(key, value, ttlMinutes), databaseLoader);
        return result == null ? Collections.<T>emptyList() : result;
    }

    /**
     * Hot-key policy: Redis TTL remains a hard safety boundary, while the logical deadline permits
     * a stale value to be returned during one asynchronous distributed-lock protected rebuild.
     */
    public <T> T getHotObject(String key, String lockKey, Class<T> type, Supplier<T> databaseLoader) {
        CacheLookup<LogicalCacheValue<T>> initial = readLogicalObject(key, type);
        if (initial.hit) {
            if (initial.value == null) {
                metrics.count("cache_null_hit_total", "catalog", "hot_read", "success", "none");
                return null;
            }
            if (!initial.value.isExpired()) {
                metrics.count("cache_hot_hit_total", "catalog", "hot_read", "success", "none");
                return initial.value.getData();
            }
            metrics.count("cache_hot_stale_return_total", "catalog", "hot_read", "success", "LOGICAL_EXPIRED");
            rebuildHotAsync(key, lockKey, type, databaseLoader);
            return initial.value.getData();
        }
        metrics.count("cache_miss_total", "catalog", "hot_read", "miss", "none");
        if (!initial.available) {
            return databaseLoader.get();
        }
        return rebuildHotSynchronously(key, lockKey, type, databaseLoader);
    }

    public void evictAfterCommit(final Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        final List<String> distinctKeys = new ArrayList<String>(new java.util.LinkedHashSet<String>(keys));
        if (TransactionSynchronizationManager.isActualTransactionActive() && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict(distinctKeys);
                }
            });
            return;
        }
        evict(distinctKeys);
    }

    public void evict(Collection<String> keys) {
        int retries = Math.max(1, properties.getEvictMaxRetries());
        for (int attempt = 1; attempt <= retries; attempt++) {
            try { redisTemplate.delete(keys); return; }
            catch (Exception exception) {
                metrics.count("cache_evict_failure_total", "catalog", "evict", "failure", "CACHE_EVICT_FAILED");
                log.warn("目录缓存删除失败，keyCount={}，attempt={}, type={}", keys == null ? 0 : keys.size(), attempt, exception.getClass().getSimpleName());
                if (attempt < retries) waitForEvictRetry();
            }
        }
    }

    private <T> T get(String key, String lockKey, Reader<T> reader, Writer<T> writer, Supplier<T> databaseLoader) {
        CacheLookup<T> initial = reader.read();
        if (initial.hit) {
            metrics.count(initial.value == null ? "cache_null_hit_total" : "cache_hit_total", "catalog", "read", "success", "none");
            return initial.value;
        }
        metrics.count("cache_miss_total", "catalog", "read", "miss", "none");
        if (!initial.available) {
            return databaseLoader.get();
        }
        for (int attempt = 0; attempt < properties.getLockMaxRetries(); attempt++) {
            RLock lock = null;
            boolean locked = false;
            try {
                lock = redissonClient.getLock(lockKey);
                long started = System.nanoTime(); locked = lock.tryLock(properties.getLockWaitMillis(), TimeUnit.MILLISECONDS);
                metrics.lockWait("catalog", "rebuild", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
                if (locked) {
                    metrics.count("redis_lock_acquire_total", "catalog", "rebuild", "success", "none");
                    CacheLookup<T> doubleChecked = reader.read();
                    if (doubleChecked.hit) {
                        return doubleChecked.value;
                    }
                    if (!doubleChecked.available) {
                        return databaseLoader.get();
                    }
                    T value = databaseLoader.get();
                    writer.write(value);
                    metrics.count("cache_rebuild_total", "catalog", "rebuild", "success", "none");
                    return value;
                }
                metrics.count("redis_lock_acquire_failure_total", "catalog", "rebuild", "failure", "LOCK_ACQUIRE_FAILED");
                waitBeforeRetry();
                CacheLookup<T> retry = reader.read();
                if (retry.hit) {
                    return retry.value;
                }
                if (!retry.available) {
                    return databaseLoader.get();
                }
            } catch (Exception exception) {
                log.warn("目录缓存互斥重建降级查询，key={}，type={}", key, exception.getClass().getSimpleName());
                return databaseLoader.get();
            } finally {
                if (locked && lock != null) {
                    try {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    } catch (Exception exception) {
                        log.warn("目录缓存锁释放失败，key={}，type={}", key, exception.getClass().getSimpleName());
                    }
                }
            }
        }
        return databaseLoader.get();
    }

    private <T> T rebuildHotSynchronously(String key, String lockKey, Class<T> type, Supplier<T> databaseLoader) {
        for (int attempt = 0; attempt < properties.getLockMaxRetries(); attempt++) {
            RLock lock = null;
            boolean locked = false;
            try {
                lock = redissonClient.getLock(lockKey);
                long started = System.nanoTime(); locked = lock.tryLock(properties.getLockWaitMillis(), TimeUnit.MILLISECONDS);
                metrics.lockWait("catalog", "hot_rebuild", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
                if (locked) {
                    metrics.count("redis_lock_acquire_total", "catalog", "hot_rebuild", "success", "none");
                    CacheLookup<LogicalCacheValue<T>> doubleChecked = readLogicalObject(key, type);
                    if (doubleChecked.hit && doubleChecked.value == null) return null;
                    if (doubleChecked.hit && !doubleChecked.value.isExpired()) return doubleChecked.value.getData();
                    if (!doubleChecked.available) return databaseLoader.get();
                    T value = databaseLoader.get();
                    writeLogicalObject(key, value);
                    metrics.count("cache_hot_rebuild_total", "catalog", "hot_rebuild", "success", "none");
                    return value;
                }
                metrics.count("redis_lock_acquire_failure_total", "catalog", "hot_rebuild", "failure", "LOCK_ACQUIRE_FAILED");
                waitBeforeRetry();
                CacheLookup<LogicalCacheValue<T>> retry = readLogicalObject(key, type);
                if (retry.hit) return retry.value == null ? null : retry.value.getData();
                if (!retry.available) return databaseLoader.get();
            } catch (Exception exception) {
                log.warn("热点目录缓存同步重建降级查询，key={}，type={}", key, exception.getClass().getSimpleName());
                return databaseLoader.get();
            } finally {
                unlock(lock, locked, key);
            }
        }
        return databaseLoader.get();
    }

    private <T> void rebuildHotAsync(final String key, final String lockKey, final Class<T> type, final Supplier<T> databaseLoader) {
        try {
            logicalRebuildExecutor.execute(new Runnable() {
                @Override public void run() {
                    RLock lock = null;
                    boolean locked = false;
                    try {
                        lock = redissonClient.getLock(lockKey);
                        locked = lock.tryLock(0L, TimeUnit.MILLISECONDS);
                        if (!locked) return;
                        metrics.count("redis_lock_acquire_total", "catalog", "hot_async_rebuild", "success", "none");
                        CacheLookup<LogicalCacheValue<T>> current = readLogicalObject(key, type);
                        if (!current.available || (current.hit && (current.value == null || !current.value.isExpired()))) return;
                        T value = databaseLoader.get();
                        writeLogicalObject(key, value);
                        metrics.count("cache_hot_rebuild_total", "catalog", "hot_async_rebuild", "success", "none");
                    } catch (Exception exception) {
                        metrics.count("cache_hot_rebuild_failure_total", "catalog", "hot_async_rebuild", "failure", exception.getClass().getSimpleName());
                        log.warn("热点目录缓存异步重建失败，key={}，type={}", key, exception.getClass().getSimpleName());
                    } finally {
                        unlock(lock, locked, key);
                    }
                }
            });
        } catch (Exception exception) {
            metrics.count("cache_hot_rebuild_failure_total", "catalog", "hot_async_submit", "failure", exception.getClass().getSimpleName());
            log.warn("热点目录缓存异步重建提交失败，key={}，type={}", key, exception.getClass().getSimpleName());
        }
    }

    private <T> CacheLookup<T> readObject(String key, Class<T> type) {
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                return CacheLookup.miss();
            }
            if (NULL_MARKER.equals(cached)) {
                return CacheLookup.hit(null);
            }
            return CacheLookup.hit(objectMapper.readValue(cached, type));
        } catch (Exception exception) {
            deleteCorruptKey(key, exception);
            return CacheLookup.unavailable();
        }
    }

    private <T> CacheLookup<List<T>> readList(String key, Class<T> elementType) {
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                return CacheLookup.miss();
            }
            if (NULL_MARKER.equals(cached)) {
                return CacheLookup.hit(Collections.<T>emptyList());
            }
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return CacheLookup.hit(objectMapper.readValue(cached, listType));
        } catch (Exception exception) {
            deleteCorruptKey(key, exception);
            return CacheLookup.unavailable();
        }
    }

    private <T> CacheLookup<LogicalCacheValue<T>> readLogicalObject(String key, Class<T> type) {
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached == null) return CacheLookup.miss();
            if (NULL_MARKER.equals(cached)) return CacheLookup.hit(null);
            JavaType envelopeType = objectMapper.getTypeFactory().constructParametricType(LogicalCacheValue.class, type);
            LogicalCacheValue<T> value = objectMapper.readValue(cached, envelopeType);
            if (value == null || value.getData() == null || value.getLogicalExpireTime() == null) throw new IllegalStateException("invalid logical cache envelope");
            return CacheLookup.hit(value);
        } catch (Exception exception) {
            deleteCorruptKey(key, exception);
            return CacheLookup.unavailable();
        }
    }

    private <T> void writeObject(String key, T value, long ttlMinutes) {
        write(key, value, value == null ? properties.getNullTtlMinutes() : jitteredTtl(ttlMinutes));
    }

    private <T> void writeList(String key, List<T> value, long ttlMinutes) {
        boolean empty = value == null || value.isEmpty();
        write(key, empty ? null : value, empty ? properties.getNullTtlMinutes() : jitteredTtl(ttlMinutes));
    }

    private void write(String key, Object value, long ttlMinutes) {
        try {
            String serialized = value == null ? NULL_MARKER : objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, serialized, ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception exception) {
            log.warn("目录缓存写入失败，key={}，type={}", key, exception.getClass().getSimpleName());
        }
    }

    private <T> void writeLogicalObject(String key, T value) {
        if (value == null) {
            write(key, null, properties.getNullTtlMinutes());
            return;
        }
        try {
            long logicalTtl = Math.max(1L, properties.getHotShopLogicalTtlMinutes());
            long physicalTtl = Math.max(logicalTtl + 1L, properties.getHotShopPhysicalTtlMinutes());
            LogicalCacheValue<T> envelope = new LogicalCacheValue<T>();
            envelope.setData(value);
            envelope.setLogicalExpireTime(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(logicalTtl));
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(envelope), physicalTtl, TimeUnit.MINUTES);
        } catch (Exception exception) {
            log.warn("热点目录缓存写入失败，key={}，type={}", key, exception.getClass().getSimpleName());
        }
    }

    private long jitteredTtl(long ttlMinutes) {
        long jitter = properties.getTtlJitterMinutes();
        return jitter <= 0 ? ttlMinutes : ttlMinutes + ThreadLocalRandom.current().nextLong(jitter + 1);
    }

    private void waitBeforeRetry() {
        try {
            Thread.sleep(Math.max(1L, Math.min(properties.getLockWaitMillis(), 50L)));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
    private void waitForEvictRetry() {
        try { Thread.sleep(Math.max(1L, Math.min(properties.getEvictRetryMillis(), 100L))); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }

    private void deleteCorruptKey(String key, Exception exception) {
        try {
            redisTemplate.delete(key);
            log.warn("目录缓存内容异常，已删除坏 Key，key={}，type={}", key, exception.getClass().getSimpleName());
        } catch (Exception deleteException) {
            log.warn("目录缓存读取和坏 Key 删除均失败，key={}，type={}", key, deleteException.getClass().getSimpleName());
        }
    }

    private void unlock(RLock lock, boolean locked, String key) {
        if (!locked || lock == null) return;
        try {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        } catch (Exception exception) {
            log.warn("目录缓存锁释放失败，key={}，type={}", key, exception.getClass().getSimpleName());
        }
    }

    private interface Reader<T> { CacheLookup<T> read(); }
    private interface Writer<T> { void write(T value); }

    private static class CacheLookup<T> {
        private final boolean available;
        private final boolean hit;
        private final T value;
        private CacheLookup(boolean available, boolean hit, T value) { this.available = available; this.hit = hit; this.value = value; }
        private static <T> CacheLookup<T> hit(T value) { return new CacheLookup<T>(true, true, value); }
        private static <T> CacheLookup<T> miss() { return new CacheLookup<T>(true, false, null); }
        private static <T> CacheLookup<T> unavailable() { return new CacheLookup<T>(false, false, null); }
    }

    public static class LogicalCacheValue<T> {
        private T data;
        private Long logicalExpireTime;
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public Long getLogicalExpireTime() { return logicalExpireTime; }
        public void setLogicalExpireTime(Long logicalExpireTime) { this.logicalExpireTime = logicalExpireTime; }
        @JsonIgnore
        public boolean isExpired() { return logicalExpireTime != null && logicalExpireTime <= System.currentTimeMillis(); }
    }
}
