package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.dto.OrderCreateDTO;
import com.qinghe.life.entity.Building;
import com.qinghe.life.entity.Campus;
import com.qinghe.life.entity.Cart;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.OperateLog;
import com.qinghe.life.entity.Order;
import com.qinghe.life.entity.OrderItem;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.entity.User;
import com.qinghe.life.entity.UserAddress;
import com.qinghe.life.enums.OrderStatus;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.BuildingMapper;
import com.qinghe.life.mapper.CampusMapper;
import com.qinghe.life.mapper.CartMapper;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.OperateLogMapper;
import com.qinghe.life.mapper.OrderItemMapper;
import com.qinghe.life.mapper.OrderMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.mapper.UserAddressMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.service.OrderService;
import com.qinghe.life.service.OrderTimeoutCancelService;
import com.qinghe.life.service.impl.OrderCancellationService;
import com.qinghe.life.task.OrderPaymentTimeoutTask;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "order.timeout.batch-size=2",
        "order.timeout.cron=0 0 0 1 1 ?"
})
class OrderTimeoutCancelIntegrationTest {
    private static final String MARKER = "ORDER_TIMEOUT_TEST_";

    @Autowired private OrderService orderService;
    @Autowired private OrderTimeoutCancelService timeoutCancelService;
    @Autowired private OrderCancellationService cancellationService;
    @Autowired private OrderPaymentTimeoutTask timeoutTask;
    @Autowired private RedissonClient redissonClient;
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

    private User owner;
    private Shop shop;
    private Goods goods;
    private Campus campus;
    private Building building;

    @BeforeEach
    void setUp() {
        cleanup();
        campus = campusMapper.selectOne(Wrappers.<Campus>lambdaQuery().eq(Campus::getStatus, 1).last("LIMIT 1"));
        building = campus == null ? null : buildingMapper.selectOne(Wrappers.<Building>lambdaQuery()
                .eq(Building::getCampusId, campus.getId()).eq(Building::getStatus, 1).last("LIMIT 1"));
        assertNotNull(campus, "订单超时测试需要启用校区");
        assertNotNull(building, "订单超时测试需要启用楼栋");
        owner = user("OWNER", "13922220001");
        shop = shop();
        goods = goods(20);
        asUser(owner);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        cleanup();
    }

    @Test
    void expiredPendingOrderIsCancelledRestoredAndLoggedExactlyOnce() {
        Order expired = order(OrderStatus.PENDING_PAY, LocalDateTime.now().minusMinutes(1));
        int stockBefore = goodsMapper.selectById(goods.getId()).getStock();

        assertEquals(1, timeoutCancelService.cancelExpiredOrders());
        Order cancelled = orderMapper.selectById(expired.getId());
        assertEquals(OrderStatus.CANCELLED.getCode(), cancelled.getStatus());
        assertEquals("PAYMENT_TIMEOUT", cancelled.getCancelReason());
        assertNotNull(cancelled.getCancelTime());
        assertEquals(stockBefore + 2, goodsMapper.selectById(goods.getId()).getStock().intValue());
        assertEquals(1L, orderItemMapper.selectCount(Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, expired.getId())).longValue());
        assertEquals(1L, operateLogMapper.selectCount(Wrappers.<OperateLog>lambdaQuery()
                .eq(OperateLog::getUserId, owner.getId()).eq(OperateLog::getAction, "订单支付超时取消")).longValue());

