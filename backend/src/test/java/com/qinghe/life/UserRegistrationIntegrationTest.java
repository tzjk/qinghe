package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.User;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class UserRegistrationIntegrationTest {
    private static final String MARKER = "REGISTER_TEST_";
    private static final String NEW_PHONE = "13900009681";
    private static final String LEGACY_PHONE = "13900009682";
    private static final String BOUND_PHONE = "13900009683";
    private static final String USERNAME_OWNER_PHONE = "13900009684";
    private static final String CODE = "246810";
    private static final String PASSWORD = "Account2026";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
        UserContext.clear();
    }

    @Test
    void registerEndpointIsPublicAndValidatesRequest() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(registerJson("bad", NEW_PHONE, CODE, PASSWORD, PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(registerJson("REGISTER_TEST_OK", "123", CODE, PASSWORD, PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(registerJson("REGISTER_TEST_OK", NEW_PHONE, CODE, "password", "password")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(registerJson("REGISTER_TEST_OK", NEW_PHONE, CODE, PASSWORD, "Account2027")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("两次密码不一致"));
        assertNull(userByPhone(NEW_PHONE));
    }

    @Test
    void wrongAndMissingVerificationCodeDoNotCreateUserOrDeleteCode() throws Exception {
        putCode(NEW_PHONE, CODE);
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(registerJson("REGISTER_TEST_BAD", NEW_PHONE, "111111", PASSWORD, PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("验证码错误"));
        assertNull(userByPhone(NEW_PHONE));
        assertEquals(CODE, redis.opsForValue().get(RedisKeys.code(NEW_PHONE)));

        redis.delete(RedisKeys.code(NEW_PHONE));
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(registerJson("REGISTER_TEST_BAD", NEW_PHONE, CODE, PASSWORD, PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("验证码已过期或尚未获取"));
        assertNull(userByPhone(NEW_PHONE));
    }

    @Test
    void newPhoneRegistrationStoresOnlyBcryptHashAndDeletesCode() throws Exception {
        putCode(NEW_PHONE, CODE);
        String body = mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(registerJson("REGISTER_TEST_NEW", NEW_PHONE, CODE, PASSWORD, PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        User user = userByPhone(NEW_PHONE);
        assertNotNull(user);
        assertEquals("REGISTER_TEST_NEW", user.getUsername());
        assertNotNull(user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches(PASSWORD, user.getPasswordHash()));
        assertFalse(PASSWORD.equals(user.getPasswordHash()));
        assertFalse(body.contains("password"));
        assertFalse(body.contains("passwordHash"));
        assertFalse(body.contains("token"));
        assertNull(redis.opsForValue().get(RedisKeys.code(NEW_PHONE)));
    }

    @Test
    void legacyPhoneUserBindsInPlaceAndBoundPhoneCannotRegisterAgain() throws Exception {
        User legacy = createUser(LEGACY_PHONE, null, null, MARKER + "LEGACY");
        Long legacyId = legacy.getId();
        putCode(LEGACY_PHONE, CODE);
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(registerJson("REGISTER_TEST_OLD", LEGACY_PHONE, CODE, PASSWORD, PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        User boundLegacy = userByPhone(LEGACY_PHONE);
        assertEquals(legacyId, boundLegacy.getId());
        assertEquals("REGISTER_TEST_OLD", boundLegacy.getUsername());
        assertTrue(passwordEncoder.matches(PASSWORD, boundLegacy.getPasswordHash()));

        createUser(BOUND_PHONE, "REGISTER_TEST_BOUND", passwordEncoder.encode(PASSWORD), MARKER + "BOUND");
        putCode(BOUND_PHONE, CODE);
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(registerJson("REGISTER_TEST_AGAIN", BOUND_PHONE, CODE, PASSWORD, PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("该手机号已注册"));
        assertEquals(CODE, redis.opsForValue().get(RedisKeys.code(BOUND_PHONE)));
    }

    @Test
    void duplicateUsernameIsRejectedWithoutCreatingUser() throws Exception {
        createUser(USERNAME_OWNER_PHONE, "REGISTER_TEST_DUP", passwordEncoder.encode(PASSWORD), MARKER + "OWNER");
        putCode(NEW_PHONE, CODE);
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(registerJson("REGISTER_TEST_DUP", NEW_PHONE, CODE, PASSWORD, PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
        assertNull(userByPhone(NEW_PHONE));
        assertEquals(CODE, redis.opsForValue().get(RedisKeys.code(NEW_PHONE)));
    }

    private void putCode(String phone, String code) {
        redis.opsForValue().set(RedisKeys.code(phone), code, RedisKeys.LOGIN_CODE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    private User createUser(String phone, String username, String passwordHash, String nickname) {
        User user = new User();
        user.setPhone(phone);
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setNickname(nickname);
        user.setGender(0);
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    private String registerJson(String username, String phone, String code, String password, String confirmPassword) {
        return "{\"username\":\"" + username + "\",\"phone\":\"" + phone + "\",\"code\":\"" + code
                + "\",\"password\":\"" + password + "\",\"confirmPassword\":\"" + confirmPassword + "\"}";
    }

    private User userByPhone(String phone) {
        return userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone));
    }

    private void cleanup() {
        List<String> phones = Arrays.asList(NEW_PHONE, LEGACY_PHONE, BOUND_PHONE, USERNAME_OWNER_PHONE);
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery().in(User::getPhone, phones));
        for (User user : users) {
            if ((user.getNickname() != null && user.getNickname().startsWith(MARKER))
                    || (user.getUsername() != null && user.getUsername().startsWith(MARKER))) {
                userMapper.deleteById(user.getId());
            }
        }
        for (String phone : phones) {
            redis.delete(RedisKeys.code(phone));
        }
    }
}
