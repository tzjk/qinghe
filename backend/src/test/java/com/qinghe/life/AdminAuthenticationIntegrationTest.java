package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.Admin;
import com.qinghe.life.mapper.AdminMapper;
import com.qinghe.life.utils.AdminContext;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAuthenticationIntegrationTest {
    private static final String MARKER = "ADMIN_AUTH_TEST_";
    private static final String PASSWORD = "Password123";

    @Autowired private MockMvc mvc;
    @Autowired private StringRedisTemplate redis;
    @Autowired private AdminMapper adminMapper;

    private Admin admin;

    @BeforeEach
    void setUp() {
        cleanup();
        admin = new Admin();
        admin.setUsername(MARKER + "ADMIN");
        admin.setDisplayName(MARKER + "Admin");
        admin.setPasswordHash(new BCryptPasswordEncoder().encode(PASSWORD));
        admin.setStatus(1);
        adminMapper.insert(admin);
    }

    @AfterEach
    void tearDown() {
        cleanup();
        UserContext.clear();
        AdminContext.clear();
    }

    @Test
    void bcryptPasswordLoginCreatesIndependentRandomRedisTokensAndExposesCurrentAdmin() throws Exception {
        mvc.perform(post("/api/admin/auth/login").contentType("application/json")
                        .content("{\"username\":\"" + admin.getUsername() + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));

        String firstToken = login();
        String secondToken = login();
        assertNotEquals(firstToken, secondToken);
        assertTrue(firstToken.matches("[0-9a-f]{32}"));
        assertTrue(Boolean.TRUE.equals(redis.hasKey(RedisKeys.adminToken(firstToken))));
        assertTrue(redis.getExpire(RedisKeys.adminToken(firstToken), TimeUnit.SECONDS) > 0);

        mvc.perform(get("/api/admin/auth/me").header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(admin.getId()))
                .andExpect(jsonPath("$.data.username").value(admin.getUsername()));
        assertNull(AdminContext.getAdmin());
        assertNull(AdminContext.getToken());
        assertNull(UserContext.getUser());
    }

    @Test
    void regularUserTokenAndExpiredAdminTokenCannotAccessAdminApiAndContextsAreCleared() throws Exception {
        String userToken = MARKER + "USER";
        Map<String, String> userSession = new HashMap<String, String>();
        userSession.put("id", "9988");
        userSession.put("username", "ordinary-user");
        redis.opsForHash().putAll(RedisKeys.token(userToken), userSession);
        mvc.perform(get("/api/admin/auth/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        assertNull(UserContext.getUser());
        assertNull(AdminContext.getAdmin());

        String token = login();
        redis.expire(RedisKeys.adminToken(token), 0, TimeUnit.SECONDS);
        assertFalse(Boolean.TRUE.equals(redis.hasKey(RedisKeys.adminToken(token))));
        mvc.perform(get("/api/admin/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        assertNull(UserContext.getUser());
        assertNull(AdminContext.getAdmin());
    }

    @Test
    void logoutDeletesAdminTokenAndSubsequentRequestIsUnauthorized() throws Exception {
        String token = login();
        mvc.perform(post("/api/admin/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(RedisKeys.adminToken(token))));
        mvc.perform(get("/api/admin/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        assertNull(AdminContext.getAdmin());
        assertNull(AdminContext.getToken());
    }

    private String login() throws Exception {
        String body = mvc.perform(post("/api/admin/auth/login").contentType("application/json")
                        .content("{\"username\":\"" + admin.getUsername() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll("(?s).*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private void cleanup() {
        if (adminMapper != null) {
            for (Admin item : adminMapper.selectList(Wrappers.<Admin>lambdaQuery().likeRight(Admin::getUsername, MARKER))) {
                adminMapper.deleteById(item);
            }
        }
        if (redis != null) {
            redis.delete(RedisKeys.token(MARKER + "USER"));
            for (String key : redis.keys(RedisKeys.namespace() + "admin:token:*")) {
                Map<Object, Object> session = redis.opsForHash().entries(key);
                if (MARKER.concat("ADMIN").equals(String.valueOf(session.get("username")))) redis.delete(key);
            }
        }
    }
}