        assertEquals(0, timeoutCancelService.cancelExpiredOrders());
        assertEquals(stockBefore + 2, goodsMapper.selectById(goods.getId()).getStock().intValue());
        assertEquals(1L, operateLogMapper.selectCount(Wrappers.<OperateLog>lambdaQuery()
                .eq(OperateLog::getUserId, owner.getId()).eq(OperateLog::getAction, "订单支付超时取消")).longValue());
    }

    @Test
    void activeAndPaidOrdersAreNotCancelled() {
        Order active = order(OrderStatus.PENDING_PAY, LocalDateTime.now().plusMinutes(5));
        Order paid = order(OrderStatus.PAID, LocalDateTime.now().minusMinutes(1));

        assertEquals(0, timeoutCancelService.cancelExpiredOrders());
        assertEquals(OrderStatus.PENDING_PAY.getCode(), orderMapper.selectById(active.getId()).getStatus());
        assertEquals(OrderStatus.PAID.getCode(), orderMapper.selectById(paid.getId()).getStatus());
    }

    @Test
    void timeoutCancellationDoesNotRestoreCart() {
        Cart cart = cart(2);
        OrderCreateDTO request = new OrderCreateDTO();
        request.setCartItemIds(Collections.singletonList(cart.getId()));
        request.setAddressId(address().getId());
        Long orderId = orderService.create(request).getOrderId();
        assertNull(cartMapper.selectById(cart.getId()));
        orderMapper.update(null, Wrappers.<Order>lambdaUpdate().eq(Order::getId, orderId)
                .set(Order::getPayExpireTime, LocalDateTime.now().minusMinutes(1)));

        assertEquals(1, timeoutCancelService.cancelExpiredOrders());
        assertNull(cartMapper.selectById(cart.getId()));
        assertEquals(OrderStatus.CANCELLED.getCode(), orderMapper.selectById(orderId).getStatus());
    }

    @Test
    void paymentAndTimeoutCancellationRaceAllowsOnlyOneStateUpdate() throws Exception {
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(1);
        Order target = order(OrderStatus.PENDING_PAY, expiry);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> paid = executor.submit(() -> attemptPayment(start, target.getId()));
            Future<Boolean> cancelled = executor.submit(() -> attemptTimeoutCancel(start, target.getId(), expiry));
            start.countDown();
            assertEquals(1L, Arrays.asList(paid.get(20, TimeUnit.SECONDS), cancelled.get(20, TimeUnit.SECONDS))
                    .stream().filter(Boolean::booleanValue).count());
        } finally {
            executor.shutdownNow();
        }
        String status = orderMapper.selectById(target.getId()).getStatus();
        assertTrue(OrderStatus.PAID.getCode().equals(status) || OrderStatus.CANCELLED.getCode().equals(status));
    }

    @Test
    void moreThanConfiguredBatchSizeIsProcessedInBatches() {
        for (int index = 0; index < 5; index++) {
            order(OrderStatus.PENDING_PAY, LocalDateTime.now().minusMinutes(1));
        }
        assertEquals(5, timeoutCancelService.cancelExpiredOrders());
        assertEquals(5L, orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .likeRight(Order::getReceiverName, MARKER).eq(Order::getStatus, OrderStatus.CANCELLED.getCode())).longValue());
    }

    @Test
    void taskSkipsWhenLockIsHeldAndRunsAfterRelease() throws Exception {
        Order expired = order(OrderStatus.PENDING_PAY, LocalDateTime.now().minusMinutes(1));
        RLock lock = redissonClient.getLock(RedisKeys.orderTimeoutLock());
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> holder = executor.submit(() -> {
            lock.lock(15, TimeUnit.SECONDS);
            acquired.countDown();
            release.await(20, TimeUnit.SECONDS);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            return null;
        });
        try {
            assertTrue(acquired.await(10, TimeUnit.SECONDS));
            timeoutTask.cancelExpiredPendingOrders();
            assertEquals(OrderStatus.PENDING_PAY.getCode(), orderMapper.selectById(expired.getId()).getStatus());
            release.countDown();
            holder.get(20, TimeUnit.SECONDS);
            timeoutTask.cancelExpiredPendingOrders();
            assertEquals(OrderStatus.CANCELLED.getCode(), orderMapper.selectById(expired.getId()).getStatus());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void cancellationFailureRollsBackStatusStockAndLog() {
        Order broken = order(OrderStatus.PENDING_PAY, LocalDateTime.now().minusMinutes(1));
        orderItemMapper.update(null, Wrappers.<OrderItem>lambdaUpdate().eq(OrderItem::getOrderId, broken.getId())
                .set(OrderItem::getGoodsId, Long.MAX_VALUE));
        int stockBefore = goodsMapper.selectById(goods.getId()).getStock();

        assertThrows(BusinessException.class, () -> cancellationService.cancelExpiredOrder(broken.getId(), LocalDateTime.now()));
        assertEquals(OrderStatus.PENDING_PAY.getCode(), orderMapper.selectById(broken.getId()).getStatus());
        assertEquals(stockBefore, goodsMapper.selectById(goods.getId()).getStock().intValue());
        assertEquals(0L, operateLogMapper.selectCount(Wrappers.<OperateLog>lambdaQuery()
                .eq(OperateLog::getUserId, owner.getId()).eq(OperateLog::getAction, "订单支付超时取消")).longValue());
    }

    private boolean attemptPayment(CountDownLatch start, Long orderId) throws Exception {
        start.await();
        asUser(owner);
        try {
            orderService.simulatePay(orderId);
            return true;
        } catch (BusinessException exception) {
            return false;
        } finally {
            UserContext.clear();
        }
    }

    private boolean attemptTimeoutCancel(CountDownLatch start, Long orderId, LocalDateTime cutoff) throws Exception {
        start.await();
        try {
            return cancellationService.cancelExpiredOrder(orderId, cutoff);
        } catch (BusinessException exception) {
            return false;
        }
    }

    private User user(String suffix, String phone) { User value = new User(); value.setPhone(phone); value.setNickname(MARKER + suffix); value.setProfileCompleted(1); value.setStatus(1); userMapper.insert(value); return value; }
    private Shop shop() { Shop value = new Shop(); value.setCategoryId(1L); value.setName(MARKER + "SHOP"); value.setAddress(MARKER + "地址"); value.setPhone("010-87770001"); value.setScore(new BigDecimal("5.00")); value.setStatus(1); value.setIsFeatured(0); value.setSortOrder(9999); shopMapper.insert(value); return value; }
    private Goods goods(int stock) { Goods value = new Goods(); value.setShopId(shop.getId()); value.setName(MARKER + "GOODS"); value.setDescription(MARKER); value.setPrice(new BigDecimal("8.80")); value.setStock(stock); value.setSalesCount(0); value.setSaleStatus("ON_SALE"); goodsMapper.insert(value); return value; }
    private Cart cart(int quantity) { Cart value = new Cart(); value.setUserId(owner.getId()); value.setShopId(shop.getId()); value.setGoodsId(goods.getId()); value.setQuantity(quantity); value.setSelected(1); cartMapper.insert(value); return value; }
    private UserAddress address() { UserAddress value = new UserAddress(); value.setUserId(owner.getId()); value.setReceiverName(MARKER + "收件人"); value.setReceiverPhone("13922229999"); value.setCampusId(campus.getId()); value.setArea(building.getArea()); value.setBuildingId(building.getId()); value.setBuildingType(building.getBuildingType()); value.setBuildingName(building.getBuildingName()); value.setFloor("3"); value.setRoomNo("301"); value.setDeliveryPoint("门口"); value.setDetail(MARKER); value.setAddressType("CAMPUS"); value.setIsDefault(0); addressMapper.insert(value); return value; }
    private Order order(OrderStatus status, LocalDateTime expiry) { Order value = new Order(); value.setOrderNo("QHT" + System.nanoTime()); value.setUserId(owner.getId()); value.setShopId(shop.getId()); value.setAddressId(address().getId()); value.setReceiverName(MARKER + "收件人"); value.setReceiverPhone("13922229999"); value.setDeliveryAddress(MARKER + "配送地址"); value.setTotalAmount(new BigDecimal("17.60")); value.setDiscountAmount(BigDecimal.ZERO); value.setDeliveryFee(BigDecimal.ZERO); value.setPayAmount(new BigDecimal("17.60")); value.setStatus(status.getCode()); value.setCreateTime(LocalDateTime.now()); value.setPayExpireTime(expiry); value.setRemark(MARKER); orderMapper.insert(value); OrderItem item = new OrderItem(); item.setOrderId(value.getId()); item.setGoodsId(goods.getId()); item.setGoodsName(goods.getName()); item.setGoodsPrice(new BigDecimal("8.80")); item.setQuantity(2); item.setSubtotal(new BigDecimal("17.60")); orderItemMapper.insert(item); goodsMapper.decreaseStockIfSaleable(goods.getId(), 2); return value; }
    private void asUser(User user) { UserContext.setUser(UserDTO.fromUser(user)); }
    private void cleanup() { if (orderMapper == null) return; java.util.List<Order> orders = orderMapper.selectList(Wrappers.<Order>lambdaQuery().likeRight(Order::getReceiverName, MARKER)); for (Order order : orders) orderItemMapper.delete(Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, order.getId())); orderMapper.delete(Wrappers.<Order>lambdaQuery().likeRight(Order::getReceiverName, MARKER)); java.util.List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery().likeRight(User::getNickname, MARKER)); java.util.List<Long> ids = new java.util.ArrayList<Long>(); for (User user : users) ids.add(user.getId()); if (!ids.isEmpty()) { cartMapper.delete(Wrappers.<Cart>lambdaQuery().in(Cart::getUserId, ids)); addressMapper.delete(Wrappers.<UserAddress>lambdaQuery().in(UserAddress::getUserId, ids)); operateLogMapper.delete(Wrappers.<OperateLog>lambdaQuery().in(OperateLog::getUserId, ids)); } goodsMapper.delete(Wrappers.<Goods>lambdaQuery().likeRight(Goods::getName, MARKER)); shopMapper.delete(Wrappers.<Shop>lambdaQuery().likeRight(Shop::getName, MARKER)); for (User user : users) userMapper.deleteById(user.getId()); }
}
