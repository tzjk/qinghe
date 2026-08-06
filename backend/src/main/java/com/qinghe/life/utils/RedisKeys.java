package com.qinghe.life.utils;

/** All runtime keys share a configurable namespace; default keeps legacy qh: compatibility. */
public final class RedisKeys {
    public static final long LOGIN_CODE_TTL_MINUTES = 2L;
    public static final long LOGIN_TOKEN_TTL_MINUTES = 30L;
    public static final long ADMIN_TOKEN_TTL_MINUTES = 30L;
    private static volatile String namespace = "qh:";
    private RedisKeys() { }
    public static void configureNamespace(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Redis namespace must not be empty");
        namespace = value.trim().endsWith(":") ? value.trim() : value.trim() + ":";
    }
    public static String namespace() { return namespace; }
    public static String code(String phone) { return key("login:code:") + phone; }
    public static String token(String token) { return key("login:token:") + token; }
    public static String adminToken(String token) { return key("admin:token:") + token; }
    public static String exploreHot() { return key("zset:explore:hot"); }
    public static String shopGeo() { return key("geo:shop"); }
    public static String shopDetail(Long shopId) { return key("cache:shop:") + shopId; }
    public static String goodsDetail(Long goodsId) { return key("cache:goods:") + goodsId; }
    public static String shopGoods(Long shopId) { return key("cache:shop-goods:") + shopId; }
    public static String shopCategory(Long shopId) { return key("cache:shop-category:") + shopId; }
    public static String shopLock(Long shopId) { return key("lock:cache:shop:") + shopId; }
    public static String goodsLock(Long goodsId) { return key("lock:cache:goods:") + goodsId; }
    public static String shopGoodsLock(Long shopId) { return key("lock:cache:shop-goods:") + shopId; }
    @Deprecated public static String shopNull(Long shopId) { return shopDetail(shopId); }
    public static String seckillStock(Long couponId) { return key("seckill:stock:") + couponId; }
    public static String seckillUsers(Long couponId) { return key("seckill:users:") + couponId; }
    public static String seckillMeta(Long couponId) { return key("seckill:meta:") + couponId; }
    public static String seckillStream() { return key("stream:seckill-orders"); }
    public static String seckillStatus(Long orderId) { return key("seckill:order:status:") + orderId; }
    public static String seckillLock(Long couponId, Long userId) { return key("lock:seckill:") + couponId + ":" + userId; }
    public static String seckillId(java.time.LocalDate date) { return key("id:seckill:") + date; }
    public static String seckillInitLock(Long couponId) { return key("lock:seckill:init:") + couponId; }
    public static String couponSeckillDlqStream() { return key("stream:seckill-orders:dlq"); }
    public static String couponSeckillDlqIndex(String messageId) { return key("seckill:dlq-index:") + messageId; }
    public static String couponSeckillRetry(String messageId) { return key("seckill:retry:") + messageId; }
    public static String couponSeckillRecoveryLock() { return key("lock:seckill:pending-recovery"); }
    public static String seckillTestRunMeta(String runId) { return key("seckill:test:run:") + runId + ":meta"; }
    public static String seckillTestRunCoupons(String runId) { return key("seckill:test:run:") + runId + ":coupons"; }
    public static String seckillTestRunUsers(String runId) { return key("seckill:test:run:") + runId + ":users"; }
    public static String seckillTestRunTokens(String runId) { return key("seckill:test:run:") + runId + ":tokens"; }
    public static String seckillTestRunStreamIds(String runId) { return key("seckill:test:run:") + runId + ":stream-ids"; }
    public static String seckillTestRunAckIds(String runId) { return key("seckill:test:run:") + runId + ":ack-ids"; }
    public static String seckillTestRunDlqIds(String runId) { return key("seckill:test:run:") + runId + ":dlq-ids"; }
    public static String seckillTestRunOrderIds(String runId) { return key("seckill:test:run:") + runId + ":order-ids"; }
    public static String seckillTestRunStatusKeys(String runId) { return key("seckill:test:run:") + runId + ":status-keys"; }
    public static String seckillTestRunLockKeys(String runId) { return key("seckill:test:run:") + runId + ":lock-keys"; }
    /* Compatibility aliases for the earlier coupon-claim implementation. */
    public static String couponSeckillStock(Long couponId) { return seckillStock(couponId); }
    public static String couponSeckillUsers(Long couponId) { return seckillUsers(couponId); }
    public static String couponSeckillMeta(Long couponId) { return seckillMeta(couponId); }
    public static String couponSeckillReservation(String orderId) { return seckillStatus(Long.valueOf(orderId)); }
    public static String couponSeckillStream() { return seckillStream(); }
    public static String orderTimeoutLock() { return key("lock:order:timeout-cancel"); }
    public static String signIn(Long userId, String yearMonth) { return key("sign:") + userId + ":" + yearMonth; }
    public static String followings(Long userId) { return key("followings:") + userId; }
    public static String followers(Long userId) { return key("followers:") + userId; }
    public static String followingsLoaded(Long userId) { return key("followings:loaded:") + userId; }
    public static String followersLoaded(Long userId) { return key("followers:loaded:") + userId; }
    public static String followCacheLock(Long userId) { return key("lock:follow-cache:") + userId; }
    public static String exploreLikers(Long postId) { return key("explore:likers:v2:") + postId; }
    public static String exploreLikersLoaded(Long postId) { return key("explore:likers:v2:loaded:") + postId; }
    public static String exploreLikersLock(Long postId) { return key("lock:explore-likers:v2:") + postId; }
    public static String followingFeed(Long userId) { return key("feed:") + userId; }
    private static String key(String suffix) { return namespace + suffix; }
}
