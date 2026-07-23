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
    public static String shopDetail(Long shopId) { return "qh:shop:detail:" + shopId; }
    public static String shopNull(Long shopId) { return "qh:shop:null:" + shopId; }
    public static String shopLock(Long shopId) { return "qh:lock:shop:" + shopId; }
}
