package com.qinghe.life;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinghe.life.cache.CatalogCache;
import com.qinghe.life.config.CatalogCacheProperties;
import com.qinghe.life.redis.RedisBusinessMetrics;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogCacheUnitTest {
    private final Map<String, String> values = new ConcurrentHashMap<String, String>();
    private final Map<String, Long> ttlMinutes = new ConcurrentHashMap<String, Long>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void missingValueUsesShortTtlNullMarkerAndPreventsRepeatLoad() {
        CatalogCache cache = cache(newLockingRedisson(), null);
        AtomicInteger loads = new AtomicInteger();
        assertNull(cache.getObject("qh:test:missing", "qh:lock:test:missing", String.class, 30L, () -> {
            loads.incrementAndGet();
            return null;
        }));
        assertNull(cache.getObject("qh:test:missing", "qh:lock:test:missing", String.class, 30L, () -> {
            loads.incrementAndGet();
            return "unexpected";
        }));
        assertEquals(1, loads.get());
        assertEquals(2L, ttlMinutes.get("qh:test:missing").longValue());
    }

    @Test
    void logicalExpiryReturnsStaleValueThenRebuildsAsyncWithPhysicalTtl() throws Exception {
        CatalogCache cache = cache(newLockingRedisson(), executor);
        putExpiredEnvelope("qh:test:hot", "old");
        AtomicInteger loads = new AtomicInteger();
        CountDownLatch rebuildStarted = new CountDownLatch(1);
        CountDownLatch allowRebuild = new CountDownLatch(1);

        assertEquals("old", cache.getHotObject("qh:test:hot", "qh:lock:test:hot", String.class, () -> {
            loads.incrementAndGet();
            rebuildStarted.countDown();
            await(allowRebuild);
            return "new";
        }));
        assertTrue(rebuildStarted.await(1, TimeUnit.SECONDS));
        assertEquals(1, loads.get(), "重建应在独立线程中开始");
        allowRebuild.countDown();
        awaitValue("qh:test:hot", "new");
        assertEquals(60L, ttlMinutes.get("qh:test:hot").longValue(), "逻辑过期缓存仍须有真实 TTL");
    }

    @Test
    void concurrentExpiredReadsScheduleOnlyOneActualRebuild() throws Exception {
        CatalogCache cache = cache(newLockingRedisson(), executor);
        putExpiredEnvelope("qh:test:hot-concurrent", "old");
        AtomicInteger loads = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        java.util.List<java.util.concurrent.Future<String>> results = new java.util.ArrayList<java.util.concurrent.Future<String>>();
        for (int i = 0; i < 12; i++) {
            results.add(executor.submit(() -> {
                start.await();
                return cache.getHotObject("qh:test:hot-concurrent", "qh:lock:test:hot-concurrent", String.class, () -> {
                    loads.incrementAndGet();
                    sleep(100L);
                    return "new";
                });
            }));
        }
        start.countDown();
        for (java.util.concurrent.Future<String> result : results) assertEquals("old", result.get(2, TimeUnit.SECONDS));
        awaitValue("qh:test:hot-concurrent", "new");
        assertEquals(1, loads.get(), "并发逻辑过期请求只能有一个线程重建");
    }

    @Test
    void evictionRunsAfterCommitAndRetriesFailedDelete() {
        StringRedisTemplate redis = redisTemplate();
        AtomicInteger deletes = new AtomicInteger();
        when(redis.delete(any(Collection.class))).thenAnswer(invocation -> {
            if (deletes.incrementAndGet() == 1) throw new IllegalStateException("temporary redis failure");
            return 1L;
        });
        CatalogCache cache = cache(redis, newLockingRedisson(), executor);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        cache.evictAfterCommit(Arrays.asList("qh:test:evict"));
        assertEquals(0, deletes.get(), "事务提交前不能删除缓存");
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        assertEquals(2, deletes.get(), "删除失败应按配置有限重试");
        verify(redis, times(2)).delete(any(Collection.class));
    }

    private CatalogCache cache(RedissonClient redisson, ExecutorService asyncExecutor) {
        return cache(redisTemplate(), redisson, asyncExecutor);
    }

    private CatalogCache cache(StringRedisTemplate redis, RedissonClient redisson, ExecutorService asyncExecutor) {
        return new CatalogCache(redis, redisson, new ObjectMapper(), properties(), new RedisBusinessMetrics(),
                asyncExecutor == null ? Runnable::run : asyncExecutor);
    }

    @SuppressWarnings("unchecked")
    private StringRedisTemplate redisTemplate() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operations);
        when(operations.get(anyString())).thenAnswer(invocation -> values.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            values.put(key, invocation.getArgument(1));
            ttlMinutes.put(key, invocation.getArgument(2));
            return null;
        }).when(operations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        when(redis.delete(anyString())).thenAnswer(invocation -> values.remove(invocation.getArgument(0)) != null);
        when(redis.delete(any(Collection.class))).thenAnswer(invocation -> {
            for (Object key : (Collection<?>) invocation.getArgument(0)) values.remove(String.valueOf(key));
            return 1L;
        });
        return redis;
    }

    private RedissonClient newLockingRedisson() {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        AtomicBoolean held = new AtomicBoolean(false);
        when(redisson.getLock(anyString())).thenReturn(lock);
        try {
            when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenAnswer((Answer<Boolean>) invocation -> held.compareAndSet(false, true));
        } catch (InterruptedException exception) {
            throw new AssertionError(exception);
        }
        when(lock.isHeldByCurrentThread()).thenAnswer(invocation -> held.get());
        doAnswer(invocation -> { held.set(false); return null; }).when(lock).unlock();
        return redisson;
    }

    private CatalogCacheProperties properties() {
        CatalogCacheProperties value = new CatalogCacheProperties();
        value.setNullTtlMinutes(2L);
        value.setLockWaitMillis(20L);
        value.setLockMaxRetries(2);
        value.setEvictMaxRetries(2);
        value.setEvictRetryMillis(1L);
        value.setHotShopLogicalTtlMinutes(10L);
        value.setHotShopPhysicalTtlMinutes(60L);
        return value;
    }

    private void putExpiredEnvelope(String key, String data) throws Exception {
        CatalogCache.LogicalCacheValue<String> envelope = new CatalogCache.LogicalCacheValue<String>();
        envelope.setData(data);
        envelope.setLogicalExpireTime(System.currentTimeMillis() - 1000L);
        values.put(key, new ObjectMapper().writeValueAsString(envelope));
    }

    private void awaitValue(String key, String expected) throws Exception {
        long deadline = System.currentTimeMillis() + 2000L;
        ObjectMapper mapper = new ObjectMapper();
        while (System.currentTimeMillis() < deadline) {
            String raw = values.get(key);
            if (raw != null) {
                CatalogCache.LogicalCacheValue<?> value = mapper.readValue(raw, CatalogCache.LogicalCacheValue.class);
                if (expected.equals(value.getData())) return;
            }
            sleep(10L);
        }
        fail("异步重建未在时限内完成");
    }

    private static void await(CountDownLatch latch) {
        try { latch.await(2, TimeUnit.SECONDS); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }
    private static void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }
}
