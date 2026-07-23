package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.Admin;
import com.qinghe.life.entity.Category;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.OperateLog;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.mapper.AdminMapper;
import com.qinghe.life.mapper.CategoryMapper;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.OperateLogMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.oss.AliyunOSSOperator;
import com.qinghe.life.utils.RedisKeys;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminGoodsIntegrationTest {
    private static final String MARKER = "ADMIN_GOODS_TEST_";
    private static final String ADMIN_TOKEN = MARKER + "ADMIN_TOKEN";
    private static final String USER_TOKEN = MARKER + "USER_TOKEN";

    @Autowired private MockMvc mvc;
    @Autowired private AdminMapper adminMapper;
    @Autowired private CategoryMapper categoryMapper;
    @Autowired private ShopMapper shopMapper;
    @Autowired private GoodsMapper goodsMapper;
    @Autowired private OperateLogMapper operateLogMapper;
    @Autowired private StringRedisTemplate redis;
    @MockBean private AliyunOSSOperator oss;

    private Admin admin;
    private Category category;
    private Shop shop;

    @BeforeEach
    void setUp() {
        cleanup();
        category = new Category();
        category.setName(MARKER + "分类");
        category.setStatus(1);
        category.setSortOrder(1);
        categoryMapper.insert(category);
        shop = new Shop();
        shop.setName(MARKER + "店铺");
        shop.setCategoryId(category.getId());
        shop.setAddress("校园商业街 A 区");
        shop.setScore(new java.math.BigDecimal("4.50"));
        shop.setStatus(1);
        shop.setIsFeatured(0);
        shop.setSortOrder(1);
        shopMapper.insert(shop);
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
    void tearDown() { cleanup(); }

    @Test
    void administratorMaintainsGoodsAndPublicDataReflectsLatestState() throws Exception {
        mvc.perform(get("/api/admin/goods")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        mvc.perform(get("/api/admin/goods").header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));

        mvc.perform(post("/api/admin/goods").header("Authorization", adminAuthorization())
                        .contentType("application/json").content(validBody(MARKER + "新品", "18.50", 5, "ON_SALE", shop.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value(MARKER + "新品"));
        Goods goods = goodsMapper.selectOne(Wrappers.<Goods>lambdaQuery().eq(Goods::getName, MARKER + "新品"));
        assertTrue(goods != null);

        mvc.perform(get("/api/admin/goods").param("keyword", MARKER).param("shopId", String.valueOf(shop.getId()))
                        .param("categoryId", String.valueOf(category.getId())).param("saleStatus", "ON_SALE")
                        .header("Authorization", adminAuthorization()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(put("/api/admin/goods/{id}", goods.getId()).header("Authorization", adminAuthorization())
                        .contentType("application/json").content(validBody(MARKER + "已编辑", "19.90", 5, "ON_SALE", shop.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value(MARKER + "已编辑"));
        mvc.perform(get("/api/goods/{id}", goods.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value(MARKER + "已编辑"))
                .andExpect(jsonPath("$.data.price").value(19.90));

        mvc.perform(put("/api/admin/goods/{id}/status", goods.getId()).header("Authorization", adminAuthorization())
                        .contentType("application/json").content("{\"saleStatus\":\"OFF_SALE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/goods/{id}", goods.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        mvc.perform(put("/api/admin/goods/{id}/status", goods.getId()).header("Authorization", adminAuthorization())
                        .contentType("application/json").content("{\"saleStatus\":\"ON_SALE\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/admin/goods/{id}/stock", goods.getId()).header("Authorization", adminAuthorization())
                        .contentType("application/json").content("{\"stock\":0}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/shops/{id}/goods", shop.getId()).param("page", "1").param("size", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.records[0].stock").value(0));
        mvc.perform(post("/api/cart").header("Authorization", "Bearer " + USER_TOKEN)
                        .contentType("application/json").content("{\"goodsId\":" + goods.getId() + ",\"quantity\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));

        mvc.perform(put("/api/admin/goods/{id}/stock", goods.getId()).header("Authorization", adminAuthorization())
                        .contentType("application/json").content("{\"stock\":5}"))
                .andExpect(status().isOk());
        shop.setStatus(0);
        shopMapper.updateById(shop);
        mvc.perform(get("/api/goods/{id}", goods.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        shop.setStatus(1);
        shopMapper.updateById(shop);

        mvc.perform(multipart("/api/admin/goods/{id}/image", goods.getId()).file(pngFile()).header("Authorization", adminAuthorization()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.coverImage").value(org.hamcrest.Matchers.containsString("qinghe-life-service/goods/")));
        goods = goodsMapper.selectById(goods.getId());
        assertTrue(goods.getCoverImage().contains("qinghe-life-service/goods/"));
        String oldKey = "qinghe-life-service/goods/2026/07/123e4567-e89b-12d3-a456-426614174000.webp";
        String oldUrl = "https://java-ai1-kevin.oss-cn-beijing.aliyuncs.com/" + oldKey;
        goods.setCoverImage(oldUrl);
        goodsMapper.updateById(goods);
        when(oss.ownGoodsImageKey(oldUrl)).thenReturn(oldKey);
        clearInvocations(oss);
        mvc.perform(multipart("/api/admin/goods/{id}/image", goods.getId()).file(pngFile()).header("Authorization", adminAuthorization()))
                .andExpect(status().isOk());
        verify(oss).deleteObject(oldKey);

        mvc.perform(post("/api/admin/goods").header("Authorization", adminAuthorization()).contentType("application/json")
                        .content(validBody("bad-shop", "1.00", 1, "ON_SALE", 999999L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/admin/goods").header("Authorization", adminAuthorization()).contentType("application/json")
                        .content(validBody("bad-price", "-0.01", 1, "ON_SALE", shop.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/admin/goods").header("Authorization", adminAuthorization()).contentType("application/json")
                        .content(validBody("bad-stock", "1.00", -1, "ON_SALE", shop.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(multipart("/api/admin/goods/{id}/image", goods.getId())
                        .file(new MockMultipartFile("file", "bad.txt", "text/plain", "not-an-image".getBytes())).header("Authorization", adminAuthorization()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(multipart("/api/admin/goods/{id}/image", goods.getId())
                        .file(new MockMultipartFile("file", "empty.png", "image/png", new byte[0])).header("Authorization", adminAuthorization()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(multipart("/api/admin/goods/{id}/image", goods.getId())
                        .file(new MockMultipartFile("file", "large.webp", "image/webp", new byte[3 * 1024 * 1024 + 1])).header("Authorization", adminAuthorization()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        when(oss.upload(anyString(), any(java.io.InputStream.class), anyLong(), anyString())).thenThrow(new IllegalStateException("mock failure"));
        mvc.perform(multipart("/api/admin/goods/{id}/image", goods.getId()).file(pngFile()).header("Authorization", adminAuthorization()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("商品主图上传服务暂不可用"));

        for (OperateLog item : operateLogMapper.selectList(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getUserId, admin.getId()))) {
            assertFalse(String.valueOf(item.getRequestSummary()).contains(ADMIN_TOKEN));
        }
    }

    private String validBody(String name, String price, int stock, String saleStatus, Long shopId) {
        return "{\"shopId\":" + shopId + ",\"name\":\"" + name + "\",\"description\":\"商品简介\",\"price\":" + price
                + ",\"stock\":" + stock + ",\"saleStatus\":\"" + saleStatus + "\"}";
    }

    private String adminAuthorization() { return "Bearer " + ADMIN_TOKEN; }
    private MockMultipartFile pngFile() { return new MockMultipartFile("file", "goods.png", "image/png", Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScL3vgAAAABJRU5ErkJggg==")); }

    private void cleanup() {
        if (operateLogMapper != null && admin != null) operateLogMapper.delete(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getUserId, admin.getId()));
        if (goodsMapper != null) goodsMapper.delete(Wrappers.<Goods>lambdaQuery().likeRight(Goods::getName, MARKER));
        if (shopMapper != null) shopMapper.delete(Wrappers.<Shop>lambdaQuery().likeRight(Shop::getName, MARKER));
        if (categoryMapper != null) categoryMapper.delete(Wrappers.<Category>lambdaQuery().likeRight(Category::getName, MARKER));
        if (adminMapper != null) adminMapper.delete(Wrappers.<Admin>lambdaQuery().likeRight(Admin::getUsername, MARKER));
        if (redis != null) { redis.delete(RedisKeys.adminToken(ADMIN_TOKEN)); redis.delete(RedisKeys.token(USER_TOKEN)); }
    }
}
