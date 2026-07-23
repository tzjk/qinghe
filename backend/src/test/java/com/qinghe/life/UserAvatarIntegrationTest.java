package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.OperateLog;
import com.qinghe.life.entity.User;
import com.qinghe.life.mapper.OperateLogMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.oss.AliyunOSSOperator;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.vo.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserAvatarIntegrationTest {
    private static final String PHONE_A = "13900001021", PHONE_B = "13900001022";
    @Autowired private MockMvc mvc;
    @Autowired private UserMapper users;
    @Autowired private OperateLogMapper logs;
    @Autowired private StringRedisTemplate redis;
    @MockBean private AliyunOSSOperator oss;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        cleanup();
        tokenA = signIn(seed(PHONE_A, "当前用户", "https://external.example/default.png"));
        tokenB = signIn(seed(PHONE_B, "另一用户", "https://external.example/other.png"));
    }

    @AfterEach
    void tearDown() { cleanup(); }

    @Test
    void updatesOnlyCurrentUserAndSynchronizesRedisWithoutExposingPasswordHash() throws Exception {
        when(oss.upload(anyString(), any(InputStream.class), anyLong(), anyString())).thenAnswer(invocation -> "https://java-ai1-kevin.oss-cn-beijing.aliyuncs.com/" + invocation.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "avatar.webp", "image/webp", webp());

        mvc.perform(multipart("/api/user/avatar").file(file).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.avatarUrl").value(org.hamcrest.Matchers.containsString("qinghe-life-service/")))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        User current = users.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, PHONE_A));
        User other = users.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, PHONE_B));
        assertTrue(current.getAvatarUrl().matches("https://java-ai1-kevin\\.oss-cn-beijing\\.aliyuncs\\.com/qinghe-life-service/\\d{4}/\\d{2}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.webp"));
        assertEquals("https://external.example/other.png", other.getAvatarUrl());
        assertEquals(current.getAvatarUrl(), redis.opsForHash().entries(RedisKeys.token(tokenA)).get("avatarUrl"));
        assertFalse(logs.selectList(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getUserId, current.getId())).isEmpty());
        verify(oss, never()).deleteObject(anyString());
    }

    @Test
    void rejectsInvalidFilesAndReturnsFriendlyOssFailure() throws Exception {
        mvc.perform(multipart("/api/user/avatar").file(new MockMultipartFile("file", "bad.txt", "text/plain", "bad".getBytes())).header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(multipart("/api/user/avatar").file(new MockMultipartFile("file", "empty.webp", "image/webp", new byte[0])).header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(multipart("/api/user/avatar").file(new MockMultipartFile("file", "large.webp", "image/webp", new byte[2 * 1024 * 1024 + 1])).header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.code").value(400));
        when(oss.upload(anyString(), any(InputStream.class), anyLong(), anyString())).thenThrow(new IllegalStateException("simulated"));
        mvc.perform(multipart("/api/user/avatar").file(new MockMultipartFile("file", "avatar.webp", "image/webp", webp())).header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.message").value("头像上传服务暂不可用"));
    }

    private User seed(String phone, String nickname, String avatar) { User user = new User(); user.setPhone(phone); user.setUsername("avatar_" + phone.substring(phone.length() - 4)); user.setNickname(nickname); user.setAvatarUrl(avatar); user.setProfileCompleted(1); user.setStatus(1); users.insert(user); return user; }
    private String signIn(User user) { String token = UUID.randomUUID().toString().replace("-", ""); redis.opsForHash().putAll(RedisKeys.token(token), UserDTO.fromUser(user).toMap()); return token; }
    private void cleanup() { for (String phone : Arrays.asList(PHONE_A, PHONE_B)) { User user = users.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone)); if (user != null) { logs.delete(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getUserId, user.getId())); users.deleteById(user.getId()); } } if (tokenA != null) redis.delete(RedisKeys.token(tokenA)); if (tokenB != null) redis.delete(RedisKeys.token(tokenB)); }
    private byte[] webp() { return new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}; }
}
