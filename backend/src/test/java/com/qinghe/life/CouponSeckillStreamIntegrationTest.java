package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.Coupon;
import com.qinghe.life.entity.User;
import com.qinghe.life.entity.UserCoupon;
import com.qinghe.life.enums.CouponClaimStatus;
import com.qinghe.life.enums.CouponStatus;
import com.qinghe.life.mapper.CouponMapper;
import com.qinghe.life.mapper.UserCouponMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.service.CouponSeckillService;
import com.qinghe.life.service.CouponService;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.CouponClaimVO;
import com.qinghe.life.vo.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "coupon.seckill.consume-enabled=false",
        "coupon.seckill.pending-min-idle-seconds=1",
        "coupon.seckill.pending-max-retries=3"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CouponSeckillStreamIntegrationTest {
    private static final String MARKER = "COUPON_SECKILL_STREAM_TEST_";
    @Autowired private CouponService couponService;
    @Autowired private CouponSeckillService couponSeckillService;
    @Autowired private CouponMapper couponMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private UserCouponMapper userCouponMapper;
    @Autowired private StringRedisTemplate redisTemplate;
    private final List<Long> couponIds = new ArrayList<Long>();
    private final List<Long> userIds = new ArrayList<Long>();

    @BeforeEach void setUp() { cleanup(); }
    @AfterEach void tearDown() { UserContext.clear(); cleanup(); }

    @Test void acceptsClaimWritesStreamAndConsumerPersistsAndAcks() {
        Coupon coupon = seckill("SUCCESS", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(5), 1);
        User user = user("SUCCESS", "13810001001");
        couponSeckillService.preheat(coupon.getId());
        assertEquals(CouponClaimStatus.CLAIM_SUCCESS.name(), claim(user, coupon).getClaimStatus());
        assertEquals(1, records(coupon.getId()).size());
        couponSeckillService.consumeNewMessages();
        assertEquals(1L, userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery().eq(UserCoupon::getUserId, user.getId()).eq(UserCoupon::getCouponId, coupon.getId())).longValue());
        assertEquals(0, couponMapper.selectById(coupon.getId()).getAvailableStock().intValue());
    }

    @Test void rejectsNotStartedEndedAndUnheatedActivities() {
        User user = user("WINDOW", "13810001002");
        Coupon unheated = seckill("UNHEATED", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 1);
        assertEquals(CouponClaimStatus.ACTIVITY_NOT_READY.name(), claim(user, unheated).getClaimStatus());
        Coupon future = seckill("FUTURE", LocalDateTime.now().plusMinutes(1), LocalDateTime.now().plusMinutes(2), 1); couponSeckillService.preheat(future.getId());
        assertEquals(CouponClaimStatus.NOT_STARTED.name(), claim(user, future).getClaimStatus());
        Coupon ended = seckill("ENDED", LocalDateTime.now().minusMinutes(2), LocalDateTime.now().minusMinutes(1), 1); couponSeckillService.preheat(ended.getId());
        assertEquals(CouponClaimStatus.ENDED.name(), claim(user, ended).getClaimStatus());
    }

    @Test void rejectsDisabledAndOutOfStockActivities() {
        User user = user("DISABLED", "13810001003");
        Coupon disabled = seckill("DISABLED", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 1); couponSeckillService.preheat(disabled.getId()); couponSeckillService.markDisabled(disabled.getId());
        assertEquals(CouponClaimStatus.ACTIVITY_DISABLED.name(), claim(user, disabled).getClaimStatus());
        Coupon empty = seckill("EMPTY", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 1); couponSeckillService.preheat(empty.getId()); redisTemplate.opsForValue().set(RedisKeys.couponSeckillStock(empty.getId()), "0");
        assertEquals(CouponClaimStatus.OUT_OF_STOCK.name(), claim(user, empty).getClaimStatus());
    }

    @Test void duplicateClaimAndDuplicateConsumptionAreIdempotent() {
        Coupon coupon = seckill("IDEMPOTENT", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 2); User user = user("IDEMPOTENT", "13810001004"); couponSeckillService.preheat(coupon.getId());
        assertEquals(CouponClaimStatus.CLAIM_SUCCESS.name(), claim(user, coupon).getClaimStatus());
        assertEquals(CouponClaimStatus.ALREADY_CLAIMED.name(), claim(user, coupon).getClaimStatus());
        couponSeckillService.consumeNewMessages(); couponSeckillService.consumeNewMessages();
        assertEquals(1L, userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery().eq(UserCoupon::getUserId, user.getId()).eq(UserCoupon::getCouponId, coupon.getId())).longValue());
        assertEquals(1, couponMapper.selectById(coupon.getId()).getAvailableStock().intValue());
    }

    @Test void lastCouponAndConcurrentUsersDoNotOversell() throws Exception {
        Coupon coupon = seckill("CONCURRENT", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(2), 2); couponSeckillService.preheat(coupon.getId());
        List<User> users = new ArrayList<User>(); for (int index = 0; index < 6; index++) users.add(user("CONCURRENT_" + index, "13810001" + String.format("%03d", index)));
        ExecutorService pool = Executors.newFixedThreadPool(users.size()); CountDownLatch ready = new CountDownLatch(users.size()); CountDownLatch start = new CountDownLatch(1); List<Future<CouponClaimVO>> results = new ArrayList<Future<CouponClaimVO>>();
        for (User user : users) results.add(pool.submit(() -> { UserContext.setUser(UserDTO.fromUser(user)); ready.countDown(); start.await(5, TimeUnit.SECONDS); try { return couponService.claimSeckill(coupon.getId()); } finally { UserContext.clear(); } }));
        assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown(); int accepted = 0; for (Future<CouponClaimVO> result : results) if (CouponClaimStatus.CLAIM_SUCCESS.name().equals(result.get(10, TimeUnit.SECONDS).getClaimStatus())) accepted++; pool.shutdownNow();
        assertEquals(2, accepted); couponSeckillService.consumeNewMessages();
        assertEquals(2L, userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery().eq(UserCoupon::getCouponId, coupon.getId())).longValue());
        assertEquals(0, couponMapper.selectById(coupon.getId()).getAvailableStock().intValue());
    }

    @Test void failedMessageStaysPendingThenRecovers() throws Exception {
        Coupon coupon = seckill("PENDING", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(2), 1); User user = user("PENDING", "13810001005"); couponSeckillService.preheat(coupon.getId()); claim(user, coupon);
        coupon.setStatus(CouponStatus.DISABLED.name()); couponMapper.updateById(coupon); couponSeckillService.consumeNewMessages();
        assertEquals(0L, userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery().eq(UserCoupon::getCouponId, coupon.getId())).longValue());
        coupon.setStatus(CouponStatus.ENABLED.name()); couponMapper.updateById(coupon); Thread.sleep(1100L); couponSeckillService.recoverPendingMessages();
        assertEquals(1L, userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery().eq(UserCoupon::getCouponId, coupon.getId())).longValue());
    }

    @Test void ordinaryClaimRemainsMysqlBasedAndSeckillCannotUseIt() {
        User user = user("ROUTING", "13810001006"); Coupon regular = regular("REGULAR", 1); UserContext.setUser(UserDTO.fromUser(user));
        assertEquals(CouponClaimStatus.CLAIM_SUCCESS.name(), couponService.claim(regular.getId()).getClaimStatus());
        Coupon seckill = seckill("NO_BYPASS", LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), 1);
        assertThrows(RuntimeException.class, () -> couponService.claim(seckill.getId()));
    }

    private CouponClaimVO claim(User user, Coupon coupon) { UserContext.setUser(UserDTO.fromUser(user)); try { return couponService.claimSeckill(coupon.getId()); } finally { UserContext.clear(); } }
    private Coupon seckill(String suffix, LocalDateTime start, LocalDateTime end, int stock) { Coupon coupon = base(suffix, start, end, stock); coupon.setCouponStatus(CouponSeckillService.SECKILL_COUPON_STATUS); couponMapper.insert(coupon); couponIds.add(coupon.getId()); return coupon; }
    private Coupon regular(String suffix, int stock) { Coupon coupon = base(suffix, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(2), stock); coupon.setCouponStatus("PUBLISHED"); couponMapper.insert(coupon); couponIds.add(coupon.getId()); return coupon; }
    private Coupon base(String suffix, LocalDateTime start, LocalDateTime end, int stock) { Coupon coupon = new Coupon(); coupon.setName(MARKER + suffix); coupon.setCouponType("CASH"); coupon.setDiscountAmount(new BigDecimal("2.00")); coupon.setThresholdAmount(BigDecimal.ZERO); coupon.setTotalStock(stock); coupon.setAvailableStock(stock); coupon.setClaimedCount(0); coupon.setReceiveStartTime(start); coupon.setReceiveEndTime(end); coupon.setUseStartTime(start); coupon.setUseEndTime(end.plusMinutes(5)); coupon.setStartTime(start); coupon.setEndTime(end.plusMinutes(5)); coupon.setShopId(1L); coupon.setPerUserLimit(1); coupon.setStatus(CouponStatus.ENABLED.name()); return coupon; }
    private User user(String suffix, String phone) { User user = new User(); user.setPhone(phone); user.setNickname(MARKER + suffix); user.setProfileCompleted(1); user.setStatus(1); userMapper.insert(user); userIds.add(user.getId()); return user; }
    private List<MapRecord<String, Object, Object>> records(Long couponId) { List<MapRecord<String, Object, Object>> result = new ArrayList<MapRecord<String, Object, Object>>(); List<MapRecord<String, Object, Object>> all = redisTemplate.opsForStream().range(RedisKeys.couponSeckillStream(), Range.unbounded()); if (all != null) for (MapRecord<String, Object, Object> record : all) if (String.valueOf(couponId).equals(String.valueOf(record.getValue().get("couponId")))) result.add(record); return result; }
    private void cleanup() { for (Long couponId : new ArrayList<Long>(couponIds)) { for (MapRecord<String, Object, Object> record : records(couponId)) { redisTemplate.opsForStream().delete(RedisKeys.couponSeckillStream(), record.getId()); redisTemplate.opsForHash().delete(RedisKeys.couponSeckillRetry(), record.getId().getValue()); redisTemplate.opsForHash().delete(RedisKeys.couponSeckillFailure(), record.getId().getValue()); } redisTemplate.delete(RedisKeys.couponSeckillStock(couponId)); redisTemplate.delete(RedisKeys.couponSeckillUsers(couponId)); redisTemplate.delete(RedisKeys.couponSeckillMeta(couponId)); userCouponMapper.delete(Wrappers.<UserCoupon>lambdaQuery().eq(UserCoupon::getCouponId, couponId)); couponMapper.deleteById(couponId); } for (Long userId : new ArrayList<Long>(userIds)) userMapper.deleteById(userId); couponIds.clear(); userIds.clear(); }
}
