package com.qinghe.life.utils;

public final class RedisKeys {
    public static final String LOGIN_CODE_KEY = "qh:login:code:";
    public static final long LOGIN_CODE_TTL_MINUTES = 2L;
    public static final String LOGIN_TOKEN_KEY = "qh:login:token:";
    public static final long LOGIN_TOKEN_TTL_MINUTES = 30L;
    public static final String ADMIN_TOKEN_KEY = "qh:admin:token:";
    public static final long ADMIN_TOKEN_TTL_MINUTES = 30L;

    private RedisKeys() {
    }

    public static String code(String phone) { return LOGIN_CODE_KEY + phone; }
    public static String token(String token) { return LOGIN_TOKEN_KEY + token; }
    public static String adminToken(String token) { return ADMIN_TOKEN_KEY + token; }
    public static String shopDetail(Long shopId) { return "qh:cache:shop:" + shopId; }
    public static String goodsDetail(Long goodsId) { return "qh:cache:goods:" + goodsId; }
    public static String shopGoods(Long shopId) { return "qh:cache:shop-goods:" + shopId; }
    public static String shopCategory(Long shopId) { return "qh:cache:shop-category:" + shopId; }
    public static String shopLock(Long shopId) { return "qh:lock:cache:shop:" + shopId; }
    public static String goodsLock(Long goodsId) { return "qh:lock:cache:goods:" + goodsId; }
    public static String shopGoodsLock(Long shopId) { return "qh:lock:cache:shop-goods:" + shopId; }
    @Deprecated
    public static String shopNull(Long shopId) { return shopDetail(shopId); }
    public static String couponSeckillStock(Long couponId) { return "qh:coupon:seckill:stock:" + couponId; }
    public static String couponSeckillUsers(Long couponId) { return "qh:coupon:seckill:users:" + couponId; }
    public static String couponSeckillMeta(Long couponId) { return "qh:coupon:seckill:meta:" + couponId; }
    public static String couponSeckillStream() { return "qh:stream:coupon:claim"; }
    public static String couponSeckillRetry() { return "qh:coupon:seckill:retry"; }
    public static String couponSeckillFailure() { return "qh:coupon:seckill:failure"; }
    public static String couponSeckillRecoveryLock() { return "qh:lock:coupon:seckill:pending-recovery"; }
}
