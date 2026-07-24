package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.dto.OrderCreateDTO;
import com.qinghe.life.dto.OrderQuery;
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
import com.qinghe.life.utils.AdminContext;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.AdminInfoVO;
import com.qinghe.life.vo.UserDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class OrderLifecycleIntegrationTest {
    private static final String MARKER = "ORDER_LIFECYCLE_TEST_";

    @Autowired private OrderService orderService;
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
    private User other;
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
        assertNotNull(campus, "订单生命周期测试需要启用校区");
        assertNotNull(building, "订单生命周期测试需要启用楼栋");
        owner = user("OWNER", "13911110001");
        other = user("OTHER", "13911110002");
        shop = new Shop(); shop.setCategoryId(1L); shop.setName(MARKER + "SHOP"); shop.setAddress(MARKER + "地址"); shop.setPhone("010-89990001"); shop.setScore(new BigDecimal("5.00")); shop.setStatus(1); shop.setIsFeatured(0); shop.setSortOrder(9999); shopMapper.insert(shop);
        goods = goods("GOODS", 20);
        asUser(owner);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        AdminContext.clear();
        cleanup();
    }

    @Test
    void createInitializesPendingPayAndConfiguredPaymentExpiry() {
        Cart cart = cart(owner, goods, 2);
        OrderCreateDTO request = new OrderCreateDTO(); request.setCartItemIds(Collections.singletonList(cart.getId())); request.setAddressId(address(owner, "CREATE").getId()); request.setRemark(MARKER + "CREATE");
        Long orderId = orderService.create(request).getOrderId();
        Order order = orderMapper.selectById(orderId);
        assertEquals(OrderStatus.PENDING_PAY.getCode(), order.getStatus());
        assertNotNull(order.getCreateTime());
        assertNotNull(order.getPayExpireTime());
        assertTrue(order.getPayExpireTime().isAfter(order.getCreateTime()));
    }

    @Test
    void userCanReadOnlyOwnPagedOrdersAndDetailsWithItems() {
        Order order = order(owner, OrderStatus.PENDING_PAY, LocalDateTime.now().plusMinutes(10));
        order(other, OrderStatus.PENDING_PAY, LocalDateTime.now().plusMinutes(10));
        assertEquals(1L, orderService.page(new OrderQuery()).getTotal().longValue());
        assertEquals(order.getId(), orderService.detail(order.getId()).getOrderId());
        asUser(other);
        assertThrows(BusinessException.class, () -> orderService.detail(order.getId()));
    }

    @Test
    void simulatePaymentIsConditionalAndRejectsRepeatExpiredAndCancelledOrders() {
        Order payable = order(owner, OrderStatus.PENDING_PAY, LocalDateTime.now().plusMinutes(10));
        assertEquals(OrderStatus.PAID.getCode(), orderService.simulatePay(payable.getId()).getStatus());
        assertThrows(BusinessException.class, () -> orderService.simulatePay(payable.getId()));
        Order expired = order(owner, OrderStatus.PENDING_PAY, LocalDateTime.now().minusMinutes(1));
        assertThrows(BusinessException.class, () -> orderService.simulatePay(expired.getId()));
        Order cancelled = order(owner, OrderStatus.CANCELLED, LocalDateTime.now().plusMinutes(10));
        assertThrows(BusinessException.class, () -> orderService.simulatePay(cancelled.getId()));
    }

    @Test
    void userCancellationRestoresStockExactlyOnceKeepsOrderAndDoesNotRestoreCart() {
        Order order = order(owner, OrderStatus.PENDING_PAY, LocalDateTime.now().plusMinutes(10));
        int stockBeforeCancel = goodsMapper.selectById(goods.getId()).getStock();
        orderService.cancel(order.getId());
        assertEquals(OrderStatus.CANCELLED.getCode(), orderMapper.selectById(order.getId()).getStatus());
        assertEquals(stockBeforeCancel + 2, goodsMapper.selectById(goods.getId()).getStock().intValue());
        assertEquals(1L, orderItemMapper.selectCount(Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, order.getId())).longValue());
        assertThrows(BusinessException.class, () -> orderService.cancel(order.getId()));
        assertEquals(stockBeforeCancel + 2, goodsMapper.selectById(goods.getId()).getStock().intValue());
        assertEquals(1L, operateLogMapper.selectCount(Wrappers.<OperateLog>lambdaQuery().eq(OperateLog::getAction, "用户取消订单")).longValue());
    }

    @Test
    void paidOrderCannotBeCancelledByOwnerAndOtherUserCannotCancelIt() {
        Order paid = order(owner, OrderStatus.PAID, LocalDateTime.now().plusMinutes(10));
        assertThrows(BusinessException.class, () -> orderService.cancel(paid.getId()));
        Order pending = order(owner, OrderStatus.PENDING_PAY, LocalDateTime.now().plusMinutes(10));
        asUser(other);
        assertThrows(BusinessException.class, () -> orderService.cancel(pending.getId()));
    }

    @Test
    void paymentAndCancellationRaceAllowsExactlyOneStateChange() throws Exception {
        final Order order = order(owner, OrderStatus.PENDING_PAY, LocalDateTime.now().plusMinutes(10));
        ExecutorService executor = Executors.newFixedThreadPool(2); CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> pay = executor.submit(() -> attempt(owner, start, true, order.getId()));
            Future<Boolean> cancel = executor.submit(() -> attempt(owner, start, false, order.getId()));
            start.countDown();
            assertEquals(1L, Arrays.asList(pay.get(20, TimeUnit.SECONDS), cancel.get(20, TimeUnit.SECONDS)).stream().filter(Boolean::booleanValue).count());
        } finally { executor.shutdownNow(); }
        String status = orderMapper.selectById(order.getId()).getStatus();
        assertTrue(OrderStatus.PAID.getCode().equals(status) || OrderStatus.CANCELLED.getCode().equals(status));
    }

    @Test
    void adminTransitionsAreFixedAndOrdinaryUserCannotInvokeAdminService() {
        Order order = order(owner, OrderStatus.PAID, LocalDateTime.now().plusMinutes(10));
        AdminContext.clear();
        assertThrows(BusinessException.class, () -> orderService.adminPage(new com.qinghe.life.dto.AdminOrderQuery()));
        asAdmin(owner.getId());
        assertEquals(OrderStatus.ACCEPTED.getCode(), orderService.accept(order.getId()).getStatus());
        assertEquals(OrderStatus.DELIVERING.getCode(), orderService.deliver(order.getId()).getStatus());
        assertEquals(OrderStatus.COMPLETED.getCode(), orderService.complete(order.getId()).getStatus());
        assertThrows(BusinessException.class, () -> orderService.deliver(order.getId()));
    }

    @Test
    void expiryBatchCancelsOnlyExpiredPendingOrdersAndIsIdempotent() {
        Order expired = order(owner, OrderStatus.PENDING_PAY, LocalDateTime.now().minusMinutes(1));
        Order active = order(owner, OrderStatus.PENDING_PAY, LocalDateTime.now().plusMinutes(10));
        Order paid = order(owner, OrderStatus.PAID, LocalDateTime.now().minusMinutes(1));
        int stock = goodsMapper.selectById(goods.getId()).getStock();
        assertEquals(1, orderService.cancelExpiredOrders());
        assertEquals(OrderStatus.CANCELLED.getCode(), orderMapper.selectById(expired.getId()).getStatus());
        assertEquals(OrderStatus.PENDING_PAY.getCode(), orderMapper.selectById(active.getId()).getStatus());
        assertEquals(OrderStatus.PAID.getCode(), orderMapper.selectById(paid.getId()).getStatus());
        assertEquals(stock + 2, goodsMapper.selectById(goods.getId()).getStock().intValue());
        assertEquals(0, orderService.cancelExpiredOrders());
        assertEquals(stock + 2, goodsMapper.selectById(goods.getId()).getStock().intValue());
    }

    private boolean attempt(User user, CountDownLatch start, boolean pay, Long orderId) throws Exception { start.await(); asUser(user); try { if (pay) orderService.simulatePay(orderId); else orderService.cancel(orderId); return true; } catch (BusinessException exception) { return false; } finally { UserContext.clear(); } }
    private User user(String suffix, String phone) { User user = new User(); user.setPhone(phone); user.setNickname(MARKER + suffix); user.setProfileCompleted(1); user.setStatus(1); userMapper.insert(user); return user; }
    private Goods goods(String suffix, int stock) { Goods value = new Goods(); value.setShopId(shop.getId()); value.setName(MARKER + suffix); value.setDescription(MARKER); value.setPrice(new BigDecimal("8.80")); value.setStock(stock); value.setSalesCount(0); value.setSaleStatus("ON_SALE"); goodsMapper.insert(value); return value; }
    private Cart cart(User user, Goods goods, int quantity) { Cart cart = new Cart(); cart.setUserId(user.getId()); cart.setShopId(shop.getId()); cart.setGoodsId(goods.getId()); cart.setQuantity(quantity); cart.setSelected(1); cartMapper.insert(cart); return cart; }
    private UserAddress address(User user, String suffix) { UserAddress address = new UserAddress(); address.setUserId(user.getId()); address.setReceiverName(MARKER + suffix); address.setReceiverPhone("13911119999"); address.setCampusId(campus.getId()); address.setArea(building.getArea()); address.setBuildingId(building.getId()); address.setBuildingType(building.getBuildingType()); address.setBuildingName(building.getBuildingName()); address.setFloor("3"); address.setRoomNo("301"); address.setDeliveryPoint("门口"); address.setDetail(MARKER); address.setAddressType("CAMPUS"); address.setIsDefault(0); addressMapper.insert(address); return address; }
    private Order order(User user, OrderStatus status, LocalDateTime expire) { Order order = new Order(); order.setOrderNo("QHL" + System.nanoTime()); order.setUserId(user.getId()); order.setShopId(shop.getId()); order.setAddressId(address(user, "ORDER").getId()); order.setReceiverName(MARKER + "收件人"); order.setReceiverPhone("13911119999"); order.setDeliveryAddress(MARKER + "配送地址"); order.setTotalAmount(new BigDecimal("17.60")); order.setDiscountAmount(BigDecimal.ZERO); order.setDeliveryFee(BigDecimal.ZERO); order.setPayAmount(new BigDecimal("17.60")); order.setStatus(status.getCode()); order.setCreateTime(LocalDateTime.now()); order.setPayExpireTime(expire); order.setRemark(MARKER); orderMapper.insert(order); OrderItem item = new OrderItem(); item.setOrderId(order.getId()); item.setGoodsId(goods.getId()); item.setGoodsName(goods.getName()); item.setGoodsPrice(new BigDecimal("8.80")); item.setQuantity(2); item.setSubtotal(new BigDecimal("17.60")); orderItemMapper.insert(item); goodsMapper.decreaseStockIfSaleable(goods.getId(), 2); return order; }
    private void asUser(User user) { UserContext.setUser(UserDTO.fromUser(user)); }
    private void asAdmin(Long id) { AdminInfoVO admin = new AdminInfoVO(); admin.setId(id); admin.setUsername(MARKER + "ADMIN"); admin.setDisplayName(MARKER + "ADMIN"); AdminContext.setAdmin(admin); }
    private void cleanup() { if (orderMapper == null) return; java.util.List<Order> orders = orderMapper.selectList(Wrappers.<Order>lambdaQuery().likeRight(Order::getReceiverName, MARKER)); for (Order order : orders) orderItemMapper.delete(Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, order.getId())); orderMapper.delete(Wrappers.<Order>lambdaQuery().likeRight(Order::getReceiverName, MARKER)); java.util.List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery().likeRight(User::getNickname, MARKER)); java.util.List<Long> ids = new java.util.ArrayList<Long>(); for (User user : users) ids.add(user.getId()); if (!ids.isEmpty()) { cartMapper.delete(Wrappers.<Cart>lambdaQuery().in(Cart::getUserId, ids)); addressMapper.delete(Wrappers.<UserAddress>lambdaQuery().in(UserAddress::getUserId, ids)); operateLogMapper.delete(Wrappers.<OperateLog>lambdaQuery().in(OperateLog::getUserId, ids)); } goodsMapper.delete(Wrappers.<Goods>lambdaQuery().likeRight(Goods::getName, MARKER)); shopMapper.delete(Wrappers.<Shop>lambdaQuery().likeRight(Shop::getName, MARKER)); for (User user : users) userMapper.deleteById(user.getId()); }
}
