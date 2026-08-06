package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.Coupon;
import com.qinghe.life.entity.SeckillCouponOrder;
import com.qinghe.life.entity.User;
import com.qinghe.life.entity.UserCoupon;
import com.qinghe.life.enums.CouponStatus;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.CouponMapper;
import com.qinghe.life.mapper.SeckillCouponOrderMapper;
import com.qinghe.life.mapper.UserCouponMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.service.CouponSeckillService;
import com.qinghe.life.service.CouponService;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.CouponClaimVO;
import com.qinghe.life.vo.UserDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Requires the reviewed seckill_coupon_increment.sql and a reachable local Redis/MySQL. */
@SpringBootTest(properties = {"coupon.seckill.consume-enabled=false", "coupon.seckill.pending-min-idle-seconds=1", "coupon.seckill.retry-base-delay=1s"})
class CouponSeckillStreamIntegrationTest {
    private static final String MARKER = "SECKILL_ORDER_TEST_";
    @Autowired private CouponService couponService; @Autowired private CouponSeckillService seckillService;
    @Autowired private CouponMapper coupons; @Autowired private UserMapper users; @Autowired private UserCouponMapper userCoupons;
    @Autowired private SeckillCouponOrderMapper orders; @Autowired private StringRedisTemplate redis;
    private final List<Long> couponIds = new ArrayList<Long>(); private final List<Long> userIds = new ArrayList<Long>();
    @BeforeEach void before() { cleanup(); }
    @AfterEach void after() { UserContext.clear(); cleanup(); }

    @Test void luaAcceptsThenConsumerPersistsAndAcks() {
        Coupon coupon = coupon("SUCCESS", 1, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(5)); User user = user("SUCCESS", "13810002001");
        CouponClaimVO accepted = claim(user, coupon); assertNotNull(accepted.getOrderId()); assertEquals("ACCEPTED", accepted.getOrderStatus());
        seckillService.consumeNewMessages();
        assertEquals(1L, orders.selectCount(Wrappers.<SeckillCouponOrder>lambdaQuery().eq(SeckillCouponOrder::getId, accepted.getOrderId())).longValue());
        assertEquals("SUCCESS", status(user, accepted.getOrderId()));
        assertEquals(0, coupons.selectById(coupon.getId()).getAvailableStock().intValue());
    }

