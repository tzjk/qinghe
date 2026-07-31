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
    public static String couponSeckillStock(Long couponId) { return key("coupon:seckill:stock:") + couponId; }
    public static String couponSeckillUsers(Long couponId) { return key("coupon:seckill:users:") + couponId; }
    public static String couponSeckillMeta(Long couponId) { return key("coupon:seckill:meta:") + couponId; }
    public static String couponSeckillReservation(String orderId) { return key("coupon:seckill:reservation:") + orderId; }
    public static String couponSeckillStream() { return key("stream:coupon:claim"); }
    public static String couponSeckillDlqStream() { return key("stream:coupon:claim:dlq"); }
    public static String couponSeckillDlqIndex(String messageId) { return key("coupon:seckill:dlq-index:") + messageId; }
    public static String couponSeckillRetry(String messageId) { return key("coupon:seckill:retry:") + messageId; }
    public static String couponSeckillRecoveryLock() { return key("lock:coupon:seckill:pending-recovery"); }
    public static String orderTimeoutLock() { return key("lock:order:timeout-cancel"); }
    private static String key(String suffix) { return namespace + suffix; }
}
