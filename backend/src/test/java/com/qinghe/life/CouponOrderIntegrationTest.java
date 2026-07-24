package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinghe.life.dto.OrderCreateDTO;
import com.qinghe.life.entity.Building;
import com.qinghe.life.entity.Campus;
import com.qinghe.life.entity.Cart;
import com.qinghe.life.entity.Coupon;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.OperateLog;
import com.qinghe.life.entity.Order;
import com.qinghe.life.entity.OrderItem;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.entity.User;
import com.qinghe.life.entity.UserAddress;
import com.qinghe.life.entity.UserCoupon;
import com.qinghe.life.enums.CouponStatus;
import com.qinghe.life.enums.OrderStatus;
import com.qinghe.life.enums.UserCouponStatus;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.BuildingMapper;
import com.qinghe.life.mapper.CampusMapper;
import com.qinghe.life.mapper.CartMapper;
import com.qinghe.life.mapper.CouponMapper;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.OperateLogMapper;
import com.qinghe.life.mapper.OrderItemMapper;
import com.qinghe.life.mapper.OrderMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.mapper.UserAddressMapper;
import com.qinghe.life.mapper.UserCouponMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.service.CouponService;
import com.qinghe.life.service.OrderService;
import com.qinghe.life.service.impl.OrderCancellationService;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.OrderCreateVO;
import com.qinghe.life.vo.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CouponOrderIntegrationTest {
    private static final String MARKER = "COUPON_ORDER_TEST_";
    @Autowired private CouponService couponService;
    @Autowired private OrderService orderService;
    @Autowired private OrderCancellationService cancellationService;
    @Autowired private CouponMapper couponMapper;
    @Autowired private UserCouponMapper userCouponMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private ShopMapper shopMapper;
    @Autowired private GoodsMapper goodsMapper;
    @Autowired private CartMapper cartMapper;
    @Autowired private UserAddressMapper addressMapper;
    @Autowired private CampusMapper campusMapper;
    @Autowired private BuildingMapper buildingMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private OrderItemMapper orderItemMapper;
    @Autowired private OperateLogMapper operateLogMapper;
    @Autowired private ObjectMapper objectMapper;
    @SpyBean private OrderItemMapper orderItemMapperSpy;

    private Campus campus;
    private Building building;
    private User owner;
    private Shop shop;
    private Goods goods;

    @BeforeEach
    void setUp() {
        cleanup();
        campus = campusMapper.selectOne(Wrappers.<Campus>lambdaQuery().eq(Campus::getStatus, 1).last("LIMIT 1"));
        building = campus == null ? null : buildingMapper.selectOne(Wrappers.<Building>lambdaQuery()
                .eq(Building::getCampusId, campus.getId()).eq(Building::getStatus, 1).last("LIMIT 1"));
        assertNotNull(campus, "优惠券订单测试需要启用校区");
        assertNotNull(building, "优惠券订单测试需要启用楼栋");
        owner = user("OWNER", "13933330001");
        shop = shop("MAIN");
        goods = goods(shop, "MAIN", "20.00", 30);
        asUser(owner);
    }

    @AfterEach
    void tearDown() {
        reset(orderItemMapperSpy);
        UserContext.clear();
        cleanup();
    }

    @Test
    void claimsCouponNormally() {
        Coupon coupon = coupon("CLAIM", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 1);
        assertNotNull(couponService.claim(coupon.getId()).getId());
        assertEquals(0, couponMapper.selectById(coupon.getId()).getAvailableStock().intValue());
        assertEquals(UserCouponStatus.AVAILABLE.name(), userCoupon(coupon, owner).getStatus());
    }

    @Test
    void rejectsCouponBeforeReceiveWindow() {
        Coupon coupon = coupon("NOT_STARTED", LocalDateTime.now().plusMinutes(1), LocalDateTime.now().plusMinutes(2), 1);
        assertThrows(BusinessException.class, () -> couponService.claim(coupon.getId()));
    }

    @Test
    void rejectsCouponAfterReceiveWindow() {
        Coupon coupon = coupon("ENDED", LocalDateTime.now().minusMinutes(2), LocalDateTime.now().minusMinutes(1), 1);
        assertThrows(BusinessException.class, () -> couponService.claim(coupon.getId()));
    }

    @Test
    void rejectsClaimWhenStockIsExhausted() {
        Coupon coupon = coupon("SOLD_OUT", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 0);
        assertThrows(BusinessException.class, () -> couponService.claim(coupon.getId()));
    }

    @Test
    void enforcesPerUserLimitAndReturnsExistingClaimOnRetry() {
        Coupon coupon = coupon("LIMIT", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2);
        Long first = couponService.claim(coupon.getId()).getId();
        Long second = couponService.claim(coupon.getId()).getId();
        assertEquals(first, second);
        assertEquals(1L, userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery().eq(UserCoupon::getUserId, owner.getId()).eq(UserCoupon::getCouponId, coupon.getId())).longValue());
        assertEquals(1, couponMapper.selectById(coupon.getId()).getAvailableStock().intValue());
    }

    @Test
    void duplicateClaimDoesNotCreateSecondRecord() {
        Coupon coupon = coupon("DUPLICATE", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2);
        couponService.claim(coupon.getId());
        couponService.claim(coupon.getId());
        assertEquals(1L, userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery().eq(UserCoupon::getCouponId, coupon.getId()).eq(UserCoupon::getUserId, owner.getId())).longValue());
    }

    @Test
    void rejectsAnotherUsersCouponAtOrderCreate() {
        User other = user("OTHER", "13933330002");
        Coupon coupon = coupon("FOREIGN", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2);
        UserCoupon foreign = availableCoupon(coupon, other, LocalDateTime.now().plusMinutes(5));
        assertThrows(BusinessException.class, () -> createOrder(owner, shop, goods, foreign.getId(), "FOREIGN"));
    }

    @Test
    void rejectsUsedCouponAtOrderCreate() {
        Coupon coupon = coupon("USED", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2);
        UserCoupon used = availableCoupon(coupon, owner, LocalDateTime.now().plusMinutes(5));
        used.setStatus(UserCouponStatus.USED.name()); used.setUseTime(LocalDateTime.now()); userCouponMapper.updateById(used);
        assertThrows(BusinessException.class, () -> createOrder(owner, shop, goods, used.getId(), "USED"));
    }

    @Test
    void rejectsExpiredCouponAtOrderCreate() {
        Coupon coupon = coupon("EXPIRED", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2);
        UserCoupon expired = availableCoupon(coupon, owner, LocalDateTime.now().minusSeconds(1));
        expired.setStatus(UserCouponStatus.EXPIRED.name()); userCouponMapper.updateById(expired);
        assertThrows(BusinessException.class, () -> createOrder(owner, shop, goods, expired.getId(), "EXPIRED"));
    }

    @Test
    void rejectsCouponForDifferentShop() {
        Shop otherShop = shop("OTHER_SHOP");
        Coupon coupon = coupon("SHOP", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2);
        coupon.setShopId(otherShop.getId()); couponMapper.updateById(coupon);
        UserCoupon userCoupon = availableCoupon(coupon, owner, LocalDateTime.now().plusMinutes(5));
        assertThrows(BusinessException.class, () -> createOrder(owner, shop, goods, userCoupon.getId(), "SHOP"));
    }

    @Test
    void rejectsCouponWhenThresholdIsNotMet() {
        Coupon coupon = coupon("THRESHOLD", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2);
        coupon.setThresholdAmount(new BigDecimal("50.00")); couponMapper.updateById(coupon);
        UserCoupon userCoupon = availableCoupon(coupon, owner, LocalDateTime.now().plusMinutes(5));
        assertThrows(BusinessException.class, () -> createOrder(owner, shop, goods, userCoupon.getId(), "THRESHOLD"));
    }

    @Test
    void ignoresForgedClientMoneyFields() throws Exception {
        OrderCreateDTO request = objectMapper.readValue("{\"cartItemIds\":[1],\"addressId\":1,\"userCouponId\":2,\"discountAmount\":999.99,\"payAmount\":0.01,\"discountRate\":0.01}", OrderCreateDTO.class);
        assertEquals(Long.valueOf(2L), request.getUserCouponId());
        assertEquals(1, request.getCartItemIds().size());
    }

    @Test
    void calculatesPayAmountOnlyOnServer() {
        Coupon coupon = coupon("CALCULATE", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2);
        coupon.setDiscountAmount(new BigDecimal("3.33")); couponMapper.updateById(coupon);
        UserCoupon userCoupon = availableCoupon(coupon, owner, LocalDateTime.now().plusMinutes(5));
        OrderCreateVO result = createOrder(owner, shop, goods, userCoupon.getId(), "CALCULATE");
        assertEquals(new BigDecimal("20.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("3.33"), result.getDiscountAmount());
        assertEquals(new BigDecimal("16.67"), result.getPayAmount());
    }

    @Test
    void locksCouponWhenOrderIsCreated() {
        Coupon coupon = coupon("LOCK", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2);
        UserCoupon userCoupon = availableCoupon(coupon, owner, LocalDateTime.now().plusMinutes(5));
        OrderCreateVO result = createOrder(owner, shop, goods, userCoupon.getId(), "LOCK");
        UserCoupon locked = userCouponMapper.selectById(userCoupon.getId());
        assertEquals(UserCouponStatus.LOCKED.name(), locked.getStatus());
        assertEquals(result.getOrderId(), locked.getOrderId());
        assertNotNull(locked.getLockTime());
    }

    @Test
    void redeemsCouponAfterPayment() {
        Coupon coupon = coupon("PAY", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2);
        UserCoupon userCoupon = availableCoupon(coupon, owner, LocalDateTime.now().plusMinutes(5));
        OrderCreateVO result = createOrder(owner, shop, goods, userCoupon.getId(), "PAY");
        orderService.simulatePay(result.getOrderId());
        UserCoupon used = userCouponMapper.selectById(userCoupon.getId());
        assertEquals(UserCouponStatus.USED.name(), used.getStatus());
        assertNotNull(used.getUseTime());
    }

    @Test
    void repeatedPaymentDoesNotRedeemCouponTwice() {
        Coupon coupon = coupon("REPEAT_PAY", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2);
        UserCoupon userCoupon = availableCoupon(coupon, owner, LocalDateTime.now().plusMinutes(5));
        OrderCreateVO result = createOrder(owner, shop, goods, userCoupon.getId(), "REPEAT_PAY");
        orderService.simulatePay(result.getOrderId());
        LocalDateTime useTime = userCouponMapper.selectById(userCoupon.getId()).getUseTime();
        assertThrows(BusinessException.class, () -> orderService.simulatePay(result.getOrderId()));
        UserCoupon used = userCouponMapper.selectById(userCoupon.getId());
        assertEquals(UserCouponStatus.USED.name(), used.getStatus());
        assertEquals(useTime, used.getUseTime());
    }

    @Test
    void releasesCouponAfterActiveCancellationExactlyOnce() {
        Coupon coupon = coupon("CANCEL", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2);
        UserCoupon userCoupon = availableCoupon(coupon, owner, LocalDateTime.now().plusMinutes(5));
        OrderCreateVO result = createOrder(owner, shop, goods, userCoupon.getId(), "CANCEL");
        orderService.cancel(result.getOrderId());
        assertEquals(UserCouponStatus.AVAILABLE.name(), userCouponMapper.selectById(userCoupon.getId()).getStatus());
        assertThrows(BusinessException.class, () -> orderService.cancel(result.getOrderId()));
        assertEquals(UserCouponStatus.AVAILABLE.name(), userCouponMapper.selectById(userCoupon.getId()).getStatus());
    }

    @Test
    void timeoutCancellationReleasesOrExpiresLockedCoupon() {
        Coupon coupon = coupon("TIMEOUT", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2);
        UserCoupon available = availableCoupon(coupon, owner, LocalDateTime.now().plusMinutes(5));
        OrderCreateVO live = createOrder(owner, shop, goods, available.getId(), "TIMEOUT_LIVE");
        orderMapper.update(null, Wrappers.<Order>lambdaUpdate().eq(Order::getId, live.getOrderId()).set(Order::getPayExpireTime, LocalDateTime.now().minusSeconds(1)));
        assertTrue(cancellationService.cancelExpiredOrder(live.getOrderId(), LocalDateTime.now()));
        assertEquals(UserCouponStatus.AVAILABLE.name(), userCouponMapper.selectById(available.getId()).getStatus());

        Coupon expiredCoupon = coupon("TIMEOUT_EXPIRED", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2);
        UserCoupon expired = availableCoupon(expiredCoupon, owner, LocalDateTime.now().plusMinutes(5));
        OrderCreateVO expiredOrder = createOrder(owner, shop, goods, expired.getId(), "TIMEOUT_EXPIRED");
        orderMapper.update(null, Wrappers.<Order>lambdaUpdate().eq(Order::getId, expiredOrder.getOrderId()).set(Order::getPayExpireTime, LocalDateTime.now().minusSeconds(1)));
        userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate().eq(UserCoupon::getId, expired.getId()).set(UserCoupon::getExpireTime, LocalDateTime.now().minusSeconds(1)));
        assertTrue(cancellationService.cancelExpiredOrder(expiredOrder.getOrderId(), LocalDateTime.now()));
        assertEquals(UserCouponStatus.EXPIRED.name(), userCouponMapper.selectById(expired.getId()).getStatus());
    }

    @Test
    void onlyOneConcurrentOrderCanLockTheSameCoupon() throws Exception {
        Coupon coupon = coupon("CONCURRENT", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2);
        UserCoupon userCoupon = availableCoupon(coupon, owner, LocalDateTime.now().plusMinutes(5));
        Goods secondGoods = goods(shop, "CONCURRENT_SECOND", "20.00", 30);
        Cart firstCart = cart(owner, shop, goods, 1); Cart secondCart = cart(owner, shop, secondGoods, 1);
        UserAddress firstAddress = address(owner, "CONCURRENT_A"); UserAddress secondAddress = address(owner, "CONCURRENT_B");
        CountDownLatch ready = new CountDownLatch(2); CountDownLatch start = new CountDownLatch(1); ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicInteger success = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<Future<?>>();
        for (final OrderCreateDTO request : Arrays.asList(request(firstCart, firstAddress, userCoupon.getId(), "CONCURRENT_A"), request(secondCart, secondAddress, userCoupon.getId(), "CONCURRENT_B"))) {
            futures.add(pool.submit(() -> { asUser(owner); ready.countDown(); start.await(5, TimeUnit.SECONDS); try { orderService.create(request); success.incrementAndGet(); } catch (BusinessException ignored) { } finally { UserContext.clear(); } return null; }));
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown(); for (Future<?> future : futures) future.get(10, TimeUnit.SECONDS); pool.shutdownNow();
        assertEquals(1, success.get());
        assertEquals(UserCouponStatus.LOCKED.name(), userCouponMapper.selectById(userCoupon.getId()).getStatus());
    }

    @Test
    void rollsBackCouponAndOrderWhenOrderWriteFails() {
        Coupon coupon = coupon("ROLLBACK", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2);
        UserCoupon userCoupon = availableCoupon(coupon, owner, LocalDateTime.now().plusMinutes(5));
        Cart cart = cart(owner, shop, goods, 1); UserAddress address = address(owner, "ROLLBACK");
        doThrow(new RuntimeException("COUPON_ORDER_TEST_ITEM_WRITE")).when(orderItemMapperSpy).insert(any(OrderItem.class));
        assertThrows(RuntimeException.class, () -> orderService.create(request(cart, address, userCoupon.getId(), "ROLLBACK")));
        assertEquals(UserCouponStatus.AVAILABLE.name(), userCouponMapper.selectById(userCoupon.getId()).getStatus());
        assertEquals(0L, orderMapper.selectCount(Wrappers.<Order>lambdaQuery().likeRight(Order::getRemark, MARKER + "ROLLBACK")).longValue());
    }

    private Coupon coupon(String suffix, LocalDateTime receiveStart, LocalDateTime receiveEnd, int stock) {
        Coupon value = new Coupon(); value.setName(MARKER + suffix); value.setCouponType("CASH"); value.setDiscountAmount(new BigDecimal("2.00")); value.setThresholdAmount(new BigDecimal("10.00")); value.setTotalStock(stock); value.setAvailableStock(stock); value.setClaimedCount(0); value.setCouponStatus("PUBLISHED"); value.setStartTime(receiveStart); value.setEndTime(LocalDateTime.now().plusMinutes(10)); value.setReceiveStartTime(receiveStart); value.setReceiveEndTime(receiveEnd); value.setUseStartTime(LocalDateTime.now().minusMinutes(2)); value.setUseEndTime(LocalDateTime.now().plusMinutes(10)); value.setShopId(shop.getId()); value.setPerUserLimit(1); value.setStatus(CouponStatus.ENABLED.name()); couponMapper.insert(value); return value;
    }
    private UserCoupon availableCoupon(Coupon coupon, User user, LocalDateTime expireTime) { UserCoupon value = new UserCoupon(); value.setUserId(user.getId()); value.setCouponId(coupon.getId()); value.setStatus(UserCouponStatus.AVAILABLE.name()); value.setReceiveTime(LocalDateTime.now()); value.setExpireTime(expireTime); userCouponMapper.insert(value); return value; }
    private UserCoupon userCoupon(Coupon coupon, User user) { return userCouponMapper.selectOne(Wrappers.<UserCoupon>lambdaQuery().eq(UserCoupon::getCouponId, coupon.getId()).eq(UserCoupon::getUserId, user.getId())); }
    private OrderCreateVO createOrder(User user, Shop selectedShop, Goods selectedGoods, Long userCouponId, String suffix) { asUser(user); Cart cart = cart(user, selectedShop, selectedGoods, 1); UserAddress address = address(user, suffix); return orderService.create(request(cart, address, userCouponId, suffix)); }
    private OrderCreateDTO request(Cart cart, UserAddress address, Long userCouponId, String suffix) { OrderCreateDTO value = new OrderCreateDTO(); value.setCartItemIds(Collections.singletonList(cart.getId())); value.setAddressId(address.getId()); value.setUserCouponId(userCouponId); value.setRemark(MARKER + suffix); return value; }
    private User user(String suffix, String phone) { User value = new User(); value.setPhone(phone); value.setNickname(MARKER + suffix); value.setProfileCompleted(1); value.setStatus(1); userMapper.insert(value); return value; }
    private Shop shop(String suffix) { Shop value = new Shop(); value.setCategoryId(1L); value.setName(MARKER + suffix); value.setAddress(MARKER + "地址"); value.setPhone("010-86660001"); value.setScore(new BigDecimal("5.00")); value.setStatus(1); value.setIsFeatured(0); value.setSortOrder(9999); shopMapper.insert(value); return value; }
    private Goods goods(Shop selectedShop, String suffix, String price, int stock) { Goods value = new Goods(); value.setShopId(selectedShop.getId()); value.setName(MARKER + suffix); value.setDescription(MARKER); value.setPrice(new BigDecimal(price)); value.setStock(stock); value.setSalesCount(0); value.setSaleStatus("ON_SALE"); goodsMapper.insert(value); return value; }
    private Cart cart(User user, Shop selectedShop, Goods selectedGoods, int quantity) { Cart value = new Cart(); value.setUserId(user.getId()); value.setShopId(selectedShop.getId()); value.setGoodsId(selectedGoods.getId()); value.setQuantity(quantity); value.setSelected(1); cartMapper.insert(value); return value; }
    private UserAddress address(User user, String suffix) { UserAddress value = new UserAddress(); value.setUserId(user.getId()); value.setReceiverName(MARKER + suffix); value.setReceiverPhone("13933339999"); value.setCampusId(campus.getId()); value.setArea(building.getArea()); value.setBuildingId(building.getId()); value.setBuildingType(building.getBuildingType()); value.setBuildingName(building.getBuildingName()); value.setFloor("3"); value.setRoomNo("301"); value.setDeliveryPoint("门口"); value.setDetail(MARKER); value.setAddressType("CAMPUS"); value.setIsDefault(0); addressMapper.insert(value); return value; }
    private void asUser(User user) { UserContext.setUser(UserDTO.fromUser(user)); }
    private void cleanup() { if (couponMapper == null) return; List<Coupon> coupons = couponMapper.selectList(Wrappers.<Coupon>lambdaQuery().likeRight(Coupon::getName, MARKER)); List<Long> couponIds = new ArrayList<Long>(); for (Coupon coupon : coupons) couponIds.add(coupon.getId()); if (!couponIds.isEmpty()) userCouponMapper.delete(Wrappers.<UserCoupon>lambdaQuery().in(UserCoupon::getCouponId, couponIds)); List<Order> orders = orderMapper.selectList(Wrappers.<Order>lambdaQuery().likeRight(Order::getRemark, MARKER)); for (Order order : orders) orderItemMapper.delete(Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, order.getId())); orderMapper.delete(Wrappers.<Order>lambdaQuery().likeRight(Order::getRemark, MARKER)); List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery().likeRight(User::getNickname, MARKER)); List<Long> userIds = new ArrayList<Long>(); for (User user : users) userIds.add(user.getId()); if (!userIds.isEmpty()) { cartMapper.delete(Wrappers.<Cart>lambdaQuery().in(Cart::getUserId, userIds)); addressMapper.delete(Wrappers.<UserAddress>lambdaQuery().in(UserAddress::getUserId, userIds)); operateLogMapper.delete(Wrappers.<OperateLog>lambdaQuery().in(OperateLog::getUserId, userIds)); } goodsMapper.delete(Wrappers.<Goods>lambdaQuery().likeRight(Goods::getName, MARKER)); shopMapper.delete(Wrappers.<Shop>lambdaQuery().likeRight(Shop::getName, MARKER)); couponMapper.delete(Wrappers.<Coupon>lambdaQuery().likeRight(Coupon::getName, MARKER)); for (User user : users) userMapper.deleteById(user.getId()); }
}
