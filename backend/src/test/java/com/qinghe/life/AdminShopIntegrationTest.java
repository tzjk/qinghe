package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.Admin;
import com.qinghe.life.entity.Category;
import com.qinghe.life.entity.OperateLog;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.mapper.AdminMapper;
import com.qinghe.life.mapper.CategoryMapper;
import com.qinghe.life.mapper.OperateLogMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.oss.AliyunOSSOperator;
import com.qinghe.life.utils.RedisKeys;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminShopIntegrationTest {
    private static final String MARKER = "ADMIN_SHOP_TEST_";
    private static final String ADMIN_TOKEN = MARKER + "ADMIN_TOKEN";
    private static final String USER_TOKEN = MARKER + "USER_TOKEN";
    private static final String PHONE = "13800009999";

    @Autowired private MockMvc mvc;
    @Autowired private AdminMapper adminMapper;
    @Autowired private CategoryMapper categoryMapper;
    @Autowired private ShopMapper shopMapper;
    @Autowired private OperateLogMapper operateLogMapper;
    @Autowired private StringRedisTemplate redis;
    @MockBean private AliyunOSSOperator oss;

    private Admin admin;
    private Category category;

    @BeforeEach
    void setUp() {
        cleanup();
        category = new Category();
        category.setName(MARKER + "分类");
        category.setStatus(1);
        category.setSortOrder(1);
        categoryMapper.insert(category);
        admin = new Admin();
        admin.setUsername(MARKER + "ADMIN");
        admin.setDisplayName(MARKER + "管理员");
        admin.setPasswordHash("not-used-by-this-test");
        admin.setStatus(1);
        adminMapper.insert(admin);
        Map<String, String> adminSession = new HashMap<String, String>();
        adminSession.put("adminId", String.valueOf(admin.getId()));
        adminSession.put("username", admin.getUsername());
        adminSession.put("displayName", admin.getDisplayName());
        redis.opsForHash().putAll(RedisKeys.adminToken(ADMIN_TOKEN), adminSession);
        Map<String, String> userSession = new HashMap<String, String>();
        userSession.put("id", "9988");
        userSession.put("username", "ordinary-user");
        redis.opsForHash().putAll(RedisKeys.token(USER_TOKEN), userSession);
        when(oss.upload(anyString(), any(java.io.InputStream.class), anyLong(), anyString()))
                .thenAnswer(invocation -> "https://java-ai1-kevin.oss-cn-beijing.aliyuncs.com/" + invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    @Test
    void administratorMaintainsShopCoverAndPublicReadReflectsChanges() throws Exception {
        mvc.perform(get("/api/admin/shops"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        mvc.perform(get("/api/admin/shops").header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));

        mvc.perform(post("/api/admin/shops").header("Authorization", adminAuthorization())
                        .contentType("application/json").content(validBody(MARKER + "新店", 1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value(MARKER + "新店"));
        Shop shop = shopMapper.selectOne(Wrappers.<Shop>lambdaQuery().eq(Shop::getName, MARKER + "新店"));
        assertTrue(shop != null);

        mvc.perform(get("/api/admin/shops").param("keyword", MARKER).param("categoryId", String.valueOf(category.getId()))
                        .header("Authorization", adminAuthorization()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(put("/api/admin/shops/{id}", shop.getId()).header("Authorization", adminAuthorization())
                        .contentType("application/json").content(validBody(MARKER + "已编辑", 1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value(MARKER + "已编辑"));

        redis.opsForValue().set(RedisKeys.shopDetail(shop.getId()), "stale-detail");
        redis.opsForValue().set(RedisKeys.shopNull(shop.getId()), "stale-null");
        redis.opsForValue().set("qh:admin-shop-test:unrelated", "keep");
        mvc.perform(put("/api/admin/shops/{id}/status", shop.getId()).header("Authorization", adminAuthorization())
                        .contentType("application/json").content("{\"status\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(RedisKeys.shopDetail(shop.getId()))));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(RedisKeys.shopNull(shop.getId()))));
        assertTrue(Boolean.TRUE.equals(redis.hasKey("qh:admin-shop-test:unrelated")));
        mvc.perform(get("/api/shops/{id}", shop.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        mvc.perform(put("/api/admin/shops/{id}/status", shop.getId()).header("Authorization", adminAuthorization())
                        .contentType("application/json").content("{\"status\":1}"))
                .andExpect(status().isOk());

        MockMultipartFile cover = new MockMultipartFile("file", "cover.png", "image/png", png());
        mvc.perform(multipart("/api/admin/shops/{id}/cover", shop.getId()).file(cover).header("Authorization", adminAuthorization()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.coverImage").value(org.hamcrest.Matchers.containsString("qinghe-life-service/shops/")));
        shop = shopMapper.selectById(shop.getId());
        assertTrue(shop.getCoverImage().contains("qinghe-life-service/shops/"));
        mvc.perform(get("/api/shops/{id}", shop.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.coverImage").value(shop.getCoverImage()));
        verify(oss, never()).deleteObject(anyString());

        String oldKey = "qinghe-life-service/shops/2026/07/123e4567-e89b-12d3-a456-426614174000.webp";
        String oldUrl = "https://java-ai1-kevin.oss-cn-beijing.aliyuncs.com/" + oldKey;
        shop.setCoverImage(oldUrl);
        shopMapper.updateById(shop);
        when(oss.ownShopCoverKey(oldUrl)).thenReturn(oldKey);
        clearInvocations(oss);
        mvc.perform(multipart("/api/admin/shops/{id}/cover", shop.getId())
                        .file(new MockMultipartFile("file", "cover.png", "image/png", png())).header("Authorization", adminAuthorization()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(oss).deleteObject(oldKey);

        mvc.perform(post("/api/admin/shops").header("Authorization", adminAuthorization())
                        .contentType("application/json").content("{\"name\":\"\",\"categoryId\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(delete("/api/admin/shops/{id}", shop.getId()).header("Authorization", adminAuthorization()))
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(multipart("/api/admin/shops/{id}/cover", shop.getId())
                        .file(new MockMultipartFile("file", "bad.txt", "text/plain", "not-an-image".getBytes())).header("Authorization", adminAuthorization()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(multipart("/api/admin/shops/{id}/cover", shop.getId())
                        .file(new MockMultipartFile("file", "empty.png", "image/png", new byte[0])).header("Authorization", adminAuthorization()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(multipart("/api/admin/shops/{id}/cover", shop.getId())
                        .file(new MockMultipartFile("file", "large.webp", "image/webp", new byte[3 * 1024 * 1024 + 1])).header("Authorization", adminAuthorization()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        when(oss.upload(anyString(), any(java.io.InputStream.class), anyLong(), anyString())).thenThrow(new IllegalStateException("mock failure"));
        mvc.perform(multipart("/api/admin/shops/{id}/cover", shop.getId())
                        .file(new MockMultipartFile("file", "cover.png", "image/png", png())).header("Authorization", adminAuthorization()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("店铺封面上传服务暂不可用"));

        for (OperateLog item : operateLogMapper.selectList(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getUserId, admin.getId()))) {
            assertFalse(String.valueOf(item.getRequestSummary()).contains(ADMIN_TOKEN));
            assertFalse(String.valueOf(item.getRequestSummary()).contains(PHONE));
        }
    }

    private String validBody(String name, int status) {
        return "{\"name\":\"" + name + "\",\"categoryId\":" + category.getId()
                + ",\"address\":\"校园商业街A座\",\"phone\":\"" + PHONE
                + "\",\"score\":4.50,\"status\":" + status + ",\"isFeatured\":0,\"sortOrder\":10}";
    }

    private String adminAuthorization() { return "Bearer " + ADMIN_TOKEN; }

    private byte[] png() {
        return Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScL3vgAAAABJRU5ErkJggg==");
    }

    private void cleanup() {
        if (operateLogMapper != null && admin != null) {
            operateLogMapper.delete(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getUserId, admin.getId()));
        }
        if (shopMapper != null) {
            for (Shop shop : shopMapper.selectList(Wrappers.<Shop>lambdaQuery().likeRight(Shop::getName, MARKER))) {
                redis.delete(RedisKeys.shopDetail(shop.getId()));
                redis.delete(RedisKeys.shopNull(shop.getId()));
                shopMapper.deleteById(shop.getId());
            }
        }
        if (categoryMapper != null) {
            categoryMapper.delete(Wrappers.<Category>lambdaQuery().likeRight(Category::getName, MARKER));
        }
        if (adminMapper != null) {
            adminMapper.delete(Wrappers.<Admin>lambdaQuery().likeRight(Admin::getUsername, MARKER));
        }
        if (redis != null) {
            redis.delete(RedisKeys.adminToken(ADMIN_TOKEN));
            redis.delete(RedisKeys.token(USER_TOKEN));
            redis.delete("qh:admin-shop-test:unrelated");
        }
    }
}
