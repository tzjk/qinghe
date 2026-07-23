package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.dto.OrderCreateDTO;
import com.qinghe.life.entity.Campus;
import com.qinghe.life.entity.Building;
import com.qinghe.life.entity.Cart;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.Order;
import com.qinghe.life.entity.OrderItem;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.entity.UserAddress;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.CampusMapper;
import com.qinghe.life.mapper.BuildingMapper;
import com.qinghe.life.mapper.CartMapper;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.OrderItemMapper;
import com.qinghe.life.mapper.OrderMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.mapper.UserAddressMapper;
import com.qinghe.life.service.OrderService;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.OrderCreateVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderServiceImpl implements OrderService {
    private static final String ON_SALE = "ON_SALE";
    private static final String PENDING_PAY = "PENDING_PAY";
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

    public OrderServiceImpl(CartMapper cartMapper, GoodsMapper goodsMapper, ShopMapper shopMapper,
                            UserAddressMapper userAddressMapper, CampusMapper campusMapper,
                            BuildingMapper buildingMapper, OrderMapper orderMapper, OrderItemMapper orderItemMapper) {
        this.cartMapper = cartMapper;
        this.goodsMapper = goodsMapper;
        this.shopMapper = shopMapper;
        this.userAddressMapper = userAddressMapper;
        this.campusMapper = campusMapper;
        this.buildingMapper = buildingMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
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
        order.setStatus(PENDING_PAY);
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

    private Long requireCurrentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return userId;
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
