package com.qinghe.life;

import com.qinghe.life.utils.RedisIdWorker;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Verifies worker bit composition and thread safety; Redis INCR is mocked as its atomic contract. */
class RedisIdWorkerUnitTest {
    @Test void concurrentCallsProduceNoDuplicateIds() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        AtomicLong sequence = new AtomicLong();
        when(redis.execute(any(), anyList(), anyString())).thenAnswer(call -> sequence.incrementAndGet());
        RedisIdWorker worker = new RedisIdWorker(redis);
        int total = 2000; ExecutorService pool = Executors.newFixedThreadPool(16); CountDownLatch start = new CountDownLatch(1);
        Set<Long> ids = Collections.synchronizedSet(new HashSet<Long>());
        for (int index = 0; index < total; index++) pool.submit(() -> { start.await(); ids.add(worker.nextSeckillOrderId()); return null; });
        start.countDown(); pool.shutdown(); org.junit.jupiter.api.Assertions.assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(total, ids.size());
    }
}