    @Test void luaRejectsStockDuplicateAndIllegalActivityTime() {
        User user = user("LUA", "13810002002"); Coupon empty = coupon("EMPTY", 0, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1));
        assertThrows(BusinessException.class, () -> claim(user, empty));
        Coupon future = coupon("FUTURE", 1, LocalDateTime.now().plusMinutes(1), LocalDateTime.now().plusMinutes(2));
        assertThrows(BusinessException.class, () -> claim(user, future));
        Coupon duplicate = coupon("DUPLICATE", 2, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1));
        assertNotNull(claim(user, duplicate).getOrderId()); assertThrows(BusinessException.class, () -> claim(user, duplicate));
    }

    @Test void oneUserConcurrentRequestsCreateAtMostOneDatabaseOrder() throws Exception {
        Coupon coupon = coupon("ONE_USER", 5, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(2)); User user = user("ONE_USER", "13810002003");
        ExecutorService pool = Executors.newFixedThreadPool(8); CountDownLatch ready = new CountDownLatch(8); CountDownLatch start = new CountDownLatch(1); List<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
        for (int index = 0; index < 8; index++) results.add(pool.submit(() -> { ready.countDown(); start.await(); try { return claim(user, coupon).getOrderId() != null; } catch (BusinessException expected) { return false; } }));
        assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown(); int accepted = 0; for (Future<Boolean> result : results) if (result.get(10, TimeUnit.SECONDS)) accepted++; pool.shutdownNow();
        assertEquals(1, accepted); seckillService.consumeNewMessages();
        assertEquals(1L, orders.selectCount(Wrappers.<SeckillCouponOrder>lambdaQuery().eq(SeckillCouponOrder::getUserId, user.getId()).eq(SeckillCouponOrder::getCouponId, coupon.getId())).longValue());
    }

    @Test void multipleUsersNeverOversellRedisOrMysql() throws Exception {
        Coupon coupon = coupon("MANY_USERS", 2, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(2)); List<User> participants = new ArrayList<User>(); for (int index = 0; index < 6; index++) participants.add(user("MANY_" + index, "13810002" + String.format("%03d", index)));
        ExecutorService pool = Executors.newFixedThreadPool(6); List<Future<Boolean>> results = new ArrayList<Future<Boolean>>(); for (User participant : participants) results.add(pool.submit(() -> { try { return claim(participant, coupon).getOrderId() != null; } catch (BusinessException expected) { return false; } }));
        int accepted = 0; for (Future<Boolean> result : results) if (result.get(10, TimeUnit.SECONDS)) accepted++; pool.shutdownNow(); assertEquals(2, accepted);
        seckillService.consumeNewMessages();
        assertTrue(orders.selectCount(Wrappers.<SeckillCouponOrder>lambdaQuery().eq(SeckillCouponOrder::getCouponId, coupon.getId())) <= 2L);
        assertTrue(coupons.selectById(coupon.getId()).getAvailableStock() >= 0); assertTrue(Integer.parseInt(redis.opsForValue().get(RedisKeys.seckillStock(coupon.getId()))) >= 0);
    }

    @Test void pendingMessageRetriesAfterTransactionalFailureAndDuplicateMessageIsIdempotent() throws Exception {
        Coupon coupon = coupon("PENDING", 1, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(2)); User user = user("PENDING", "13810002010"); CouponClaimVO accepted = claim(user, coupon);
        coupon.setStatus(CouponStatus.DISABLED.name()); coupons.updateById(coupon); seckillService.consumeNewMessages();
        assertEquals(0L, orders.selectCount(Wrappers.<SeckillCouponOrder>lambdaQuery().eq(SeckillCouponOrder::getId, accepted.getOrderId())).longValue());
        coupon.setStatus(CouponStatus.ENABLED.name()); coupons.updateById(coupon); Thread.sleep(1100L); seckillService.recoverPendingMessages();
        assertEquals(1L, orders.selectCount(Wrappers.<SeckillCouponOrder>lambdaQuery().eq(SeckillCouponOrder::getId, accepted.getOrderId())).longValue());
        java.util.Map<String, String> duplicate = new java.util.HashMap<String, String>(); duplicate.put("orderId", String.valueOf(accepted.getOrderId())); duplicate.put("couponId", String.valueOf(coupon.getId())); duplicate.put("userId", String.valueOf(user.getId()));
        redis.opsForStream().add(RedisKeys.seckillStream(), duplicate);
        seckillService.consumeNewMessages();
        assertEquals(1L, orders.selectCount(Wrappers.<SeckillCouponOrder>lambdaQuery().eq(SeckillCouponOrder::getId, accepted.getOrderId())).longValue());
    }

    private CouponClaimVO claim(User user, Coupon coupon) { UserContext.setUser(UserDTO.fromUser(user)); try { return couponService.claimSeckill(coupon.getId()); } finally { UserContext.clear(); } }
    private String status(User user, Long orderId) { UserContext.setUser(UserDTO.fromUser(user)); try { return seckillService.status(orderId).getStatus(); } finally { UserContext.clear(); } }
    private Coupon coupon(String suffix, int stock, LocalDateTime start, LocalDateTime end) { Coupon coupon = new Coupon(); coupon.setName(MARKER + suffix); coupon.setCouponType("CASH"); coupon.setCouponStatus(CouponSeckillService.SECKILL_COUPON_STATUS); coupon.setDiscountAmount(new BigDecimal("2.00")); coupon.setThresholdAmount(BigDecimal.ZERO); coupon.setTotalStock(stock); coupon.setAvailableStock(stock); coupon.setClaimedCount(0); coupon.setReceiveStartTime(start); coupon.setReceiveEndTime(end); coupon.setUseStartTime(start); coupon.setUseEndTime(end.plusMinutes(5)); coupon.setStartTime(start); coupon.setEndTime(end.plusMinutes(5)); coupon.setShopId(1L); coupon.setPerUserLimit(1); coupon.setStatus(CouponStatus.ENABLED.name()); coupons.insert(coupon); couponIds.add(coupon.getId()); return coupon; }
    private User user(String suffix, String phone) { User user = new User(); user.setPhone(phone); user.setNickname(MARKER + suffix); user.setProfileCompleted(1); user.setStatus(1); users.insert(user); userIds.add(user.getId()); return user; }
    private void cleanup() { for (Long couponId : new ArrayList<Long>(couponIds)) { orders.delete(Wrappers.<SeckillCouponOrder>lambdaQuery().eq(SeckillCouponOrder::getCouponId, couponId)); userCoupons.delete(Wrappers.<UserCoupon>lambdaQuery().eq(UserCoupon::getCouponId, couponId)); redis.delete(RedisKeys.seckillStock(couponId)); redis.delete(RedisKeys.seckillUsers(couponId)); redis.delete(RedisKeys.seckillMeta(couponId)); coupons.deleteById(couponId); } for (Long userId : new ArrayList<Long>(userIds)) users.deleteById(userId); couponIds.clear(); userIds.clear(); }
}
