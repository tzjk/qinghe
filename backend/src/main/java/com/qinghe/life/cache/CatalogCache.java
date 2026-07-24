package com.qinghe.life.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinghe.life.config.CatalogCacheProperties;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
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

    public CatalogCache(StringRedisTemplate redisTemplate, RedissonClient redissonClient,
                        ObjectMapper objectMapper, CatalogCacheProperties properties) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public <T> T getObject(String key, String lockKey, Class<T> type, long ttlMinutes, Supplier<T> databaseLoader) {
        return get(key, lockKey, () -> readObject(key, type), value -> writeObject(key, value, ttlMinutes), databaseLoader);
    }

    public <T> List<T> getList(String key, String lockKey, Class<T> elementType, long ttlMinutes, Supplier<List<T>> databaseLoader) {
        List<T> result = get(key, lockKey, () -> readList(key, elementType), value -> writeList(key, value, ttlMinutes), databaseLoader);
        return result == null ? Collections.<T>emptyList() : result;
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
        try {
            redisTemplate.delete(keys);
        } catch (Exception exception) {
            log.warn("目录缓存删除失败，keyCount={}，type={}", keys == null ? 0 : keys.size(), exception.getClass().getSimpleName());
        }
    }

    private <T> T get(String key, String lockKey, Reader<T> reader, Writer<T> writer, Supplier<T> databaseLoader) {
        CacheLookup<T> initial = reader.read();
        if (initial.hit) {
            return initial.value;
        }
        if (!initial.available) {
            return databaseLoader.get();
        }
        for (int attempt = 0; attempt < properties.getLockMaxRetries(); attempt++) {
            RLock lock = null;
            boolean locked = false;
            try {
                lock = redissonClient.getLock(lockKey);
                locked = lock.tryLock(properties.getLockWaitMillis(), properties.getLockLeaseSeconds(), TimeUnit.SECONDS);
                if (locked) {
                    CacheLookup<T> doubleChecked = reader.read();
                    if (doubleChecked.hit) {
                        return doubleChecked.value;
                    }
                    if (!doubleChecked.available) {
                        return databaseLoader.get();
                    }
                    T value = databaseLoader.get();
                    writer.write(value);
                    return value;
                }
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

    private void deleteCorruptKey(String key, Exception exception) {
        try {
            redisTemplate.delete(key);
            log.warn("目录缓存内容异常，已删除坏 Key，key={}，type={}", key, exception.getClass().getSimpleName());
        } catch (Exception deleteException) {
            log.warn("目录缓存读取和坏 Key 删除均失败，key={}，type={}", key, deleteException.getClass().getSimpleName());
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
}
