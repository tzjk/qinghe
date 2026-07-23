package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.Admin;
import com.qinghe.life.entity.Category;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.GoodsCategory;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.mapper.AdminMapper;
import com.qinghe.life.mapper.CategoryMapper;
import com.qinghe.life.mapper.GoodsCategoryMapper;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.utils.RedisKeys;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminGoodsCategoryIntegrationTest {
    private static final String MARKER = "CAT_TEST_";
    private static final String ADMIN_TOKEN = MARKER + "ADMIN";
    private static final String USER_TOKEN = MARKER + "USER";

    @Autowired private MockMvc mvc;
    @Autowired private AdminMapper adminMapper;
    @Autowired private CategoryMapper categoryMapper;
    @Autowired private ShopMapper shopMapper;
    @Autowired private GoodsMapper goodsMapper;
    @Autowired private GoodsCategoryMapper goodsCategoryMapper;
    @Autowired private StringRedisTemplate redis;
    private Admin admin;
    private Shop firstShop;
    private Shop secondShop;
    private GoodsCategory firstCategory;
    private GoodsCategory secondCategory;

    @BeforeEach
    void setUp() {
        cleanup();
        Category shopType = new Category();
        shopType.setName(MARKER + "TYPE"); shopType.setStatus(1); shopType.setSortOrder(1); categoryMapper.insert(shopType);
        firstShop = shop(shopType.getId(), "SHOP_A");
        secondShop = shop(shopType.getId(), "SHOP_B");
        admin = new Admin(); admin.setUsername(MARKER + "ADMIN"); admin.setDisplayName(MARKER + "ADMIN"); admin.setPasswordHash("unused"); admin.setStatus(1); adminMapper.insert(admin);
        Map<String, String> adminSession = new HashMap<String, String>();
        adminSession.put("adminId", String.valueOf(admin.getId())); adminSession.put("username", admin.getUsername()); adminSession.put("displayName", admin.getDisplayName());
        redis.opsForHash().putAll(RedisKeys.adminToken(ADMIN_TOKEN), adminSession);
        Map<String, String> userSession = new HashMap<String, String>(); userSession.put("id", "9911"); redis.opsForHash().putAll(RedisKeys.token(USER_TOKEN), userSession);
    }

    @AfterEach
    void tearDown() { cleanup(); }

    @Test
    void categoriesAreAdminOnlyUniquePerShopAndExposeCounts() throws Exception {
        mvc.perform(get("/api/admin/goods/categories").param("shopId", String.valueOf(firstShop.getId())))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/goods/categories").param("shopId", String.valueOf(firstShop.getId())).header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isUnauthorized());
        firstCategory = createCategory(firstShop.getId(), "DRINK", 2);
        mvc.perform(post("/api/admin/goods/categories").header("Authorization", adminAuthorization()).contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody(firstShop.getId(), "DRINK", 3)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        secondCategory = createCategory(secondShop.getId(), "DRINK", 1);
        createGoods(firstShop.getId(), firstCategory.getId(), "COUNTED");
        mvc.perform(get("/api/admin/goods/categories").header("Authorization", adminAuthorization()).param("shopId", String.valueOf(firstShop.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].name").value(MARKER + "DRINK"))
                .andExpect(jsonPath("$.data[0].goodsCount").value(1));
        mvc.perform(put("/api/admin/goods/categories/{id}", firstCategory.getId()).header("Authorization", adminAuthorization()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + MARKER + "BEVERAGE\",\"sortOrder\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.sortOrder").value(0));
    }

    @Test
    void goodsMustUseItsOwnShopCategoryAndDisabledCategoryIsHistoricalOnly() throws Exception {
        firstCategory = createCategory(firstShop.getId(), "FOOD", 1);
        secondCategory = createCategory(secondShop.getId(), "FOOD", 1);
        mvc.perform(post("/api/admin/goods").header("Authorization", adminAuthorization()).contentType(MediaType.APPLICATION_JSON)
                        .content(goodsBody(firstShop.getId(), secondCategory.getId(), "WRONG")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        Goods goods = createGoods(firstShop.getId(), firstCategory.getId(), "HISTORY");
        mvc.perform(put("/api/admin/goods/categories/{id}/status", firstCategory.getId()).header("Authorization", adminAuthorization()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"0\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/admin/goods").header("Authorization", adminAuthorization()).contentType(MediaType.APPLICATION_JSON)
                        .content(goodsBody(firstShop.getId(), firstCategory.getId(), "NEW_DISABLED")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(put("/api/admin/goods/{id}", goods.getId()).header("Authorization", adminAuthorization()).contentType(MediaType.APPLICATION_JSON)
                        .content(goodsBody(firstShop.getId(), firstCategory.getId(), "HISTORY_EDIT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.categoryId").value(firstCategory.getId()));
        mvc.perform(put("/api/admin/goods/{id}", goods.getId()).header("Authorization", adminAuthorization()).contentType(MediaType.APPLICATION_JSON)
                        .content(goodsBody(secondShop.getId(), firstCategory.getId(), "CROSS_SHOP")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(put("/api/admin/goods/{id}", goods.getId()).header("Authorization", adminAuthorization()).contentType(MediaType.APPLICATION_JSON)
                        .content(goodsBody(secondShop.getId(), null, "UNCATEGORIZED")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.categoryId").doesNotExist());
        assertNull(goodsMapper.selectById(goods.getId()).getCategoryId());
    }

    @Test
    void publicShopOnlyReturnsEnabledCategoriesAndSaleableGoods() throws Exception {
        firstCategory = createCategory(firstShop.getId(), "VISIBLE", 1);
        secondCategory = createCategory(firstShop.getId(), "HIDDEN", 2);
        createGoods(firstShop.getId(), firstCategory.getId(), "VISIBLE_GOODS");
        Goods offSale = createGoods(firstShop.getId(), secondCategory.getId(), "OFF_SALE"); offSale.setSaleStatus("OFF_SALE"); goodsMapper.updateById(offSale);
        mvc.perform(put("/api/admin/goods/categories/{id}/status", secondCategory.getId()).header("Authorization", adminAuthorization()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"0\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/shops/{id}/goods-categories", firstShop.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(firstCategory.getId()));
        mvc.perform(get("/api/shops/{id}/goods", firstShop.getId()).param("page", "1").param("size", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].categoryId").value(firstCategory.getId()));
    }

    private Shop shop(Long typeId, String suffix) { Shop shop = new Shop(); shop.setName(MARKER + suffix); shop.setCategoryId(typeId); shop.setAddress("Campus"); shop.setScore(new BigDecimal("4.50")); shop.setStatus(1); shop.setIsFeatured(0); shop.setSortOrder(1); shopMapper.insert(shop); return shop; }
    private GoodsCategory createCategory(Long shopId, String name, int sortOrder) throws Exception { mvc.perform(post("/api/admin/goods/categories").header("Authorization", adminAuthorization()).contentType(MediaType.APPLICATION_JSON).content(categoryBody(shopId, name, sortOrder))).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)); return goodsCategoryMapper.selectOne(Wrappers.<GoodsCategory>lambdaQuery().eq(GoodsCategory::getShopId, shopId).eq(GoodsCategory::getName, MARKER + name)); }
    private Goods createGoods(Long shopId, Long categoryId, String name) throws Exception { mvc.perform(post("/api/admin/goods").header("Authorization", adminAuthorization()).contentType(MediaType.APPLICATION_JSON).content(goodsBody(shopId, categoryId, name))).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)); return goodsMapper.selectOne(Wrappers.<Goods>lambdaQuery().eq(Goods::getName, MARKER + name)); }
    private String categoryBody(Long shopId, String name, int sortOrder) { return "{\"shopId\":" + shopId + ",\"name\":\"" + MARKER + name + "\",\"sortOrder\":" + sortOrder + "}"; }
    private String goodsBody(Long shopId, Long categoryId, String name) { return "{\"shopId\":" + shopId + (categoryId == null ? "" : ",\"categoryId\":" + categoryId) + ",\"name\":\"" + MARKER + name + "\",\"description\":\"test\",\"price\":12.50,\"stock\":5,\"saleStatus\":\"ON_SALE\"}"; }
    private String adminAuthorization() { return "Bearer " + ADMIN_TOKEN; }
    private void cleanup() { if (goodsMapper != null) goodsMapper.delete(Wrappers.<Goods>lambdaQuery().likeRight(Goods::getName, MARKER)); if (goodsCategoryMapper != null) goodsCategoryMapper.delete(Wrappers.<GoodsCategory>lambdaQuery().likeRight(GoodsCategory::getName, MARKER)); if (shopMapper != null) shopMapper.delete(Wrappers.<Shop>lambdaQuery().likeRight(Shop::getName, MARKER)); if (categoryMapper != null) categoryMapper.delete(Wrappers.<Category>lambdaQuery().likeRight(Category::getName, MARKER)); if (adminMapper != null) adminMapper.delete(Wrappers.<Admin>lambdaQuery().likeRight(Admin::getUsername, MARKER)); if (redis != null) { redis.delete(RedisKeys.adminToken(ADMIN_TOKEN)); redis.delete(RedisKeys.token(USER_TOKEN)); } }
}
