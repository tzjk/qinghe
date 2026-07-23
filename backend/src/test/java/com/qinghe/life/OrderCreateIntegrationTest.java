package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jayway.jsonpath.JsonPath;
import com.qinghe.life.entity.Building;
import com.qinghe.life.entity.Cart;
import com.qinghe.life.entity.Campus;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.OperateLog;
import com.qinghe.life.entity.Order;
import com.qinghe.life.entity.OrderItem;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.entity.User;
import com.qinghe.life.entity.UserAddress;
import com.qinghe.life.enums.OrderStatus;
import com.qinghe.life.mapper.BuildingMapper;
import com.qinghe.life.mapper.CartMapper;
import com.qinghe.life.mapper.CampusMapper;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.OperateLogMapper;
import com.qinghe.life.mapper.OrderItemMapper;
import com.qinghe.life.mapper.OrderMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.mapper.UserAddressMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderCreateIntegrationTest {
    private static final String MARKER = "ORDER_CREATE_TEST_";
    private static final String OWNER_PHONE = "13900009311";
    private static final String OTHER_PHONE = "13900009312";
    private static final String CONCURRENT_A_PHONE = "13900009313";
    private static final String CONCURRENT_B_PHONE = "13900009314";
    private static final List<String> TEST_PHONES = Arrays.asList(OWNER_PHONE, OTHER_PHONE, CONCURRENT_A_PHONE, CONCURRENT_B_PHONE);

    @Autowired private MockMvc mvc;
    @Autowired private StringRedisTemplate redis;
    @Autowired private UserMapper userMapper;
    @Autowired private ShopMapper shopMapper;
    @Autowired private GoodsMapper goodsMapper;
    @Autowired private CartMapper cartMapper;
    @Autowired private UserAddressMapper addressMapper;
    @Autowired private CampusMapper campusMapper;
    @Autowired private BuildingMapper buildingMapper;
    @Autowired private OperateLogMapper operateLogMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private OrderItemMapper orderItemMapper;

    @SpyBean private OrderMapper orderMapperSpy;
    @SpyBean private OrderItemMapper orderItemMapperSpy;
    @SpyBean private CartMapper cartMapperSpy;
    @SpyBean private GoodsMapper goodsMapperSpy;

    private String ownerToken;
    private Long ownerId;
    private Campus activeCampus;
    private Building activeBuilding;
    private final Set<Long> createdUserIds = new LinkedHashSet<Long>();
    private final Set<String> createdTokens = new LinkedHashSet<String>();

    @BeforeEach
    void setUp() throws Exception {
        cleanupTestData();
        activeCampus = campusMapper.selectOne(Wrappers.<Campus>lambdaQuery().eq(Campus::getStatus, 1).last("LIMIT 1"));
        assertNotNull(activeCampus, "订单测试需要现有启用校区");
        activeBuilding = buildingMapper.selectOne(Wrappers.<Building>lambdaQuery()
                .eq(Building::getCampusId, activeCampus.getId()).eq(Building::getStatus, 1).last("LIMIT 1"));
        assertNotNull(activeBuilding, "订单测试需要现有启用楼栋");
        ownerToken = login(OWNER_PHONE, "OWNER");
        ownerId = userId(OWNER_PHONE);
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(orderMapperSpy, orderItemMapperSpy, cartMapperSpy, goodsMapperSpy);
        cleanupTestData();
        UserContext.clear();
    }

    @AfterAll
    void assertsNoOrderCreateTestResidue() {
        cleanupTestData();
        assertEquals(0L, shopMapper.selectCount(Wrappers.<Shop>lambdaQuery().likeRight(Shop::getName, MARKER)));
        assertEquals(0L, goodsMapper.selectCount(Wrappers.<Goods>lambdaQuery().likeRight(Goods::getName, MARKER)));
        assertEquals(0L, addressMapper.selectCount(Wrappers.<UserAddress>lambdaQuery().likeRight(UserAddress::getReceiverName, MARKER)));
        assertEquals(0L, orderMapper.selectCount(Wrappers.<Order>lambdaQuery().likeRight(Order::getRemark, MARKER)));
        assertEquals(0L, orderItemMapper.selectCount(Wrappers.<OrderItem>lambdaQuery().likeRight(OrderItem::getGoodsName, MARKER)));
        assertEquals(0L, userMapper.selectCount(Wrappers.<User>lambdaQuery().in(User::getPhone, TEST_PHONES)
                .likeRight(User::getNickname, MARKER)));
        assertEquals(0L, cartMapper.selectCount(Wrappers.<Cart>lambdaQuery().in(Cart::getUserId, createdUserIds)));
        assertEquals(0L, operateLogMapper.selectCount(Wrappers.<OperateLog>lambdaQuery().in(OperateLog::getUserId, createdUserIds)));
        for (String phone : TEST_PHONES) {
            assertFalse(Boolean.TRUE.equals(redis.hasKey(RedisKeys.code(phone))));
        }
        for (String token : createdTokens) assertFalse(Boolean.TRUE.equals(redis.hasKey(RedisKeys.token(token))));
        assertTrue(testSessionKeys(createdUserIds).isEmpty());
    }

    @Test
    void rejectsUnauthenticatedAndInvalidOrderRequests() throws Exception {
        mvc.perform(post("/api/orders").contentType("application/json")
                        .content("{\"cartItemIds\":[1],\"addressId\":1}"))
                .andExpect(status().isUnauthorized());
        String header = bearer(ownerToken);
        mvc.perform(post("/api/orders").header("Authorization", header).contentType("application/json")
                        .content("{\"cartItemIds\":[],\"addressId\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/orders").header("Authorization", header).contentType("application/json")
                        .content("{\"cartItemIds\":[999999999],\"addressId\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));

        Long shopId = createShop("REQUEST", 1).getId();
        Long goodsId = createGoods(shopId, "REQUEST", "ON_SALE", "10.00", 5).getId();
        Long zeroQuantityCart = createCart(ownerId, shopId, goodsId, 0);
        Long addressId = createAddress(ownerId, "REQUEST");
        assertCode(orderRequest(header, Arrays.asList(zeroQuantityCart), addressId, MARKER + "REQUEST"), 400);
        assertCode(orderRequest(header, Arrays.asList(zeroQuantityCart, zeroQuantityCart), addressId, MARKER + "DUPLICATE"), 400);
        String tooLongRemark = String.join("", Collections.nCopies(256, "x"));
        mvc.perform(post("/api/orders").header("Authorization", header).contentType("application/json")
                        .content(orderJson(Arrays.asList(zeroQuantityCart), addressId, tooLongRemark, "")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void rejectsOwnershipLiveCatalogAndInvalidAddressCases() throws Exception {
        String header = bearer(ownerToken);
        String otherToken = login(OTHER_PHONE, "OTHER");
        Long otherId = userId(OTHER_PHONE);
        Long shopA = createShop("A", 1).getId();
        Long shopB = createShop("B", 1).getId();
        Long closedShop = createShop("CLOSED", 0).getId();
        Long goodA = createGoods(shopA, "A", "ON_SALE", "10.00", 5).getId();
        Long goodB = createGoods(shopB, "B", "ON_SALE", "10.00", 5).getId();
        Long offSale = createGoods(shopA, "OFF", "OFF_SALE", "10.00", 5).getId();
        Long closedGood = createGoods(closedShop, "CLOSED", "ON_SALE", "10.00", 5).getId();
        Long soldOut = createGoods(shopA, "SOLD_OUT", "ON_SALE", "10.00", 0).getId();
        Long addressId = createAddress(ownerId, "CATALOG");

        Long foreignCart = createCart(otherId, shopA, goodA, 1);
        assertCode(orderRequest(header, Arrays.asList(foreignCart), addressId, MARKER + "FOREIGN"), 404);
        assertCode(orderRequest(bearer(otherToken), Arrays.asList(foreignCart), createAddress(otherId, "OTHER"), MARKER + "OTHER_OK"), 200);
        Long crossA = createCart(ownerId, shopA, goodA, 1);
        Long crossB = createCart(ownerId, shopB, goodB, 1);
        assertCode(orderRequest(header, Arrays.asList(crossA, crossB), addressId, MARKER + "CROSS"), 400);
        cartMapper.deleteById(crossA);
        cartMapper.deleteById(crossB);
        assertCode(orderRequest(header, Arrays.asList(createCart(ownerId, shopA, offSale, 1)), addressId, MARKER + "OFF"), 400);
        assertCode(orderRequest(header, Arrays.asList(createCart(ownerId, closedShop, closedGood, 1)), addressId, MARKER + "CLOSED"), 400);
        assertCode(orderRequest(header, Arrays.asList(createCart(ownerId, shopA, soldOut, 1)), addressId, MARKER + "SOLD"), 400);
        assertCode(orderRequest(header, Arrays.asList(createCart(ownerId, shopA, 999999999L, 1)), addressId, MARKER + "MISSING"), 404);
        assertCode(orderRequest(header, Arrays.asList(createCart(ownerId, shopA, goodB, 1)), addressId, MARKER + "MOVED"), 409);
        Long missingAddressCart = createCart(ownerId, shopA, goodA, 1);
        assertCode(orderRequest(header, Arrays.asList(missingAddressCart), 999999999L, MARKER + "ADDRESS_MISSING"), 404);
        cartMapper.deleteById(missingAddressCart);
        Long foreignAddressCart = createCart(ownerId, shopA, goodA, 1);
        assertCode(orderRequest(header, Arrays.asList(foreignAddressCart), createAddress(otherId, "FOREIGN_ADDRESS"), MARKER + "ADDRESS_FOREIGN"), 404);
        cartMapper.deleteById(foreignAddressCart);

        Building disabledBuilding = new Building();
        disabledBuilding.setCampusId(activeCampus.getId());
        disabledBuilding.setArea(activeBuilding.getArea());
        disabledBuilding.setBuildingType(activeBuilding.getBuildingType());
        disabledBuilding.setBuildingName(MARKER + "DISABLED_BUILDING");
        disabledBuilding.setBuildingCode("OCT_DISABLED");
        disabledBuilding.setStatus(0);
        disabledBuilding.setSortOrder(9999);
        buildingMapper.insert(disabledBuilding);
        UserAddress invalid = addressMapper.selectById(addressId);
        invalid.setBuildingId(disabledBuilding.getId());
        addressMapper.updateById(invalid);
        assertCode(orderRequest(header, Arrays.asList(createCart(ownerId, shopA, goodA, 1)), addressId, MARKER + "ADDRESS_INVALID"), 400);
    }

    @Test
    void persistsServerCalculatedMoneySnapshotsAndPreciselyClearsSubmittedCarts() throws Exception {
        String header = bearer(ownerToken);
        String otherToken = login(OTHER_PHONE, "OTHER");
        Long otherId = userId(OTHER_PHONE);
        Long shopA = createShop("MONEY_A", 1).getId();
        Long shopB = createShop("MONEY_B", 1).getId();
        Goods first = createGoods(shopA, "MONEY_A", "ON_SALE", "10.01", 10);
        Goods second = createGoods(shopA, "MONEY_B", "ON_SALE", "2.50", 10);
        Goods unsubmittedGoods = createGoods(shopA, "MONEY_UNSUBMITTED", "ON_SALE", "1.00", 10);
        Goods otherShopGoods = createGoods(shopB, "MONEY_OTHER", "ON_SALE", "3.00", 10);
        Long firstCart = createCart(ownerId, shopA, first.getId(), 2);
        Long secondCart = createCart(ownerId, shopA, second.getId(), 2);
        Long unsubmittedCart = createCart(ownerId, shopA, unsubmittedGoods.getId(), 1);
        Long otherShopCart = createCart(ownerId, shopB, otherShopGoods.getId(), 1);
        Long otherUserCart = createCart(otherId, shopA, first.getId(), 1);
        Long addressId = createAddress(ownerId, "SNAPSHOT");

        String body = orderRequest(header, Arrays.asList(firstCart, secondCart), addressId, MARKER + "MONEY_SNAPSHOT",
                "\"totalAmount\":0.01,\"discountAmount\":99.99,\"deliveryFee\":99.99,\"payAmount\":0.01,\"stock\":999");
        assertCode(body, 200);
        assertFalse(body.contains(ownerToken));
        assertFalse(body.contains("receiverPhone"));
        assertFalse(body.contains("password"));
        Long orderId = number(body, "$.data.orderId");
        Order order = orderMapper.selectById(orderId);
        assertEquals(0, new BigDecimal("25.02").compareTo(order.getTotalAmount()));
        assertEquals(0, new BigDecimal("0.00").compareTo(order.getDiscountAmount()));
        assertEquals(0, new BigDecimal("0.00").compareTo(order.getDeliveryFee()));
        assertEquals(0, new BigDecimal("25.02").compareTo(order.getPayAmount()));
        assertEquals(OrderStatus.PENDING_PAY.getCode(), order.getStatus());
        assertEquals(activeCampus.getCampusName(), order.getCampusName());
        assertEquals(activeBuilding.getBuildingName(), order.getBuildingName());
        assertEquals(2, orderItemMapper.selectCount(Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, orderId)).intValue());
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, orderId));
        assertTrue(items.stream().anyMatch(item -> item.getGoodsId().equals(first.getId()) && item.getQuantity() == 2
                && item.getGoodsPrice().compareTo(new BigDecimal("10.01")) == 0 && item.getSubtotal().compareTo(new BigDecimal("20.02")) == 0));
        assertTrue(items.stream().anyMatch(item -> item.getGoodsId().equals(second.getId()) && item.getQuantity() == 2
                && item.getGoodsPrice().compareTo(new BigDecimal("2.50")) == 0 && item.getSubtotal().compareTo(new BigDecimal("5.00")) == 0));
        assertNull(cartMapper.selectById(firstCart));
        assertNull(cartMapper.selectById(secondCart));
        assertNotNull(cartMapper.selectById(unsubmittedCart));
        assertNotNull(cartMapper.selectById(otherShopCart));
        assertNotNull(cartMapper.selectById(otherUserCart));
        assertEquals(8, goodsMapper.selectById(first.getId()).getStock().intValue());
        assertEquals(8, goodsMapper.selectById(second.getId()).getStock().intValue());

        OrderItem firstItem = items.stream().filter(item -> item.getGoodsId().equals(first.getId())).findFirst().orElseThrow(AssertionError::new);
        String oldName = firstItem.getGoodsName();
        String oldImage = firstItem.getGoodsImage();
        first.setName(MARKER + "MUTATED_NAME"); first.setCoverImage("/mutated.png"); first.setPrice(new BigDecimal("88.88"));
        goodsMapper.updateById(first);
        UserAddress currentAddress = addressMapper.selectById(addressId);
        currentAddress.setReceiverName(MARKER + "MUTATED_ADDRESS"); currentAddress.setDetail("mutated");
        addressMapper.updateById(currentAddress);
        assertEquals(oldName, orderItemMapper.selectById(firstItem.getId()).getGoodsName());
        assertEquals(oldImage, orderItemMapper.selectById(firstItem.getId()).getGoodsImage());
        assertEquals(MARKER + "SNAPSHOT", orderMapper.selectById(orderId).getReceiverName());
    }

    @Test
    void allowsOnlyOneConcurrentPurchaseOfTheLastItemAndKeepsLoserCart() throws Exception {
        String tokenA = login(CONCURRENT_A_PHONE, "CONCURRENT_A");
        String tokenB = login(CONCURRENT_B_PHONE, "CONCURRENT_B");
        Long userA = userId(CONCURRENT_A_PHONE);
        Long userB = userId(CONCURRENT_B_PHONE);
        Long shopId = createShop("CONCURRENT", 1).getId();
        Long goodsId = createGoods(shopId, "CONCURRENT", "ON_SALE", "9.99", 1).getId();
        Long cartA = createCart(userA, shopId, goodsId, 1);
        Long cartB = createCart(userB, shopId, goodsId, 1);
        Long addressA = createAddress(userA, "CONCURRENT_A");
        Long addressB = createAddress(userB, "CONCURRENT_B");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<String> first = executor.submit(() -> { start.await(); return orderRequest(bearer(tokenA), Arrays.asList(cartA), addressA, MARKER + "CONCURRENT_A"); });
            Future<String> second = executor.submit(() -> { start.await(); return orderRequest(bearer(tokenB), Arrays.asList(cartB), addressB, MARKER + "CONCURRENT_B"); });
            start.countDown();
            List<Integer> codes = Arrays.asList(code(first.get(30, TimeUnit.SECONDS)), code(second.get(30, TimeUnit.SECONDS)));
            assertEquals(1L, codes.stream().filter(value -> value == 200).count());
            assertEquals(1L, codes.stream().filter(value -> value == 400).count());
        } finally {
            executor.shutdownNow();
        }
        assertEquals(0, goodsMapper.selectById(goodsId).getStock().intValue());
        assertEquals(1, orderMapper.selectCount(Wrappers.<Order>lambdaQuery().likeRight(Order::getRemark, MARKER + "CONCURRENT")).intValue());
        assertEquals(1, orderItemMapper.selectCount(Wrappers.<OrderItem>lambdaQuery().in(OrderItem::getGoodsId, Collections.singletonList(goodsId))).intValue());
        assertTrue(cartMapper.selectById(cartA) == null || cartMapper.selectById(cartB) == null);
        assertTrue(cartMapper.selectById(cartA) != null || cartMapper.selectById(cartB) != null);
    }

    @Test
    void rollsBackWriteAndCartDeletionFailuresWithoutOrphanedData() throws Exception {
        String header = bearer(ownerToken);
        Long shopId = createShop("ROLLBACK", 1).getId();
        Long goodsId = createGoods(shopId, "ROLLBACK", "ON_SALE", "6.00", 4).getId();
        Long addressId = createAddress(ownerId, "ROLLBACK");
        Long masterCart = createCart(ownerId, shopId, goodsId, 1);
        int originalStock = goodsMapper.selectById(goodsId).getStock();

        Mockito.doThrow(new DuplicateKeyException("uk_qh_order_no")).when(orderMapperSpy).insert(any(Order.class));
        String masterFailure = orderRequest(header, Arrays.asList(masterCart), addressId, MARKER + "ROLLBACK_MASTER");
        assertEquals(409, code(masterFailure));
        assertRollbackState(goodsId, originalStock, masterCart);
        Mockito.reset(orderMapperSpy);
        cartMapper.deleteById(masterCart);

        Long itemCart = createCart(ownerId, shopId, goodsId, 1);
        Mockito.doThrow(new RuntimeException("ORDER_CREATE_TEST_ITEM_WRITE")).when(orderItemMapperSpy).insert(any(OrderItem.class));
        assertEquals(500, code(orderRequest(header, Arrays.asList(itemCart), addressId, MARKER + "ROLLBACK_ITEM")));
        assertRollbackState(goodsId, originalStock, itemCart);
        Mockito.reset(orderItemMapperSpy);
        cartMapper.deleteById(itemCart);

        Long cartFailure = createCart(ownerId, shopId, goodsId, 1);
        Mockito.doReturn(0).when(cartMapperSpy).delete(any());
        assertEquals(409, code(orderRequest(header, Arrays.asList(cartFailure), addressId, MARKER + "ROLLBACK_CART")));
        assertRollbackState(goodsId, originalStock, cartFailure);
        Mockito.reset(cartMapperSpy);
    }

    private void assertRollbackState(Long goodsId, int originalStock, Long cartId) {
        assertEquals(originalStock, goodsMapper.selectById(goodsId).getStock().intValue());
        assertNotNull(cartMapper.selectById(cartId));
        assertEquals(0, orderMapper.selectCount(Wrappers.<Order>lambdaQuery().likeRight(Order::getRemark, MARKER + "ROLLBACK")).intValue());
        assertEquals(0, orderItemMapper.selectCount(Wrappers.<OrderItem>lambdaQuery().in(OrderItem::getGoodsId, Collections.singletonList(goodsId))).intValue());
    }

    private String login(String phone, String suffix) throws Exception {
        mvc.perform(post("/api/user/code").contentType("application/json").content("{\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        String verificationCode = redis.opsForValue().get(RedisKeys.code(phone));
        assertNotNull(verificationCode);
        String response = mvc.perform(post("/api/user/login").contentType("application/json")
                        .content("{\"phone\":\"" + phone + "\",\"code\":\"" + verificationCode + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        User user = userMapper.selectById(userId(phone));
        user.setNickname(MARKER + suffix);
        userMapper.updateById(user);
        Long userId = userId(phone);
        assertNotNull(userId);
        String token = JsonPath.read(response, "$.data.token");
        createdUserIds.add(userId);
        createdTokens.add(token);
        return token;
    }

    private Shop createShop(String suffix, int status) {
        Shop shop = new Shop();
        shop.setCategoryId(1L); shop.setName(MARKER + suffix); shop.setAddress(MARKER + "地址");
        shop.setPhone("010-89990001"); shop.setScore(new BigDecimal("5.00")); shop.setStatus(status);
        shop.setIsFeatured(0); shop.setCoverImage("/order-create-test-shop.png"); shop.setSortOrder(9999);
        shopMapper.insert(shop);
        return shop;
    }

    private Goods createGoods(Long shopId, String suffix, String saleStatus, String price, int stock) {
        Goods goods = new Goods();
        goods.setShopId(shopId); goods.setName(MARKER + suffix); goods.setDescription(MARKER + "商品");
        goods.setPrice(new BigDecimal(price)); goods.setStock(stock); goods.setSalesCount(0);
        goods.setSaleStatus(saleStatus); goods.setCoverImage("/order-create-test-" + suffix + ".png");
        goodsMapper.insert(goods);
        return goods;
    }

    private Long createCart(Long userId, Long shopId, Long goodsId, int quantity) {
        Cart cart = new Cart();
        cart.setUserId(userId); cart.setShopId(shopId); cart.setGoodsId(goodsId); cart.setQuantity(quantity); cart.setSelected(1);
        cartMapper.insert(cart);
        return cart.getId();
    }

    private Long createAddress(Long userId, String suffix) {
        UserAddress address = new UserAddress();
        address.setUserId(userId); address.setReceiverName(MARKER + suffix); address.setReceiverPhone("13900000000");
        address.setCampusId(activeCampus.getId()); address.setArea(activeBuilding.getArea()); address.setBuildingId(activeBuilding.getId());
        address.setBuildingType(activeBuilding.getBuildingType()); address.setBuildingName(activeBuilding.getBuildingName());
        address.setFloor("3"); address.setRoomNo("301"); address.setDeliveryPoint("门口"); address.setDetail(MARKER + "地址详情");
        address.setAddressType("CAMPUS"); address.setIsDefault(0);
        addressMapper.insert(address);
        return address.getId();
    }

    private String orderRequest(String authorization, List<Long> cartIds, Long addressId, String remark) throws Exception {
        return orderRequest(authorization, cartIds, addressId, remark, "");
    }

    private String orderRequest(String authorization, List<Long> cartIds, Long addressId, String remark, String extra) throws Exception {
        return mvc.perform(post("/api/orders").header("Authorization", authorization).contentType("application/json")
                        .content(orderJson(cartIds, addressId, remark, extra)))
                .andReturn().getResponse().getContentAsString();
    }

    private String orderJson(List<Long> cartIds, Long addressId, String remark, String extra) {
        String ids = cartIds.toString();
        return "{\"cartItemIds\":" + ids + ",\"addressId\":" + addressId + ",\"remark\":\"" + remark + "\""
                + (extra.isEmpty() ? "" : "," + extra) + "}";
    }

    private int code(String response) {
        return ((Number) JsonPath.read(response, "$.code")).intValue();
    }

    private void assertCode(String response, int expected) { assertEquals(expected, code(response)); }
    private Long number(String response, String path) { return ((Number) JsonPath.read(response, path)).longValue(); }
    private Long userId(String phone) { User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone)); return user == null ? null : user.getId(); }
    private String bearer(String token) { return "Bearer " + token; }

    private void cleanupTestData() {
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery().in(User::getPhone, TEST_PHONES));
        List<Long> userIds = new ArrayList<Long>();
        for (User user : users) {
            if (user.getNickname() != null && user.getNickname().startsWith(MARKER)) userIds.add(user.getId());
        }
        List<Order> orders = orderMapper.selectList(Wrappers.<Order>lambdaQuery().likeRight(Order::getRemark, MARKER));
        for (Order order : orders) orderItemMapper.delete(Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, order.getId()));
        orderMapper.delete(Wrappers.<Order>lambdaQuery().likeRight(Order::getRemark, MARKER));
        if (!userIds.isEmpty()) {
            cartMapper.delete(Wrappers.<Cart>lambdaQuery().in(Cart::getUserId, userIds));
            addressMapper.delete(Wrappers.<UserAddress>lambdaQuery().in(UserAddress::getUserId, userIds));
            operateLogMapper.delete(Wrappers.<OperateLog>lambdaQuery().in(OperateLog::getUserId, userIds));
        }
        buildingMapper.delete(Wrappers.<Building>lambdaQuery().likeRight(Building::getBuildingName, MARKER));
        List<Goods> goods = goodsMapper.selectList(Wrappers.<Goods>lambdaQuery().likeRight(Goods::getName, MARKER));
        for (Goods good : goods) cartMapper.delete(Wrappers.<Cart>lambdaQuery().eq(Cart::getGoodsId, good.getId()));
        goodsMapper.delete(Wrappers.<Goods>lambdaQuery().likeRight(Goods::getName, MARKER));
        shopMapper.delete(Wrappers.<Shop>lambdaQuery().likeRight(Shop::getName, MARKER));
        for (User user : users) {
            if (user.getNickname() != null && user.getNickname().startsWith(MARKER)) userMapper.deleteById(user.getId());
        }
        List<String> redisKeys = new ArrayList<String>();
        for (String phone : TEST_PHONES) redisKeys.add(RedisKeys.code(phone));
        for (String token : createdTokens) redisKeys.add(RedisKeys.token(token));
        redisKeys.addAll(testSessionKeys(userIds));
        redis.delete(redisKeys);
    }

    private Set<String> testSessionKeys(java.util.Collection<Long> userIds) {
        Set<String> keys = new LinkedHashSet<String>();
        if (userIds == null || userIds.isEmpty()) return keys;
        Set<String> candidates = redis.keys("qh:login:token:*");
        if (candidates == null) return keys;
        for (String key : candidates) {
            Object userId = redis.opsForHash().get(key, "id");
            if (userId != null && userIds.contains(Long.valueOf(String.valueOf(userId)))) keys.add(key);
        }
        return keys;
    }
}
