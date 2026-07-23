package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jayway.jsonpath.JsonPath;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.entity.User;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.service.ShopService;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class UserAuthenticationIntegrationTest {
    private static final String TEST_MARKER = "M2A_REDIS_LOGIN_TEST_";
    private static final String PHONE = "13900009981";
    private static final String LEGACY_FIXED_CODE = "123456";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private StringRedisTemplate redis;
    @Autowired private MockMvc mvc;
    @Autowired private UserMapper users;
    @Autowired private ShopMapper shops;
    @Autowired private ShopService shopService;

    private String token;

    @BeforeEach
    void removeResidualTestData() {
        cleanup();
    }

    @AfterEach
    void cleanup() {
        if (token != null) redis.delete(RedisKeys.token(token));
        redis.delete(RedisKeys.code(PHONE));
        User user = users.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, PHONE));
        if (user != null && user.getNickname() != null && user.getNickname().startsWith(TEST_MARKER)) users.deleteById(user.getId());
        UserContext.clear();
    }

    @Test
    void randomCodeTokenHashRefreshAndOneTimeUseFlow() throws Exception {
        assertEquals(1, jdbc.queryForObject("SELECT 1", Integer.class));
        RedisConnection connection = redis.getConnectionFactory().getConnection();
        try { assertEquals("PONG", connection.ping()); } finally { connection.close(); }

        mvc.perform(post("/api/user/code").contentType("application/json").content("{\"phone\":\"123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/user/login").contentType("application/json")
                        .content(loginJson(LEGACY_FIXED_CODE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("验证码已过期或尚未获取"));

        requestCode();
        String firstCode = currentCode();
        requestCode();
        String secondCode = currentCode();
        assertTrue(firstCode.matches("\\d{6}"));
        assertTrue(secondCode.matches("\\d{6}"));
        assertNotEquals(LEGACY_FIXED_CODE, secondCode);
        String codeKey = RedisKeys.code(PHONE);
        assertTrue(Boolean.TRUE.equals(redis.hasKey(codeKey)));
        assertEquals(DataType.STRING, redis.type(codeKey));
        Long codeTtl = redis.getExpire(codeKey, TimeUnit.SECONDS);
        assertTrue(codeTtl != null && codeTtl > 0 && codeTtl <= TimeUnit.MINUTES.toSeconds(RedisKeys.LOGIN_CODE_TTL_MINUTES));

        mvc.perform(post("/api/user/login").contentType("application/json").content(loginJson(LEGACY_FIXED_CODE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("验证码错误"));
        assertNull(users.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, PHONE)));
        String wrongCode = "000000".equals(secondCode) ? "000001" : "000000";
        mvc.perform(post("/api/user/login").contentType("application/json").content(loginJson(wrongCode)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));

        String body = mvc.perform(post("/api/user/login").contentType("application/json").content(loginJson(secondCode)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        token = JsonPath.read(body, "$.data.token");
        assertTrue(token.matches("[0-9a-f]{32}"));
        Map<Object, Object> hash = redis.opsForHash().entries(RedisKeys.token(token));
        assertFalse(hash.isEmpty());
        assertTrue(hash.containsKey("id") && hash.containsKey("nickname") && hash.containsKey("avatarUrl") && hash.containsKey("phoneMasked"));
        for (Object value : hash.values()) assertTrue(value instanceof String);
        assertFalse(Boolean.TRUE.equals(redis.hasKey(codeKey)));
        Long tokenTtl = redis.getExpire(RedisKeys.token(token), TimeUnit.SECONDS);
        assertTrue(tokenTtl != null && tokenTtl > 0);

        mvc.perform(post("/api/user/login").contentType("application/json").content(loginJson(secondCode)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("验证码已过期或尚未获取"));
        mvc.perform(get("/api/user/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/user/me").header("Authorization", "Bearer invalid-token")).andExpect(status().isUnauthorized());

        redis.expire(RedisKeys.token(token), 1, TimeUnit.SECONDS);
        mvc.perform(get("/api/user/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").exists());
        assertTrue(redis.getExpire(RedisKeys.token(token), TimeUnit.SECONDS) > 60);
        assertNull(UserContext.getUser());

        User testUser = users.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, PHONE));
        assertNotNull(testUser);
        testUser.setNickname(TEST_MARKER + "USER");
        users.updateById(testUser);
        mvc.perform(put("/api/user/profile").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"nickname\":\"" + TEST_MARKER + "PROFILE\",\"avatarUrl\":\"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nickname").value(TEST_MARKER + "PROFILE"));
        mvc.perform(post("/api/user/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/user/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredVerificationCodeCannotLogIn() throws Exception {
        redis.opsForValue().set(RedisKeys.code(PHONE), "654321", 1, TimeUnit.MILLISECONDS);
        Thread.sleep(25L);
        assertNull(redis.opsForValue().get(RedisKeys.code(PHONE)));
        mvc.perform(post("/api/user/login").contentType("application/json").content(loginJson("654321")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("验证码已过期或尚未获取"));
    }

    @Test
    void publicBrowsingAndShopCache() throws Exception {
        mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/home/summary")).andExpect(jsonPath("$.data.categories").isArray());
        mvc.perform(get("/api/shops?page=1&size=5")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/goods?page=1&size=5")).andExpect(jsonPath("$.code").value(200));
        Shop shop = shops.selectOne(Wrappers.<Shop>lambdaQuery().eq(Shop::getStatus, 1).last("LIMIT 1"));
        assertNotNull(shop);
        redis.delete(RedisKeys.shopDetail(shop.getId()));
        shopService.detail(shop.getId());
        assertNotNull(redis.opsForValue().get(RedisKeys.shopDetail(shop.getId())));
        Long absent = Long.MAX_VALUE;
        redis.delete(RedisKeys.shopNull(absent));
        assertThrows(com.qinghe.life.exception.BusinessException.class, () -> shopService.detail(absent));
        assertTrue(Boolean.TRUE.equals(redis.hasKey(RedisKeys.shopNull(absent))));
        redis.delete(RedisKeys.shopDetail(shop.getId()));
        redis.delete(RedisKeys.shopNull(absent));
    }

    private void requestCode() throws Exception {
        mvc.perform(post("/api/user/code").contentType("application/json").content("{\"phone\":\"" + PHONE + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    private String currentCode() {
        String code = redis.opsForValue().get(RedisKeys.code(PHONE));
        assertNotNull(code);
        return code;
    }

    private String loginJson(String code) { return "{\"phone\":\"" + PHONE + "\",\"code\":\"" + code + "\"}"; }
}
