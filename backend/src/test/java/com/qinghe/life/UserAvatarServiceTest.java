package com.qinghe.life;

import com.qinghe.life.entity.User;
import com.qinghe.life.entity.StudentProfile;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.mapper.StudentProfileMapper;
import com.qinghe.life.oss.AliyunOSSOperator;
import com.qinghe.life.service.AddressService;
import com.qinghe.life.service.impl.UserServiceImpl;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserAvatarServiceTest {
    private static final String TOKEN = "avatar-service-token";
    @Mock private UserMapper users;
    @Mock private StudentProfileMapper studentProfiles;
    @Mock private StringRedisTemplate redis;
    @Mock private HashOperations<String, Object, Object> hashes;
    @Mock private Environment environment;
    @Mock private AddressService addresses;
    @Mock private AliyunOSSOperator oss;
    private User user;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        user = user("https://java-ai1-kevin.oss-cn-beijing.aliyuncs.com/avatars/101/2026/07/old.webp");
        service = new UserServiceImpl(users, studentProfiles, redis, environment, addresses, oss);
        when(users.selectById(101L)).thenReturn(user);
        when(users.updateById(any(User.class))).thenReturn(1);
        when(redis.opsForHash()).thenReturn(hashes);
        UserContext.setUser(UserDTO.fromUser(user));
        UserContext.setToken(TOKEN);
    }

    @AfterEach
    void tearDown() { UserContext.clear(); }

    @Test
    void uploadsOwnAvatarSyncsRedisAndBestEffortDeletesOwnedOldObject() {
        when(oss.upload(anyString(), any(InputStream.class), anyLong(), eq("image/webp")))
                .thenAnswer(invocation -> "https://java-ai1-kevin.oss-cn-beijing.aliyuncs.com/" + invocation.getArgument(0));
        when(oss.ownAvatarKey(eq(101L), eq(user.getAvatarUrl()))).thenReturn("avatars/101/2026/07/old.webp");

        UserDTO result = service.uploadAvatar(webp());

        assertTrue(result.getAvatarUrl().matches("https://java-ai1-kevin\\.oss-cn-beijing\\.aliyuncs\\.com/qinghe-life-service/\\d{4}/\\d{2}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.webp"));
        verify(users).updateById(user);
        verify(hashes).putAll(eq(RedisKeys.token(TOKEN)), any(Map.class));
        verify(oss).deleteObject("avatars/101/2026/07/old.webp");
    }

    @Test
    void removesNewObjectWhenDatabaseUpdateFails() {
        when(oss.upload(anyString(), any(InputStream.class), anyLong(), eq("image/webp"))).thenReturn("https://example/avatar.webp");
        when(users.updateById(any(User.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.uploadAvatar(webp()));

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(oss).deleteObject(key.capture());
        assertTrue(key.getValue().matches("qinghe-life-service/\\d{4}/\\d{2}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.webp"));
    }

    @Test
    void oldObjectDeleteFailureDoesNotUndoSuccessfulAvatarSave() {
        when(oss.upload(anyString(), any(InputStream.class), anyLong(), eq("image/webp"))).thenReturn("https://example/avatar.webp");
        when(oss.ownAvatarKey(eq(101L), eq(user.getAvatarUrl()))).thenReturn("avatars/101/2026/07/old.webp");
        doThrow(new IllegalStateException("simulated")).when(oss).deleteObject("avatars/101/2026/07/old.webp");

        assertEquals("https://example/avatar.webp", service.uploadAvatar(webp()).getAvatarUrl());
        verify(users).updateById(user);
    }

    @Test
    void rejectsEmptyOversizedAndForgedFilesWithoutCallingOss() {
        assertThrows(BusinessException.class, () -> service.uploadAvatar(new MockMultipartFile("file", "avatar.webp", "image/webp", new byte[0])));
        assertThrows(BusinessException.class, () -> service.uploadAvatar(new MockMultipartFile("file", "avatar.webp", "image/webp", new byte[2 * 1024 * 1024 + 1])));
        assertThrows(BusinessException.class, () -> service.uploadAvatar(new MockMultipartFile("file", "avatar.jpg", "image/jpeg", "not-an-image".getBytes())));
        verify(oss, never()).upload(anyString(), any(InputStream.class), anyLong(), anyString());
    }

    @Test
    void convertsOssFailureToFriendlyBusinessMessage() {
        when(oss.upload(anyString(), any(InputStream.class), anyLong(), eq("image/webp"))).thenThrow(new IllegalStateException("simulated"));
        BusinessException error = assertThrows(BusinessException.class, () -> service.uploadAvatar(webp()));
        assertEquals("头像上传服务暂不可用", error.getMessage());
    }

    @Test
    void currentUserUsesCurrentStudentProfileWithoutLeakingInternalFields() {
        StudentProfile profile = new StudentProfile();
        profile.setUserId(101L); profile.setRealName("张三"); profile.setStudentNo("20260001"); profile.setCurrentFlag(1);
        when(studentProfiles.selectOne(any())).thenReturn(profile);

        UserDTO first = service.currentUser();
        profile.setRealName("李四");
        UserDTO refreshed = service.currentUser();

        assertTrue(first.getHasStudentProfile());
        assertEquals("张三", first.getRealName());
        assertEquals("20260001", first.getStudentNo());
        assertEquals("李四", refreshed.getRealName());
        assertEquals("头像测试", refreshed.getNickname());
        assertFalse(refreshed.toMap().containsKey("currentFlag"));
        assertFalse(refreshed.toMap().containsKey("passwordHash"));
        assertFalse(refreshed.toMap().containsKey("token"));
    }

    private User user(String avatarUrl) { User item = new User(); item.setId(101L); item.setPhone("13900001011"); item.setUsername("avatar_user"); item.setNickname("头像测试"); item.setAvatarUrl(avatarUrl); item.setProfileCompleted(1); item.setStatus(1); return item; }
    private MockMultipartFile webp() { byte[] bytes = new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}; return new MockMultipartFile("file", "avatar.webp", "image/webp", bytes); }
}
