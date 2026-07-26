package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.dto.BusinessReportQuery;
import com.qinghe.life.entity.Admin;
import com.qinghe.life.entity.Building;
import com.qinghe.life.entity.Campus;
import com.qinghe.life.entity.Coupon;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.Order;
import com.qinghe.life.entity.OrderItem;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.entity.User;
import com.qinghe.life.entity.UserAddress;
import com.qinghe.life.entity.UserCoupon;
import com.qinghe.life.enums.OrderStatus;
import com.qinghe.life.enums.UserCouponStatus;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.AdminMapper;
import com.qinghe.life.mapper.BuildingMapper;
import com.qinghe.life.mapper.CampusMapper;
import com.qinghe.life.mapper.CouponMapper;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.OrderItemMapper;
import com.qinghe.life.mapper.OrderMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.mapper.UserCouponMapper;
import com.qinghe.life.mapper.UserAddressMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.service.BusinessReportService;
import com.qinghe.life.utils.AdminContext;
import com.qinghe.life.vo.AdminInfoVO;
import com.qinghe.life.vo.BusinessTrendVO;
import com.qinghe.life.vo.GoodsSalesRankingVO;
import com.qinghe.life.vo.ShopSalesRankingVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BusinessReportIntegrationTest {
    private static final String MARKER = "BUSINESS_REPORT_TEST_";
    private static final LocalDate DAY_ONE = LocalDate.of(2098, 1, 10);
    private static final LocalDate DAY_TWO = DAY_ONE.plusDays(1);
    @Autowired private MockMvc mvc;
    @Autowired private BusinessReportService reportService;
    @Autowired private AdminMapper adminMapper;
    @Autowired private CampusMapper campusMapper;
    @Autowired private BuildingMapper buildingMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private ShopMapper shopMapper;
    @Autowired private GoodsMapper goodsMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private OrderItemMapper orderItemMapper;
    @Autowired private CouponMapper couponMapper;
    @Autowired private UserCouponMapper userCouponMapper;
    @Autowired private UserAddressMapper addressMapper;
    private Admin admin;

    @BeforeEach void setUp() { cleanup(); admin = new Admin(); admin.setUsername(MARKER + "ADMIN"); admin.setDisplayName(MARKER + "ADMIN"); admin.setPasswordHash("unused"); admin.setStatus(1); adminMapper.insert(admin); }
    @AfterEach void tearDown() { AdminContext.clear(); cleanup(); }

    @Test
    void rejectsRequestWithoutAdministratorSession() throws Exception {
        mvc.perform(get("/api/admin/reports/overview")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void returnsZerosAndEmptyListsForDateRangeWithoutData() throws Exception {
        mvc.perform(get("/api/admin/reports/trend").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        asAdmin();
        BusinessReportQuery query = query(LocalDate.of(2099, 1, 1), LocalDate.of(2099, 1, 2));
        List<BusinessTrendVO> trend = reportService.trend(query);
        assertEquals(2, trend.size()); assertEquals(0L, trend.get(0).getOrderCount().longValue()); assertEquals(BigDecimal.ZERO, trend.get(1).getSalesAmount());
        assertEquals(0, reportService.shopRanking(query).size()); assertEquals(0, reportService.goodsRanking(query).size()); assertEquals(0, reportService.couponSummary(query).size());
    }

    @Test
    void aggregatesValidStatesDiscountsBoundariesRankingsAndCouponUsage() {
        User user = user(); UserAddress address = address(user); Shop shopA = shop("A"); Shop shopB = shop("B"); Shop shopC = shop("C");
        Goods goodsA = goods(shopA, "A"); Goods goodsB = goods(shopB, "B"); Goods goodsC = goods(shopC, "C");
        Order paid = order(user, address, shopA, goodsA, DAY_ONE, OrderStatus.PAID, "100.00", "110.00", 2);
        order(user, address, shopA, goodsA, DAY_ONE, OrderStatus.PENDING_PAY, "100.00", "100.00", 1);
        order(user, address, shopA, goodsA, DAY_ONE, OrderStatus.CANCELLED, "100.00", "110.00", 1);
        order(user, address, shopB, goodsB, DAY_ONE, OrderStatus.ACCEPTED, "100.00", "100.00", 1);
        order(user, address, shopB, goodsB, DAY_ONE, OrderStatus.DELIVERING, "50.00", "55.00", 1);
        order(user, address, shopA, goodsA, DAY_ONE, OrderStatus.COMPLETED, "50.00", "60.00", 2);
        order(user, address, shopA, goodsA, DAY_TWO, OrderStatus.PAID, "200.00", "210.00", 2);
        Order tie = order(user, address, shopC, goodsC, DAY_TWO, OrderStatus.PAID, "150.00", "150.00", 1);
        Coupon coupon = coupon(shopA); UserCoupon userCoupon = new UserCoupon(); userCoupon.setUserId(user.getId()); userCoupon.setCouponId(coupon.getId()); userCoupon.setOrderId(paid.getId()); userCoupon.setStatus(UserCouponStatus.USED.name()); userCoupon.setReceiveTime(DAY_ONE.atStartOfDay()); userCoupon.setUseTime(DAY_ONE.atStartOfDay()); userCoupon.setExpireTime(DAY_TWO.plusDays(1).atStartOfDay()); userCouponMapper.insert(userCoupon);
        asAdmin(); BusinessReportQuery query = query(DAY_ONE, DAY_TWO);
        List<BusinessTrendVO> trend = reportService.trend(query);
        assertEquals(2, trend.size()); assertEquals(6L, trend.get(0).getOrderCount().longValue()); assertEquals(4L, trend.get(0).getPaidOrderCount().longValue()); assertEquals(1L, trend.get(0).getCancelledOrderCount().longValue()); assertEquals(new BigDecimal("300.00"), trend.get(0).getSalesAmount()); assertEquals(new BigDecimal("25.00"), trend.get(0).getDiscountAmount()); assertEquals(2L, trend.get(1).getOrderCount().longValue()); assertEquals(new BigDecimal("350.00"), trend.get(1).getSalesAmount());
        List<ShopSalesRankingVO> shops = reportService.shopRanking(query); assertEquals(shopA.getId(), shops.get(0).getShopId()); assertEquals(shopB.getId(), shops.get(1).getShopId()); assertEquals(shopC.getId(), shops.get(2).getShopId());
        List<GoodsSalesRankingVO> goods = reportService.goodsRanking(query); assertEquals(goodsA.getId(), goods.get(0).getGoodsId()); assertEquals(goodsB.getId(), goods.get(1).getGoodsId()); assertEquals(goodsC.getId(), goods.get(2).getGoodsId());
        assertEquals(1, reportService.couponSummary(query).size()); assertEquals(coupon.getId(), reportService.couponSummary(query).get(0).getCouponId()); assertEquals(new BigDecimal("10.00"), reportService.couponSummary(query).get(0).getDiscountAmount());
        BusinessReportQuery tooLong = query(DAY_ONE, DAY_ONE.plusDays(90)); assertThrows(BusinessException.class, () -> reportService.trend(tooLong)); assertEquals(tie.getId(), orderMapper.selectById(tie.getId()).getId());
    }

    private BusinessReportQuery query(LocalDate start, LocalDate end) { BusinessReportQuery query = new BusinessReportQuery(); query.setStartDate(start); query.setEndDate(end); query.setTop(10); return query; }
    private void asAdmin() { AdminInfoVO view = new AdminInfoVO(); view.setId(admin.getId()); view.setUsername(admin.getUsername()); view.setDisplayName(admin.getDisplayName()); AdminContext.setAdmin(view); }
    private User user() { User value = new User(); value.setPhone("138" + System.nanoTime() % 100000000L); value.setNickname(MARKER + "USER"); value.setProfileCompleted(1); value.setStatus(1); userMapper.insert(value); return value; }
    private Shop shop(String name) { Shop value = new Shop(); value.setCategoryId(1L); value.setName(MARKER + "SHOP_" + name); value.setAddress(MARKER); value.setPhone("010-86660001"); value.setScore(new BigDecimal("5.00")); value.setStatus(1); value.setIsFeatured(0); value.setSortOrder(9999); shopMapper.insert(value); return value; }
    private Goods goods(Shop shop, String name) { Goods value = new Goods(); value.setShopId(shop.getId()); value.setName(MARKER + "GOODS_" + name); value.setDescription(MARKER); value.setPrice(new BigDecimal("50.00")); value.setStock(999); value.setSalesCount(0); value.setSaleStatus("ON_SALE"); goodsMapper.insert(value); return value; }
    private UserAddress address(User user) { Campus campus = campusMapper.selectOne(Wrappers.<Campus>lambdaQuery().eq(Campus::getStatus, 1).last("LIMIT 1")); Building building = campus == null ? null : buildingMapper.selectOne(Wrappers.<Building>lambdaQuery().eq(Building::getCampusId, campus.getId()).eq(Building::getStatus, 1).last("LIMIT 1")); if (campus == null || building == null) throw new IllegalStateException("报表测试需要启用校区和楼栋"); UserAddress value = new UserAddress(); value.setUserId(user.getId()); value.setReceiverName(MARKER + "RECEIVER"); value.setReceiverPhone("13800000000"); value.setCampusId(campus.getId()); value.setArea(building.getArea()); value.setBuildingId(building.getId()); value.setBuildingType(building.getBuildingType()); value.setBuildingName(building.getBuildingName()); value.setFloor("3"); value.setRoomNo("301"); value.setDeliveryPoint("门口"); value.setDetail(MARKER); value.setAddressType("CAMPUS"); value.setIsDefault(0); addressMapper.insert(value); return value; }
    private Order order(User user, UserAddress address, Shop shop, Goods goods, LocalDate date, OrderStatus status, String payAmount, String totalAmount, int quantity) { Order value = new Order(); value.setOrderNo("QHR" + System.nanoTime()); value.setUserId(user.getId()); value.setShopId(shop.getId()); value.setAddressId(address.getId()); value.setReceiverName(MARKER + "RECEIVER"); value.setReceiverPhone("13800000000"); value.setDeliveryAddress(MARKER); value.setTotalAmount(new BigDecimal(totalAmount)); value.setDiscountAmount(new BigDecimal(totalAmount).subtract(new BigDecimal(payAmount))); value.setDeliveryFee(BigDecimal.ZERO); value.setPayAmount(new BigDecimal(payAmount)); value.setStatus(status.getCode()); value.setCreateTime(date.atTime(12, 0)); value.setPayExpireTime(date.atTime(12, 15)); value.setRemark(MARKER); orderMapper.insert(value); OrderItem item = new OrderItem(); item.setOrderId(value.getId()); item.setGoodsId(goods.getId()); item.setGoodsName(goods.getName()); item.setGoodsPrice(new BigDecimal(payAmount).divide(new BigDecimal(quantity))); item.setQuantity(quantity); item.setSubtotal(new BigDecimal(payAmount)); orderItemMapper.insert(item); return value; }
    private Coupon coupon(Shop shop) { Coupon value = new Coupon(); value.setName(MARKER + "COUPON"); value.setCouponType("CASH"); value.setDiscountAmount(new BigDecimal("10.00")); value.setThresholdAmount(BigDecimal.ZERO); value.setTotalStock(1); value.setAvailableStock(0); value.setClaimedCount(1); value.setCouponStatus("PUBLISHED"); value.setStartTime(DAY_ONE.atStartOfDay()); value.setEndTime(DAY_TWO.plusDays(1).atStartOfDay()); value.setReceiveStartTime(DAY_ONE.atStartOfDay()); value.setReceiveEndTime(DAY_TWO.plusDays(1).atStartOfDay()); value.setUseStartTime(DAY_ONE.atStartOfDay()); value.setUseEndTime(DAY_TWO.plusDays(1).atStartOfDay()); value.setShopId(shop.getId()); value.setPerUserLimit(1); value.setStatus("ENABLED"); couponMapper.insert(value); return value; }
    private void cleanup() { if (adminMapper == null) return; List<Coupon> coupons = couponMapper.selectList(Wrappers.<Coupon>lambdaQuery().likeRight(Coupon::getName, MARKER)); List<Long> couponIds = new ArrayList<Long>(); for (Coupon item : coupons) couponIds.add(item.getId()); if (!couponIds.isEmpty()) userCouponMapper.delete(Wrappers.<UserCoupon>lambdaQuery().in(UserCoupon::getCouponId, couponIds)); List<Order> orders = orderMapper.selectList(Wrappers.<Order>lambdaQuery().likeRight(Order::getRemark, MARKER)); for (Order item : orders) orderItemMapper.delete(Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, item.getId())); orderMapper.delete(Wrappers.<Order>lambdaQuery().likeRight(Order::getRemark, MARKER)); List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery().likeRight(User::getNickname, MARKER)); List<Long> userIds = new ArrayList<Long>(); for (User item : users) userIds.add(item.getId()); if (!userIds.isEmpty()) addressMapper.delete(Wrappers.<UserAddress>lambdaQuery().in(UserAddress::getUserId, userIds)); goodsMapper.delete(Wrappers.<Goods>lambdaQuery().likeRight(Goods::getName, MARKER)); shopMapper.delete(Wrappers.<Shop>lambdaQuery().likeRight(Shop::getName, MARKER)); couponMapper.delete(Wrappers.<Coupon>lambdaQuery().likeRight(Coupon::getName, MARKER)); userMapper.delete(Wrappers.<User>lambdaQuery().likeRight(User::getNickname, MARKER)); adminMapper.delete(Wrappers.<Admin>lambdaQuery().likeRight(Admin::getUsername, MARKER)); }
}
