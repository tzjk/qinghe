package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qinghe.life.common.PageResult;
import com.qinghe.life.dto.AdminOrderQuery;
import com.qinghe.life.dto.OrderCreateDTO;
import com.qinghe.life.dto.OrderQuery;
import com.qinghe.life.entity.Campus;
import com.qinghe.life.entity.Building;
import com.qinghe.life.entity.Cart;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.Order;
import com.qinghe.life.entity.OrderItem;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.entity.UserAddress;
import com.qinghe.life.entity.User;
import com.qinghe.life.entity.OperateLog;
import com.qinghe.life.enums.OrderStatus;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.CampusMapper;
import com.qinghe.life.mapper.BuildingMapper;
import com.qinghe.life.mapper.CartMapper;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.OrderItemMapper;
import com.qinghe.life.mapper.OrderMapper;
import com.qinghe.life.mapper.OperateLogMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.mapper.UserAddressMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.service.OrderService;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.utils.AdminContext;
import com.qinghe.life.vo.AdminOrderVO;
import com.qinghe.life.vo.OrderCreateVO;
import com.qinghe.life.vo.OrderItemVO;
import com.qinghe.life.vo.OrderVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderServiceImpl implements OrderService {
    private static final String ON_SALE = "ON_SALE";
    private static final BigDecimal ZERO_AMOUNT = new BigDecimal("0.00");
    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final CartMapper cartMapper;
    private final GoodsMapper goodsMapper;
    private final ShopMapper shopMapper;
    private final UserAddressMapper userAddressMapper;
    private final CampusMapper campusMapper;
    private final BuildingMapper buildingMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final OperateLogMapper operateLogMapper;

    @Value("${order.payment-timeout-minutes:15}")
    private long paymentTimeoutMinutes;

    public OrderServiceImpl(CartMapper cartMapper, GoodsMapper goodsMapper, ShopMapper shopMapper,
                            UserAddressMapper userAddressMapper, CampusMapper campusMapper,
                            BuildingMapper buildingMapper, OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                            UserMapper userMapper, OperateLogMapper operateLogMapper) {
        this.cartMapper = cartMapper;
        this.goodsMapper = goodsMapper;
        this.shopMapper = shopMapper;
        this.userAddressMapper = userAddressMapper;
        this.campusMapper = campusMapper;
        this.buildingMapper = buildingMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.userMapper = userMapper;
        this.operateLogMapper = operateLogMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO create(OrderCreateDTO request) {
        Long userId = requireCurrentUserId();
        List<Long> cartItemIds = request.getCartItemIds();
        Set<Long> distinctIds = new HashSet<Long>(cartItemIds);
        if (distinctIds.size() != cartItemIds.size()) {
            throw new BusinessException(400, "购物车项不能重复提交");
        }
        List<Cart> carts = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId).in(Cart::getId, distinctIds));
        if (carts.size() != distinctIds.size()) {
            throw new BusinessException(404, "购物车项不存在或无权下单");
        }

        Long shopId = null;
        Shop shop = null;
        List<OrderItem> orderItems = new ArrayList<OrderItem>();
        BigDecimal totalAmount = ZERO_AMOUNT;
        for (Cart cart : carts) {
            if (cart.getQuantity() == null || cart.getQuantity() <= 0) {
                throw new BusinessException(400, "购物车商品数量必须大于0");
            }
            Goods goods = goodsMapper.selectById(cart.getGoodsId());
            if (goods == null) {
                throw new BusinessException(404, "商品不存在");
            }
            if (cart.getShopId() == null || !cart.getShopId().equals(goods.getShopId())) {
                throw new BusinessException(409, "购物车商品所属商铺已变化，请重新加入购物车");
            }
            if (!ON_SALE.equals(goods.getSaleStatus())) {
                throw new BusinessException(400, "商品已下架");
            }
            if (shopId == null) {
                shopId = goods.getShopId();
                shop = shopMapper.selectById(shopId);
                if (shop == null || !Integer.valueOf(1).equals(shop.getStatus())) {
                    throw new BusinessException(400, "商铺不存在或未营业");
                }
            } else if (!shopId.equals(goods.getShopId())) {
                throw new BusinessException(400, "一次只能提交同一商铺的商品");
            }
            if (goods.getStock() == null || goods.getStock() < cart.getQuantity()) {
                throw new BusinessException(400, "商品库存不足");
            }
            BigDecimal subtotal = goods.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
            OrderItem item = new OrderItem();
            item.setGoodsId(goods.getId());
            item.setGoodsName(goods.getName());
            item.setGoodsImage(goods.getCoverImage());
            item.setGoodsPrice(goods.getPrice());
            item.setQuantity(cart.getQuantity());
            item.setSubtotal(subtotal);
            orderItems.add(item);
        }

        UserAddress address = userAddressMapper.selectOne(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getId, request.getAddressId()).eq(UserAddress::getUserId, userId));
        if (address == null) {
            throw new BusinessException(404, "地址不存在或无权使用");
        }
        Campus campus = requireActiveCampusAddress(address);
        BigDecimal discountAmount = ZERO_AMOUNT;
        BigDecimal deliveryFee = ZERO_AMOUNT;
        BigDecimal payAmount = totalAmount.subtract(discountAmount).add(deliveryFee);
        if (payAmount.compareTo(ZERO_AMOUNT) < 0) {
            throw new BusinessException(400, "订单应付金额无效");
        }

        Order order = new Order();
        LocalDateTime now = LocalDateTime.now();
        order.setOrderNo(nextOrderNo());
        order.setUserId(userId);
        order.setShopId(shopId);
        order.setAddressId(address.getId());
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setCampusId(address.getCampusId());
        order.setCampusName(campus.getCampusName());
        order.setAddressArea(address.getArea());
        order.setBuildingId(address.getBuildingId());
        order.setBuildingType(address.getBuildingType());
        order.setBuildingName(address.getBuildingName());
        order.setFloor(address.getFloor());
        order.setRoomNo(address.getRoomNo());
        order.setDeliveryPoint(address.getDeliveryPoint());
        order.setAddressDetail(address.getDetail());
        order.setDeliveryAddress(addressSummary(order));
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setDeliveryFee(deliveryFee);
        order.setPayAmount(payAmount);
        order.setStatus(OrderStatus.PENDING_PAY.getCode());
        order.setCreateTime(now);
        order.setPayExpireTime(now.plusMinutes(paymentTimeoutMinutes));
        order.setRemark(trimToNull(request.getRemark()));
        orderMapper.insert(order);
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }
        for (OrderItem item : orderItems) {
            if (goodsMapper.decreaseStockIfSaleable(item.getGoodsId(), item.getQuantity()) != 1) {
                throw new BusinessException(400, "商品库存不足或已不可销售");
            }
        }
        for (Cart cart : carts) {
            if (cartMapper.delete(Wrappers.<Cart>lambdaQuery()
                    .eq(Cart::getId, cart.getId()).eq(Cart::getUserId, userId)) != 1) {
                throw new BusinessException(409, "购物车状态已变化，请重新结算");
            }
        }
        return toView(order, shop.getName());
    }

    @Override
    public PageResult<OrderVO> page(OrderQuery query) {
        Long userId = requireCurrentUserId();
        validateStatus(query.getStatus());
        Page<Order> page = orderMapper.selectPage(new Page<Order>(query.getPage(), query.getSize()),
                Wrappers.<Order>lambdaQuery().eq(Order::getUserId, userId)
                        .eq(query.getStatus() != null && !query.getStatus().trim().isEmpty(), Order::getStatus, query.getStatus())
                        .orderByDesc(Order::getCreateTime).orderByDesc(Order::getId));
        return new PageResult<OrderVO>(userViews(page.getRecords(), false), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public OrderVO detail(Long orderId) {
        return userViews(Collections.singletonList(requireUserOrder(orderId, requireCurrentUserId())), true).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO simulatePay(Long orderId) {
        Long userId = requireCurrentUserId();
        Order order = requireUserOrder(orderId, userId);
        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            throw new BusinessException(409, "订单当前不能模拟支付");
        }
        LocalDateTime now = LocalDateTime.now();
        if (order.getPayExpireTime() == null || now.isAfter(order.getPayExpireTime())) {
            throw new BusinessException(409, "订单支付已超时");
        }
        int updated = orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, orderId).eq(Order::getUserId, userId).eq(Order::getStatus, OrderStatus.PENDING_PAY.getCode())
                .set(Order::getStatus, OrderStatus.PAID.getCode()).set(Order::getPayTime, now));
        if (updated != 1) {
            throw new BusinessException(409, "订单状态已变化，请刷新后重试");
        }
        Order paid = orderMapper.selectById(orderId);
        writeOperationLog(userId, "用户模拟支付订单", orderId, "OrderService", "simulatePay");
        return userViews(Collections.singletonList(paid), true).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId) {
        Long userId = requireCurrentUserId();
        Order order = requireUserOrder(orderId, userId);
        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            throw new BusinessException(409, "只有待支付订单可以取消");
        }
        if (!cancelPendingOrder(orderId, userId, "USER_CANCEL", userId, "用户取消订单", "cancel")) {
            throw new BusinessException(409, "订单状态已变化，请刷新后重试");
        }
    }

    @Override
    public PageResult<AdminOrderVO> adminPage(AdminOrderQuery query) {
        requireCurrentAdminId();
        validateStatus(query.getStatus());
        String keyword = trimToNull(query.getKeyword());
        Page<Order> page = orderMapper.selectPage(new Page<Order>(query.getPage(), query.getSize()),
                Wrappers.<Order>lambdaQuery()
                        .eq(query.getStatus() != null && !query.getStatus().trim().isEmpty(), Order::getStatus, query.getStatus())
                        .like(keyword != null, Order::getOrderNo, keyword)
                        .orderByDesc(Order::getCreateTime).orderByDesc(Order::getId));
        return new PageResult<AdminOrderVO>(adminViews(page.getRecords(), false), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AdminOrderVO adminDetail(Long orderId) {
        requireCurrentAdminId();
        return adminViews(Collections.singletonList(requireOrder(orderId)), true).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminOrderVO accept(Long orderId) {
        return transitionByAdmin(orderId, OrderStatus.PAID, OrderStatus.ACCEPTED, "管理员接单", "acceptedTime", "accept");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminOrderVO deliver(Long orderId) {
        return transitionByAdmin(orderId, OrderStatus.ACCEPTED, OrderStatus.DELIVERING, "管理员开始配送", "deliveryTime", "deliver");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminOrderVO complete(Long orderId) {
        return transitionByAdmin(orderId, OrderStatus.DELIVERING, OrderStatus.COMPLETED, "管理员完成订单", "completedTime", "complete");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelExpiredOrders() {
        int cancelled = 0;
        while (true) {
            List<Order> expired = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                    .eq(Order::getStatus, OrderStatus.PENDING_PAY.getCode())
                    .le(Order::getPayExpireTime, LocalDateTime.now())
                    .orderByAsc(Order::getPayExpireTime).orderByAsc(Order::getId).last("LIMIT 100"));
            if (expired.isEmpty()) {
                return cancelled;
            }
            for (Order order : expired) {
                if (cancelPendingOrder(order.getId(), null, "PAYMENT_TIMEOUT", order.getUserId(), "订单支付超时取消", "cancelExpiredOrders")) {
                    cancelled++;
                }
            }
        }
    }

    private boolean cancelPendingOrder(Long orderId, Long expectedUserId, String reason, Long logActorId,
                                       String action, String controllerMethod) {
        LocalDateTime now = LocalDateTime.now();
        int updated = orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(expectedUserId != null, Order::getUserId, expectedUserId)
                .eq(Order::getStatus, OrderStatus.PENDING_PAY.getCode())
                .set(Order::getStatus, OrderStatus.CANCELLED.getCode())
                .set(Order::getCancelTime, now).set(Order::getCancelReason, reason));
        if (updated != 1) {
            return false;
        }
        List<OrderItem> items = orderItemMapper.selectByOrderIds(Collections.singletonList(orderId));
        if (items.isEmpty()) {
            throw new BusinessException(409, "订单明细不存在，取消已回滚");
        }
        for (OrderItem item : items) {
            if (goodsMapper.restoreStockAfterOrderCancellation(item.getGoodsId(), item.getQuantity()) != 1) {
                throw new BusinessException(409, "商品库存恢复失败，取消已回滚");
            }
        }
        writeOperationLog(logActorId, action, orderId, "OrderService", controllerMethod);
        return true;
    }

    private AdminOrderVO transitionByAdmin(Long orderId, OrderStatus expected, OrderStatus target,
                                            String action, String timeField, String controllerMethod) {
        Long adminId = requireCurrentAdminId();
        requireOrder(orderId);
        LocalDateTime now = LocalDateTime.now();
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order> update = Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, orderId).eq(Order::getStatus, expected.getCode()).set(Order::getStatus, target.getCode());
        if ("acceptedTime".equals(timeField)) {
            update.set(Order::getAcceptedTime, now);
        } else if ("deliveryTime".equals(timeField)) {
            update.set(Order::getDeliveryTime, now);
        } else {
            update.set(Order::getCompletedTime, now);
        }
        if (orderMapper.update(null, update) != 1) {
            throw new BusinessException(409, "订单状态不允许此操作，请刷新后重试");
        }
        Order changed = orderMapper.selectById(orderId);
        writeOperationLog(adminId, action, orderId, "AdminOrderController", controllerMethod);
        return adminViews(Collections.singletonList(changed), true).get(0);
    }

    private List<OrderVO> userViews(List<Order> orders, boolean detail) {
        Map<Long, String> shopNames = shopNames(orders);
        Map<Long, List<OrderItemVO>> items = itemViews(orders);
        List<OrderVO> views = new ArrayList<OrderVO>();
        for (Order order : orders) {
            OrderVO view = new OrderVO();
            view.setOrderId(order.getId());
            view.setOrderNo(order.getOrderNo());
            view.setShopName(shopNames.get(order.getShopId()));
            view.setTotalAmount(order.getTotalAmount());
            view.setDiscountAmount(order.getDiscountAmount());
            view.setDeliveryFee(order.getDeliveryFee());
            view.setPayAmount(order.getPayAmount());
            view.setStatus(order.getStatus());
            view.setStatusName(statusName(order.getStatus()));
            view.setCreateTime(order.getCreateTime());
            view.setPayExpireTime(order.getPayExpireTime());
            view.setPayTime(order.getPayTime());
            view.setCancelTime(order.getCancelTime());
            view.setCancelReason(order.getCancelReason());
            view.setAcceptedTime(order.getAcceptedTime());
            view.setDeliveryTime(order.getDeliveryTime());
            view.setCompletedTime(order.getCompletedTime());
            view.setItems(items.get(order.getId()));
            if (detail) {
                view.setDeliveryAddress(order.getDeliveryAddress());
                view.setReceiverName(order.getReceiverName());
                view.setReceiverPhone(order.getReceiverPhone());
                view.setRemark(order.getRemark());
            }
            views.add(view);
        }
        return views;
    }

    private List<AdminOrderVO> adminViews(List<Order> orders, boolean detail) {
        Map<Long, String> shopNames = shopNames(orders);
        Map<Long, List<OrderItemVO>> items = itemViews(orders);
        Map<Long, User> users = users(orders);
        List<AdminOrderVO> views = new ArrayList<AdminOrderVO>();
        for (Order order : orders) {
            User user = users.get(order.getUserId());
            AdminOrderVO view = new AdminOrderVO();
            view.setOrderId(order.getId());
            view.setOrderNo(order.getOrderNo());
            view.setShopName(shopNames.get(order.getShopId()));
            view.setUserDisplayName(user == null ? "已归档用户" : user.getNickname());
            view.setMaskedUserPhone(user == null ? null : maskPhone(user.getPhone()));
            view.setTotalAmount(order.getTotalAmount());
            view.setPayAmount(order.getPayAmount());
            view.setStatus(order.getStatus());
            view.setStatusName(statusName(order.getStatus()));
            view.setCreateTime(order.getCreateTime());
            view.setPayTime(order.getPayTime());
            view.setAcceptedTime(order.getAcceptedTime());
            view.setDeliveryTime(order.getDeliveryTime());
            view.setCompletedTime(order.getCompletedTime());
            view.setCancelTime(order.getCancelTime());
            view.setItems(items.get(order.getId()));
            if (detail) {
                view.setDeliveryAddress(order.getDeliveryAddress());
                view.setReceiverName(order.getReceiverName());
                view.setReceiverPhone(order.getReceiverPhone());
            }
            views.add(view);
        }
        return views;
    }

    private Map<Long, String> shopNames(List<Order> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> shopIds = new HashSet<Long>();
        for (Order order : orders) {
            if (order.getShopId() != null) {
                shopIds.add(order.getShopId());
            }
        }
        Map<Long, String> names = new HashMap<Long, String>();
        if (!shopIds.isEmpty()) {
            for (Shop shop : shopMapper.selectBatchIds(shopIds)) {
                names.put(shop.getId(), shop.getName());
            }
        }
        return names;
    }

    private Map<Long, User> users(List<Order> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> userIds = new HashSet<Long>();
        for (Order order : orders) {
            if (order.getUserId() != null) {
                userIds.add(order.getUserId());
            }
        }
        Map<Long, User> result = new HashMap<Long, User>();
        if (!userIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(userIds)) {
                result.put(user.getId(), user);
            }
        }
        return result;
    }

    private Map<Long, List<OrderItemVO>> itemViews(List<Order> orders) {
        Map<Long, List<OrderItemVO>> result = new HashMap<Long, List<OrderItemVO>>();
        if (orders.isEmpty()) {
            return result;
        }
        List<Long> orderIds = new ArrayList<Long>();
        for (Order order : orders) {
            orderIds.add(order.getId());
            result.put(order.getId(), new ArrayList<OrderItemVO>());
        }
        for (OrderItem item : orderItemMapper.selectByOrderIds(orderIds)) {
            OrderItemVO view = new OrderItemVO();
            view.setGoodsId(item.getGoodsId());
            view.setGoodsName(item.getGoodsName());
            view.setGoodsImage(item.getGoodsImage());
            view.setGoodsPrice(item.getGoodsPrice());
            view.setQuantity(item.getQuantity());
            view.setSubtotal(item.getSubtotal());
            result.get(item.getOrderId()).add(view);
        }
        return result;
    }

    private Order requireUserOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getId, orderId).eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(404, "订单不存在或无权访问");
        }
        return order;
    }

    private Order requireOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return order;
    }

    private void validateStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return;
        }
        try {
            OrderStatus.fromCode(status.trim());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "订单状态不合法");
        }
    }

    private String statusName(String status) {
        try {
            return OrderStatus.fromCode(status).getDisplayName();
        } catch (IllegalArgumentException exception) {
            return "未知状态";
        }
    }

    private Long requireCurrentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return userId;
    }

    private Long requireCurrentAdminId() {
        Long adminId = AdminContext.getAdminId();
        if (adminId == null) {
            throw new BusinessException(401, "管理员登录已过期");
        }
        return adminId;
    }

    private void writeOperationLog(Long actorId, String action, Long orderId, String controllerClass, String controllerMethod) {
        OperateLog log = new OperateLog();
        log.setUserId(actorId);
        log.setModule("订单");
        log.setAction(action);
        log.setControllerClass(controllerClass);
        log.setControllerMethod(controllerMethod);
        log.setRequestPath("/api/orders/" + orderId);
        log.setHttpMethod("POST");
        log.setRequestSummary("{\"orderId\":" + orderId + "}");
        log.setResponseSummary("{\"status\":\"success\"}");
        log.setSuccess(1);
        log.setDurationMs(0L);
        log.setOperateTime(LocalDateTime.now());
        if (operateLogMapper.insert(log) != 1) {
            throw new BusinessException(500, "订单操作日志写入失败，事务已回滚");
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private Campus requireActiveCampusAddress(UserAddress address) {
        if (address.getCampusId() == null) {
            throw new BusinessException(400, "当前地址不是有效校园地址");
        }
        Campus campus = campusMapper.selectById(address.getCampusId());
        if (campus == null || !Integer.valueOf(1).equals(campus.getStatus())) {
            throw new BusinessException(400, "地址所属校区不存在或未启用");
        }
        if (address.getBuildingId() == null) {
            throw new BusinessException(400, "当前地址不是有效校园地址");
        }
        Building building = buildingMapper.selectById(address.getBuildingId());
        if (building == null || !Integer.valueOf(1).equals(building.getStatus())
                || !address.getCampusId().equals(building.getCampusId())) {
            throw new BusinessException(400, "地址所属楼栋不存在、未启用或不属于当前校区");
        }
        return campus;
    }

    private String nextOrderNo() {
        return "QH" + ORDER_TIME_FORMAT.format(LocalDateTime.now())
                + ThreadLocalRandom.current().nextInt(100000, 1000000);
    }

    private OrderCreateVO toView(Order order, String shopName) {
        OrderCreateVO view = new OrderCreateVO();
        view.setOrderId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setShopId(order.getShopId());
        view.setShopName(shopName);
        view.setTotalAmount(order.getTotalAmount());
        view.setDiscountAmount(order.getDiscountAmount());
        view.setDeliveryFee(order.getDeliveryFee());
        view.setPayAmount(order.getPayAmount());
        view.setAddressSummary(addressSummary(order));
        return view;
    }

    private String addressSummary(Order order) {
        StringBuilder summary = new StringBuilder();
        append(summary, order.getCampusName());
        append(summary, order.getAddressArea());
        append(summary, order.getBuildingType());
        append(summary, order.getBuildingName());
        append(summary, order.getFloor());
        append(summary, order.getRoomNo());
        append(summary, order.getDeliveryPoint());
        return summary.toString();
    }

    private void append(StringBuilder builder, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value.trim());
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
