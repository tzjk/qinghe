package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jayway.jsonpath.JsonPath;
import com.qinghe.life.entity.Cart;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.OperateLog;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.entity.User;
import com.qinghe.life.mapper.CartMapper;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.OperateLogMapper;
import com.qinghe.life.mapper.ShopMapper;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CartIntegrationTest {
    private static final String MARKER = "M3A_CART_TEST_";
    private static final String OWNER_PHONE = "13900009771";
    private static final String OTHER_PHONE = "13900009772";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OperateLogMapper operateLogMapper;

    private String ownerToken;
    private String otherToken;
    private Long ownerId;
    private Long otherId;
    private Long shopAId;
    private Long shopBId;
    private Long normalGoodsId;
    private Long secondGoodsId;

    @BeforeEach
    void removeResidualData() {
        cleanupTestData();
        UserContext.clear();
    }

    @AfterEach
    void cleanup() {
        cleanupTestData();
        clearRedisTestKeys();
        UserContext.clear();
    }

    @Test
    void cartBusinessRulesIsolationSummaryAndCleanupFlow() throws Exception {
        mvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
        assertNull(UserContext.getUser());

        createTestCatalog();
        ownerToken = login(OWNER_PHONE, "OWNER");
        otherToken = login(OTHER_PHONE, "OTHER");
        ownerId = userIdByPhone(OWNER_PHONE);
        otherId = userIdByPhone(OTHER_PHONE);
        assertNotNull(ownerId);
        assertNotNull(otherId);
        String ownerHeader = bearer(ownerToken);
        String otherHeader = bearer(otherToken);

        Long ownerCartId = addCart(ownerHeader, normalGoodsId, 2);
        assertEquals(2, cartQuantity(ownerCartId));
        Long duplicateCartId = addCart(ownerHeader, normalGoodsId, 2);
        assertEquals(ownerCartId, duplicateCartId);
        assertEquals(4, cartQuantity(ownerCartId));
        assertEquals(1, cartCount(ownerId, normalGoodsId));

        assertCartValidationFailure(ownerHeader, normalGoodsId, 0);
        assertCartValidationFailure(ownerHeader, normalGoodsId, -1);
        assertCartValidationFailure(ownerHeader, normalGoodsId, 100);
        mvc.perform(post("/api/cart").header("Authorization", ownerHeader).contentType("application/json")
                        .content("{\"goodsId\":" + normalGoodsId + ",\"quantity\":6}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/cart").header("Authorization", ownerHeader).contentType("application/json")
                        .content("{\"goodsId\":999999999,\"quantity\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        Long offSaleGoodsId = goodsIdByName(MARKER + "OFF_SALE_GOODS");
        mvc.perform(post("/api/cart").header("Authorization", ownerHeader).contentType("application/json")
                        .content("{\"goodsId\":" + offSaleGoodsId + ",\"quantity\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        Long closedShopGoodsId = goodsIdByName(MARKER + "CLOSED_SHOP_GOODS");
        mvc.perform(post("/api/cart").header("Authorization", ownerHeader).contentType("application/json")
                        .content("{\"goodsId\":" + closedShopGoodsId + ",\"quantity\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));

        mvc.perform(put("/api/cart/{id}", ownerCartId).header("Authorization", ownerHeader)
                        .contentType("application/json").content("{\"quantity\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.quantity").value(3));
        mvc.perform(put("/api/cart/{id}", ownerCartId).header("Authorization", ownerHeader)
                        .contentType("application/json").content("{\"quantity\":6}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));

        mvc.perform(put("/api/cart/{id}", ownerCartId).header("Authorization", otherHeader)
                        .contentType("application/json").content("{\"quantity\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        mvc.perform(put("/api/cart/{id}/selected", ownerCartId).header("Authorization", otherHeader)
                        .contentType("application/json").content("{\"selected\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        mvc.perform(delete("/api/cart/{id}", ownerCartId).header("Authorization", otherHeader))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));

        Long secondCartId = addCart(ownerHeader, secondGoodsId, 2);
        addCart(otherHeader, normalGoodsId, 1);
        String summary = mvc.perform(get("/api/cart").header("Authorization", ownerHeader))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.shopGroups.length()").value(2))
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.selectedCount").value(5))
                .andExpect(jsonPath("$.data.selectedAmount").value(47.5))
                .andReturn().getResponse().getContentAsString();
        List<String> itemNames = JsonPath.read(summary, "$.data.shopGroups[*].items[*].goodsName");
        assertTrueContains(itemNames, MARKER + "NORMAL_GOODS");
        List<String> itemImages = JsonPath.read(summary, "$.data.shopGroups[*].items[*].goodsImage");
        assertTrueContains(itemImages, "/test/m3a-cart-normal.png");
        List<Number> subtotals = JsonPath.read(summary, "$.data.shopGroups[*].items[*].subtotal");
        assertTrueContainsAmount(subtotals, new BigDecimal("37.50"));

        mvc.perform(put("/api/cart/{id}/selected", ownerCartId).header("Authorization", ownerHeader)
                        .contentType("application/json").content("{\"selected\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.selected").value(false));
        mvc.perform(get("/api/cart").header("Authorization", ownerHeader))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.selectedCount").value(2))
                .andExpect(jsonPath("$.data.selectedAmount").value(10.0));

        mvc.perform(delete("/api/cart").header("Authorization", ownerHeader))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(0, cartCountByUser(ownerId));
        assertEquals(1, cartCountByUser(otherId));
        mvc.perform(get("/api/cart").header("Authorization", ownerHeader))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.shopGroups").isEmpty())
                .andExpect(jsonPath("$.data.selectedCount").value(0))
                .andExpect(jsonPath("$.data.selectedAmount").value(0))
                .andExpect(jsonPath("$.data.totalCount").value(0));
        assertNull(cartMapper.selectById(secondCartId));
        assertNull(UserContext.getUser());
    }

    private void createTestCatalog() {
        Shop shopA = createShop(MARKER + "SHOP_A", 1);
        Shop shopB = createShop(MARKER + "SHOP_B", 1);
        Shop closedShop = createShop(MARKER + "SHOP_CLOSED", 0);
        shopAId = shopA.getId();
        shopBId = shopB.getId();
        normalGoodsId = createGoods(shopAId, MARKER + "NORMAL_GOODS", "ON_SALE", new BigDecimal("12.50"), 5,
                "/test/m3a-cart-normal.png").getId();
        secondGoodsId = createGoods(shopBId, MARKER + "SECOND_GOODS", "ON_SALE", new BigDecimal("5.00"), 10,
                "/test/m3a-cart-second.png").getId();
        createGoods(shopAId, MARKER + "OFF_SALE_GOODS", "OFF_SALE", new BigDecimal("3.00"), 10,
                "/test/m3a-cart-off-sale.png");
        createGoods(closedShop.getId(), MARKER + "CLOSED_SHOP_GOODS", "ON_SALE", new BigDecimal("3.00"), 10,
                "/test/m3a-cart-closed.png");
    }

    private Long addCart(String authorization, Long goodsId, int quantity) throws Exception {
        String response = mvc.perform(post("/api/cart").header("Authorization", authorization)
                        .contentType("application/json")
                        .content("{\"goodsId\":" + goodsId + ",\"quantity\":" + quantity + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.cartId")).longValue();
    }

    private void assertCartValidationFailure(String authorization, Long goodsId, int quantity) throws Exception {
        mvc.perform(post("/api/cart").header("Authorization", authorization).contentType("application/json")
                        .content("{\"goodsId\":" + goodsId + ",\"quantity\":" + quantity + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }

    private String login(String phone, String suffix) throws Exception {
        mvc.perform(post("/api/user/code").contentType("application/json").content("{\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        String code = redis.opsForValue().get(RedisKeys.code(phone));
        assertNotNull(code);
        String response = mvc.perform(post("/api/user/login").contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        User user = userMapper.selectById(userIdByPhone(phone));
        user.setNickname(MARKER + suffix);
        userMapper.updateById(user);
        return JsonPath.read(response, "$.data.token");
    }

    private Shop createShop(String name, int status) {
        Shop shop = new Shop();
        shop.setCategoryId(1L);
        shop.setName(name);
        shop.setAddress(MARKER + "地址");
        shop.setPhone("010-89990001");
        shop.setScore(new BigDecimal("5.00"));
        shop.setStatus(status);
        shop.setIsFeatured(0);
        shop.setCoverImage("/test/m3a-cart-shop.png");
        shop.setSortOrder(99);
        shopMapper.insert(shop);
        return shop;
    }

    private Goods createGoods(Long shopId, String name, String saleStatus, BigDecimal price, int stock, String coverImage) {
        Goods goods = new Goods();
        goods.setShopId(shopId);
        goods.setName(name);
        goods.setDescription(MARKER + "商品说明");
        goods.setPrice(price);
        goods.setStock(stock);
        goods.setSalesCount(0);
        goods.setSaleStatus(saleStatus);
        goods.setCoverImage(coverImage);
        goodsMapper.insert(goods);
        return goods;
    }

    private void cleanupTestData() {
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .in(User::getPhone, Arrays.asList(OWNER_PHONE, OTHER_PHONE)));
        for (User user : users) {
            if (user.getNickname() != null && user.getNickname().startsWith(MARKER)) {
                operateLogMapper.delete(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getUserId, user.getId()));
                cartMapper.delete(Wrappers.<Cart>lambdaQuery().eq(Cart::getUserId, user.getId()));
            }
        }
        List<Goods> goods = goodsMapper.selectList(Wrappers.<Goods>lambdaQuery().likeRight(Goods::getName, MARKER));
        for (Goods item : goods) {
            cartMapper.delete(Wrappers.<Cart>lambdaQuery().eq(Cart::getGoodsId, item.getId()));
        }
        goodsMapper.delete(Wrappers.<Goods>lambdaQuery().likeRight(Goods::getName, MARKER));
        shopMapper.delete(Wrappers.<Shop>lambdaQuery().likeRight(Shop::getName, MARKER));
        for (User user : users) {
            if (user.getNickname() != null && user.getNickname().startsWith(MARKER)) {
                userMapper.deleteById(user.getId());
            }
        }
    }

    private void clearRedisTestKeys() {
        List<String> keys = new ArrayList<String>(Arrays.asList(RedisKeys.code(OWNER_PHONE), RedisKeys.code(OTHER_PHONE)));
        if (ownerToken != null) {
            keys.add(RedisKeys.token(ownerToken));
        }
        if (otherToken != null) {
            keys.add(RedisKeys.token(otherToken));
        }
        redis.delete(keys);
    }

    private Long userIdByPhone(String phone) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone));
        return user == null ? null : user.getId();
    }

    private Long goodsIdByName(String name) {
        Goods goods = goodsMapper.selectOne(Wrappers.<Goods>lambdaQuery().eq(Goods::getName, name));
        return goods == null ? null : goods.getId();
    }

    private int cartQuantity(Long cartId) {
        return jdbc.queryForObject("SELECT quantity FROM qh_cart WHERE id = ?", Integer.class, cartId);
    }

    private int cartCount(Long userId, Long goodsId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM qh_cart WHERE user_id = ? AND goods_id = ?", Integer.class, userId, goodsId);
    }

    private int cartCountByUser(Long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM qh_cart WHERE user_id = ?", Integer.class, userId);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void assertTrueContains(List<String> values, String expected) {
        assertFalse(values == null || !values.contains(expected));
    }

    private void assertTrueContainsAmount(List<Number> values, BigDecimal expected) {
        for (Number value : values) {
            if (new BigDecimal(value.toString()).compareTo(expected) == 0) {
                return;
            }
        }
        throw new AssertionError("未找到金额: " + expected);
    }
}
