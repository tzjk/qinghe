package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinghe.life.cache.CatalogCache;
import com.qinghe.life.config.CatalogCacheProperties;
import com.qinghe.life.entity.Admin;
import com.qinghe.life.entity.Category;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.mapper.AdminMapper;
import com.qinghe.life.mapper.CategoryMapper;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.oss.AliyunOSSOperator;
import com.qinghe.life.utils.RedisKeys;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogCacheIntegrationTest {
    private static final String MARKER = "CATALOG_CACHE_TEST_";
    private static final String ADMIN_TOKEN = MARKER + "ADMIN_TOKEN";

    @Autowired private MockMvc mvc;
    @Autowired private CatalogCache catalogCache;
    @Autowired private CatalogCacheProperties cacheProperties;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AdminMapper adminMapper;
    @Autowired private CategoryMapper categoryMapper;
    @Autowired private ShopMapper shopMapper;
    @Autowired private GoodsMapper goodsMapper;
    @MockBean private AliyunOSSOperator oss;

    private Admin admin;
    private Category category;
    private Shop shopA;
    private Shop shopB;
    private Goods goods;

    @BeforeEach
    void setUp() {
        cleanup();
        category = new Category();
        category.setName(MARKER + "分类");
        category.setStatus(1);
        category.setSortOrder(1);
        categoryMapper.insert(category);
        shopA = newShop(MARKER + "店铺A");
        shopB = newShop(MARKER + "店铺B");
        shopMapper.insert(shopA);
        shopMapper.insert(shopB);
        goods = new Goods();
        goods.setShopId(shopA.getId());
        goods.setName(MARKER + "商品");
        goods.setDescription("缓存测试商品");
        goods.setPrice(new BigDecimal("12.50"));
        goods.setStock(9);
        goods.setSalesCount(1);
        goods.setSaleStatus("ON_SALE");
        goodsMapper.insert(goods);
        admin = new Admin();
        admin.setUsername(MARKER + "ADMIN");
        admin.setDisplayName(MARKER + "管理员");
        admin.setPasswordHash("not-used-by-this-test");
        admin.setStatus(1);
        adminMapper.insert(admin);
        Map<String, String> session = new HashMap<String, String>();
        session.put("adminId", String.valueOf(admin.getId()));
        session.put("username", admin.getUsername());
        session.put("displayName", admin.getDisplayName());
        redis.opsForHash().putAll(RedisKeys.adminToken(ADMIN_TOKEN), session);
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    @Test
    void publicReadsPopulateAndReuseShopGoodsAndListCachesWithJitteredTtl() throws Exception {
        mvc.perform(get("/api/shops/{id}", shopA.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(shopA.getName()));
        mvc.perform(get("/api/goods/{id}", goods.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(goods.getName()));
        mvc.perform(get("/api/shops/{id}/goods", shopA.getId()).param("page", "1").param("size", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.records[0].id").value(goods.getId()));
        assertTrue(Boolean.TRUE.equals(redis.hasKey(RedisKeys.shopDetail(shopA.getId()))));
        assertTrue(Boolean.TRUE.equals(redis.hasKey(RedisKeys.goodsDetail(goods.getId()))));
        assertTrue(Boolean.TRUE.equals(redis.hasKey(RedisKeys.shopGoods(shopA.getId()))));
        Long shopTtl = redis.getExpire(RedisKeys.shopDetail(shopA.getId()), TimeUnit.MINUTES);
        assertTrue(shopTtl != null && shopTtl > cacheProperties.getHotShopLogicalTtlMinutes()
                && shopTtl <= cacheProperties.getHotShopPhysicalTtlMinutes());
        String cachedBefore = redis.opsForValue().get(RedisKeys.goodsDetail(goods.getId()));
        mvc.perform(get("/api/goods/{id}", goods.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(goods.getName()));
        assertEquals(cachedBefore, redis.opsForValue().get(RedisKeys.goodsDetail(goods.getId())));

        Set<Long> observedTtls = new HashSet<Long>();
        for (long id = 910000L; id < 910012L; id++) {
            final long currentId = id;
            catalogCache.getObject(RedisKeys.goodsDetail(currentId), RedisKeys.goodsLock(currentId), String.class,
                    cacheProperties.getGoodsTtlMinutes(), () -> "ttl-" + currentId);
            observedTtls.add(redis.getExpire(RedisKeys.goodsDetail(currentId), TimeUnit.MINUTES));
        }
        assertTrue(observedTtls.size() > 1, "正常缓存 TTL 应有随机抖动");
    }

    @Test
    void missingValuesAndEmptyShopListUseShortLivedNullMarkers() throws Exception {
        long absentShopId = 920001L;
        long absentGoodsId = 920002L;
        mvc.perform(get("/api/shops/{id}", absentShopId)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        mvc.perform(get("/api/shops/{id}", absentShopId)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        mvc.perform(get("/api/goods/{id}", absentGoodsId)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        assertTrue(Boolean.TRUE.equals(redis.hasKey(RedisKeys.shopDetail(absentShopId))));
        assertTrue(Boolean.TRUE.equals(redis.hasKey(RedisKeys.goodsDetail(absentGoodsId))));
        assertTrue(redis.getExpire(RedisKeys.shopDetail(absentShopId), TimeUnit.MINUTES) <= cacheProperties.getNullTtlMinutes());

        mvc.perform(get("/api/shops/{id}/goods", shopB.getId()).param("page", "1").param("size", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/api/shops/{id}/goods", shopB.getId()).param("page", "1").param("size", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        assertTrue(Boolean.TRUE.equals(redis.hasKey(RedisKeys.shopGoods(shopB.getId()))));
        assertTrue(redis.getExpire(RedisKeys.shopGoods(shopB.getId()), TimeUnit.MINUTES) <= cacheProperties.getNullTtlMinutes());
    }

    @Test
    void concurrentExpiredHotKeysReturnStaleDataAndRebuildOnlyOnce() throws Exception {
        long firstId = 930001L;
        String key = RedisKeys.shopDetail(firstId);
        putExpiredHotValue(key, "stale");
        AtomicInteger rebuildLoads = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(6);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<Future<String>>();
        for (int i = 0; i < 6; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return catalogCache.getHotObject(key, RedisKeys.shopLock(firstId), String.class, () -> {
                    rebuildLoads.incrementAndGet();
                    sleep(100L);
                    return "fresh";
                });
            }));
        }
        start.countDown();
        for (Future<String> future : futures) {
            assertEquals("stale", future.get(3, TimeUnit.SECONDS));
        }
        awaitFreshHotValue(key, "fresh");
        assertEquals(1, rebuildLoads.get());
        Long ttl = redis.getExpire(key, TimeUnit.MINUTES);
        assertTrue(ttl != null && ttl > cacheProperties.getHotShopLogicalTtlMinutes()
                && ttl <= cacheProperties.getHotShopPhysicalTtlMinutes());
        pool.shutdownNow();
    }

    @Test
    void corruptCacheFallsBackAndRedisFailureDoesNotBlockDatabaseLoader() throws Exception {
        redis.opsForValue().set(RedisKeys.shopDetail(shopA.getId()), "{not-json");
        mvc.perform(get("/api/shops/{id}", shopA.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(shopA.getName()));
        assertNotEquals("{not-json", redis.opsForValue().get(RedisKeys.shopDetail(shopA.getId())));

        StringRedisTemplate unavailableRedis = mock(StringRedisTemplate.class);
        when(unavailableRedis.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));
        CatalogCache degradedCache = new CatalogCache(unavailableRedis, mock(org.redisson.api.RedissonClient.class),
                new ObjectMapper(), cacheProperties, mock(com.qinghe.life.redis.RedisBusinessMetrics.class));
        assertEquals("mysql", degradedCache.getObject("qh:cache:shop:940001", "qh:lock:cache:shop:940001", String.class,
                cacheProperties.getShopTtlMinutes(), () -> "mysql"));
    }

    @Test
    void successfulAdminWritesEvictAffectedDetailsAndBothShopLists() throws Exception {
        primeCaches();
        mvc.perform(put("/api/admin/goods/{id}", goods.getId()).header("Authorization", adminAuthorization())
                        .contentType("application/json").content(goodsRequest(shopA.getId(), MARKER + "商品已修改", "13.50", "ON_SALE")))
                .andExpect(status().isOk());
        assertFalse(Boolean.TRUE.equals(redis.hasKey(RedisKeys.goodsDetail(goods.getId()))));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(RedisKeys.shopGoods(shopA.getId()))));

        primeCaches();
        mvc.perform(put("/api/admin/goods/{id}", goods.getId()).header("Authorization", adminAuthorization())
                        .contentType("application/json").content(goodsRequest(shopB.getId(), MARKER + "商品换店", "13.50", "ON_SALE")))
                .andExpect(status().isOk());
        assertFalse(Boolean.TRUE.equals(redis.hasKey(RedisKeys.goodsDetail(goods.getId()))));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(RedisKeys.shopGoods(shopA.getId()))));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(RedisKeys.shopGoods(shopB.getId()))));

        mvc.perform(get("/api/shops/{id}/goods", shopB.getId()).param("page", "1").param("size", "20"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/admin/goods/{id}/status", goods.getId()).header("Authorization", adminAuthorization())
                        .contentType("application/json").content("{\"saleStatus\":\"OFF_SALE\"}"))
                .andExpect(status().isOk());
        assertFalse(Boolean.TRUE.equals(redis.hasKey(RedisKeys.shopGoods(shopB.getId()))));

        mvc.perform(get("/api/shops/{id}", shopA.getId())).andExpect(status().isOk());
        mvc.perform(put("/api/admin/shops/{id}", shopA.getId()).header("Authorization", adminAuthorization())
                        .contentType("application/json").content(shopRequest(MARKER + "店铺A已修改")))
                .andExpect(status().isOk());
        assertFalse(Boolean.TRUE.equals(redis.hasKey(RedisKeys.shopDetail(shopA.getId()))));
    }

    private void primeCaches() throws Exception {
        mvc.perform(get("/api/shops/{id}", shopA.getId())).andExpect(status().isOk());
        mvc.perform(get("/api/shops/{id}", shopB.getId())).andExpect(status().isOk());
        mvc.perform(get("/api/goods/{id}", goods.getId())).andExpect(status().isOk());
        mvc.perform(get("/api/shops/{id}/goods", shopA.getId()).param("page", "1").param("size", "20")).andExpect(status().isOk());
        mvc.perform(get("/api/shops/{id}/goods", shopB.getId()).param("page", "1").param("size", "20")).andExpect(status().isOk());
    }

    private void putExpiredHotValue(String key, String data) throws Exception {
        CatalogCache.LogicalCacheValue<String> value = new CatalogCache.LogicalCacheValue<String>();
        value.setData(data);
        value.setLogicalExpireTime(System.currentTimeMillis() - 1_000L);
        redis.opsForValue().set(key, objectMapper.writeValueAsString(value),
                cacheProperties.getHotShopPhysicalTtlMinutes(), TimeUnit.MINUTES);
    }

    private void awaitFreshHotValue(String key, String expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3L);
        while (System.nanoTime() < deadline) {
            String raw = redis.opsForValue().get(key);
            if (raw != null) {
                CatalogCache.LogicalCacheValue<?> value = objectMapper.readValue(raw, CatalogCache.LogicalCacheValue.class);
                if (expected.equals(value.getData()) && value.getLogicalExpireTime() != null
                        && value.getLogicalExpireTime() > System.currentTimeMillis()) {
                    return;
                }
            }
            sleep(10L);
        }
        assertTrue(false, "logical-expiry cache was not asynchronously rebuilt within three seconds");
    }

    private Shop newShop(String name) {
        Shop value = new Shop();
        value.setName(name);
        value.setCategoryId(category.getId());
        value.setAddress("校园商业街缓存测试");
        value.setScore(new BigDecimal("4.50"));
        value.setStatus(1);
        value.setIsFeatured(0);
        value.setSortOrder(1);
        return value;
    }

    private String adminAuthorization() { return "Bearer " + ADMIN_TOKEN; }
    private String goodsRequest(Long shopId, String name, String price, String saleStatus) { return "{\"shopId\":" + shopId + ",\"name\":\"" + name + "\",\"description\":\"缓存测试\",\"price\":" + price + ",\"stock\":9,\"saleStatus\":\"" + saleStatus + "\"}"; }
    private String shopRequest(String name) { return "{\"name\":\"" + name + "\",\"categoryId\":" + category.getId() + ",\"address\":\"校园商业街缓存测试\",\"score\":4.50,\"status\":1,\"isFeatured\":0,\"sortOrder\":1}"; }
    private static void sleep(long millis) { try { Thread.sleep(millis); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); } }

    private void cleanup() {
        if (goodsMapper != null) {
            for (Goods item : goodsMapper.selectList(Wrappers.<Goods>lambdaQuery().likeRight(Goods::getName, MARKER))) {
                redis.delete(Arrays.asList(RedisKeys.goodsDetail(item.getId()), RedisKeys.shopGoods(item.getShopId())));
                goodsMapper.deleteById(item.getId());
            }
        }
        if (shopMapper != null) {
            for (Shop item : shopMapper.selectList(Wrappers.<Shop>lambdaQuery().likeRight(Shop::getName, MARKER))) {
                redis.delete(Arrays.asList(RedisKeys.shopDetail(item.getId()), RedisKeys.shopGoods(item.getId())));
                shopMapper.deleteById(item.getId());
            }
        }
        for (long id = 910000L; id < 910012L; id++) redis.delete(RedisKeys.goodsDetail(id));
        for (long id : new long[] {920001L, 920002L, 930001L, 930002L, 930011L, 940001L}) {
            redis.delete(Arrays.asList(RedisKeys.shopDetail(id), RedisKeys.goodsDetail(id)));
        }
        if (categoryMapper != null) categoryMapper.delete(Wrappers.<Category>lambdaQuery().likeRight(Category::getName, MARKER));
        if (adminMapper != null) adminMapper.delete(Wrappers.<Admin>lambdaQuery().likeRight(Admin::getUsername, MARKER));
        if (redis != null) redis.delete(RedisKeys.adminToken(ADMIN_TOKEN));
    }
}
