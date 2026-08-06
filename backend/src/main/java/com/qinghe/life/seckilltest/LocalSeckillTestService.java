package com.qinghe.life.seckilltest;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.Coupon;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.entity.User;
import com.qinghe.life.mapper.CouponMapper;
import com.qinghe.life.mapper.SeckillCouponOrderMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.mapper.UserCouponMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.service.CouponSeckillService;
import com.qinghe.life.utils.RedisIdWorker;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.vo.UserDTO;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.redisson.api.RedissonClient;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("local-seckill-test")
@ConditionalOnProperty(prefix = "seckill.test", name = "enabled", havingValue = "true")
public class LocalSeckillTestService {
    private final CouponMapper coupons; private final UserMapper users; private final UserCouponMapper userCoupons;
    private final SeckillCouponOrderMapper orders; private final ShopMapper shops; private final CouponSeckillService seckill;
    private final StringRedisTemplate redis; private final RedissonClient redisson; private final RedisIdWorker ids;
    @Value("${coupon.seckill.consumer-group:coupon-seckill-group}") private String consumerGroup;
    public LocalSeckillTestService(CouponMapper coupons, UserMapper users, UserCouponMapper userCoupons, SeckillCouponOrderMapper orders, ShopMapper shops, CouponSeckillService seckill, StringRedisTemplate redis, RedissonClient redisson, RedisIdWorker ids) {
        this.coupons=coupons; this.users=users; this.userCoupons=userCoupons; this.orders=orders; this.shops=shops; this.seckill=seckill; this.redis=redis; this.redisson=redisson; this.ids=ids;
    }
    @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> setup() {
        String runId="lst"+UUID.randomUUID().toString().replace("-", ""); Shop shop=shops.selectOne(Wrappers.<Shop>lambdaQuery().eq(Shop::getStatus, 1).last("LIMIT 1"));
        if(shop==null) throw new IllegalStateException("LOCAL_SECKILL_TEST_SHOP_UNAVAILABLE"); LocalDateTime now=LocalDateTime.now();
        Coupon single=coupon(runId+"-single",shop.getId(),now); Coupon multi=coupon(runId+"-multi",shop.getId(),now); coupons.insert(single); coupons.insert(multi);
        redis.opsForSet().add(RedisKeys.seckillTestRunCoupons(runId),String.valueOf(single.getId()),String.valueOf(multi.getId()));
        redis.opsForHash().put(RedisKeys.seckillTestRunMeta(runId),"runId",runId); redis.opsForHash().put(RedisKeys.seckillTestRunMeta(runId),"state","SETUP");
        List<Map<String,Object>> credentials=new ArrayList<Map<String,Object>>(); long base=Math.max(1000L,(System.currentTimeMillis()%100000000L)-1000L);
        for(int i=0;i<500;i++){ User user=new User(); user.setPhone("199"+String.format("%08d",base+i)); user.setUsername("lst"+base+i); user.setNickname("LOCAL_SECKILL_"+runId.substring(0,8)+"_"+i); user.setProfileCompleted(1); user.setStatus(1); users.insert(user); String token=UUID.randomUUID().toString().replace("-",""); Map<String,String> session=UserDTO.fromUser(user).toMap(); session.put("seckillTestRunId",runId); redis.opsForHash().putAll(RedisKeys.token(token),session); redis.expire(RedisKeys.token(token),RedisKeys.LOGIN_TOKEN_TTL_MINUTES,java.util.concurrent.TimeUnit.MINUTES); redis.opsForSet().add(RedisKeys.seckillTestRunUsers(runId),String.valueOf(user.getId())); redis.opsForSet().add(RedisKeys.seckillTestRunTokens(runId),token); Map<String,Object> credential=new HashMap<String,Object>(); credential.put("userId",user.getId()); credential.put("token",token); credentials.add(credential); }
        seckill.preheat(single.getId()); seckill.preheat(multi.getId()); Map<String,Object> result=new HashMap<String,Object>(); result.put("runId",runId); result.put("singleCouponId",single.getId()); result.put("multiCouponId",multi.getId()); result.put("credentials",credentials); return result;
    }
    public Map<String,Object> idCheck(String runId) { requireRun(runId); Set<Long> unique=new HashSet<Long>(); for(int i=0;i<2000;i++) unique.add(ids.nextSeckillOrderId()); Map<String,Object> value=new HashMap<String,Object>(); value.put("generated",2000); value.put("duplicates",2000-unique.size()); return value; }
    public Map<String,Object> snapshot(String runId) {
        requireRun(runId); Map<String,Object> value=new HashMap<String,Object>(); Set<String> couponIds=members(RedisKeys.seckillTestRunCoupons(runId)), streamIds=members(RedisKeys.seckillTestRunStreamIds(runId)); Map<String,Object> stocks=new HashMap<String,Object>();
        for(String raw:couponIds){Long couponId=Long.valueOf(raw); Coupon coupon=coupons.selectById(couponId); Map<String,Object> pair=new HashMap<String,Object>(); pair.put("database",coupon==null?null:coupon.getAvailableStock()); pair.put("redis",redis.opsForValue().get(RedisKeys.seckillStock(couponId))); pair.put("orders",orders.selectCount(Wrappers.<com.qinghe.life.entity.SeckillCouponOrder>lambdaQuery().eq(com.qinghe.life.entity.SeckillCouponOrder::getCouponId,couponId))); pair.put("successOrders",orders.selectCount(Wrappers.<com.qinghe.life.entity.SeckillCouponOrder>lambdaQuery().eq(com.qinghe.life.entity.SeckillCouponOrder::getCouponId,couponId).eq(com.qinghe.life.entity.SeckillCouponOrder::getStatus,"SUCCESS"))); stocks.put(raw,pair);}
        long pending=0; try{PendingMessages messages=redis.opsForStream().pending(RedisKeys.seckillStream(),consumerGroup,Range.unbounded(),10000); for(PendingMessage message:messages)if(streamIds.contains(message.getId().getValue()))pending++;}catch(Exception ignored){}
        value.put("stocks",stocks); value.put("streamMessages",streamIds.size()); value.put("acknowledged",members(RedisKeys.seckillTestRunAckIds(runId)).size()); value.put("pending",pending); value.put("dlq",members(RedisKeys.seckillTestRunDlqIds(runId)).size()); value.put("trackedOrders",members(RedisKeys.seckillTestRunOrderIds(runId)).size()); value.put("lockKeys",members(RedisKeys.seckillTestRunLockKeys(runId)).size()); return value;
    }
    public Map<String,Object> reliabilityProbe(String runId) throws Exception {
        requireRun(runId); List<Long> orderIds=longs(members(RedisKeys.seckillTestRunOrderIds(runId))), couponIds=longs(members(RedisKeys.seckillTestRunCoupons(runId))), userIds=longs(members(RedisKeys.seckillTestRunUsers(runId))); if(orderIds.isEmpty()||couponIds.isEmpty()||userIds.isEmpty())throw new IllegalStateException("LOCAL_SECKILL_TEST_ACCEPTED_ORDER_REQUIRED");
        Long orderId=orderIds.get(0), couponId=couponIds.get(0), userId=userIds.get(0); String lockKey=RedisKeys.seckillLock(couponId,userId); RLock lock=redisson.getLock(lockKey); lock.lock(); RecordId pendingId=null;
        try{pendingId=addTestMessage(runId,String.valueOf(orderId),String.valueOf(couponId),String.valueOf(userId)); if(!waitPending(pendingId.getValue(),10000L))throw new IllegalStateException("LOCAL_SECKILL_TEST_PENDING_NOT_OBSERVED");}finally{if(lock.isHeldByCurrentThread())lock.unlock();}
        Thread.sleep(31000L); seckill.recoverPendingMessages(); if(!waitAck(runId,pendingId.getValue(),10000L))throw new IllegalStateException("LOCAL_SECKILL_TEST_PENDING_RECOVERY_NOT_ACKED");
        RecordId invalidId=addTestMessage(runId,"invalid-order",String.valueOf(couponId),String.valueOf(userId)); if(!waitDlq(runId,10000L))throw new IllegalStateException("LOCAL_SECKILL_TEST_DLQ_NOT_OBSERVED"); Map<String,Object> result=new HashMap<String,Object>(); result.put("pendingRecovered",true); result.put("dlqObserved",true); result.put("pendingMessageId",pendingId.getValue()); result.put("invalidMessageId",invalidId.getValue()); return result;
    }
    @Transactional(rollbackFor = Exception.class)
    public void cleanup(String runId) {
        requireRun(runId); Set<String> couponIds=members(RedisKeys.seckillTestRunCoupons(runId)), userIds=members(RedisKeys.seckillTestRunUsers(runId)), tokens=members(RedisKeys.seckillTestRunTokens(runId)), streamIds=members(RedisKeys.seckillTestRunStreamIds(runId)), dlqIds=members(RedisKeys.seckillTestRunDlqIds(runId));
        for(String id:streamIds){ try{redis.opsForStream().acknowledge(RedisKeys.seckillStream(),consumerGroup,RecordId.of(id));}catch(Exception ignored){} redis.delete(RedisKeys.couponSeckillRetry(id)); } xdel(RedisKeys.seckillStream(),streamIds); xdel(RedisKeys.couponSeckillDlqStream(),dlqIds);
        for(String token:tokens) redis.delete(RedisKeys.token(token)); for(String key:members(RedisKeys.seckillTestRunStatusKeys(runId))) redis.delete(key); for(String key:members(RedisKeys.seckillTestRunLockKeys(runId))) try{redisson.getLock(key).forceUnlock();}catch(Exception ignored){}
        List<Long> couponLong=longs(couponIds), userLong=longs(userIds); if(!couponLong.isEmpty()){orders.delete(Wrappers.<com.qinghe.life.entity.SeckillCouponOrder>lambdaQuery().in(com.qinghe.life.entity.SeckillCouponOrder::getCouponId,couponLong)); userCoupons.delete(Wrappers.<com.qinghe.life.entity.UserCoupon>lambdaQuery().in(com.qinghe.life.entity.UserCoupon::getCouponId,couponLong)); for(Long id:couponLong){redis.delete(Arrays.asList(RedisKeys.seckillStock(id),RedisKeys.seckillUsers(id),RedisKeys.seckillMeta(id))); coupons.deleteById(id);}} if(!userLong.isEmpty()) users.deleteBatchIds(userLong);
        redis.delete(Arrays.asList(RedisKeys.seckillTestRunMeta(runId),RedisKeys.seckillTestRunCoupons(runId),RedisKeys.seckillTestRunUsers(runId),RedisKeys.seckillTestRunTokens(runId),RedisKeys.seckillTestRunStreamIds(runId),RedisKeys.seckillTestRunAckIds(runId),RedisKeys.seckillTestRunDlqIds(runId),RedisKeys.seckillTestRunOrderIds(runId),RedisKeys.seckillTestRunStatusKeys(runId),RedisKeys.seckillTestRunLockKeys(runId)));
    }
    private Coupon coupon(String name,Long shopId,LocalDateTime now){Coupon c=new Coupon();c.setName(name);c.setCouponType("CASH");c.setDiscountAmount(new BigDecimal("1.00"));c.setThresholdAmount(BigDecimal.ZERO);c.setTotalStock(100);c.setAvailableStock(100);c.setClaimedCount(0);c.setCouponStatus(CouponSeckillService.SECKILL_COUPON_STATUS);c.setReceiveStartTime(now.minusMinutes(1));c.setReceiveEndTime(now.plusHours(1));c.setUseStartTime(now);c.setUseEndTime(now.plusDays(1));c.setStartTime(now.minusMinutes(1));c.setEndTime(now.plusDays(1));c.setShopId(shopId);c.setPerUserLimit(1);c.setStatus("ENABLED");return c;}
    private void requireRun(String runId){if(runId==null||runId.trim().isEmpty()||!Boolean.TRUE.equals(redis.hasKey(RedisKeys.seckillTestRunMeta(runId))))throw new IllegalArgumentException("LOCAL_SECKILL_TEST_RUN_NOT_FOUND");}
    private Set<String> members(String key){Set<String> values=redis.opsForSet().members(key);return values==null?Collections.<String>emptySet():values;}
    private List<Long> longs(Set<String> values){List<Long> result=new ArrayList<Long>();for(String value:values)result.add(Long.valueOf(value));return result;}
    private RecordId addTestMessage(String runId,String orderId,String couponId,String userId){Map<String,String> message=new HashMap<String,String>();message.put("orderId",orderId);message.put("couponId",couponId);message.put("userId",userId);message.put("testRunId",runId);RecordId id=redis.opsForStream().add(RedisKeys.seckillStream(),message);redis.opsForSet().add(RedisKeys.seckillTestRunStreamIds(runId),id.getValue());return id;}
    private boolean waitPending(String id,long timeout)throws InterruptedException{long end=System.currentTimeMillis()+timeout;while(System.currentTimeMillis()<end){try{PendingMessages messages=redis.opsForStream().pending(RedisKeys.seckillStream(),consumerGroup,Range.unbounded(),10000);for(PendingMessage message:messages)if(id.equals(message.getId().getValue()))return true;}catch(Exception ignored){}Thread.sleep(100L);}return false;}
    private boolean waitAck(String runId,String id,long timeout)throws InterruptedException{long end=System.currentTimeMillis()+timeout;while(System.currentTimeMillis()<end){if(members(RedisKeys.seckillTestRunAckIds(runId)).contains(id))return true;Thread.sleep(100L);}return false;}
    private boolean waitDlq(String runId,long timeout)throws InterruptedException{long end=System.currentTimeMillis()+timeout;while(System.currentTimeMillis()<end){if(!members(RedisKeys.seckillTestRunDlqIds(runId)).isEmpty())return true;Thread.sleep(100L);}return false;}
    private void xdel(final String stream,Set<String> ids){for(final String id:ids)redis.execute(new RedisCallback<Long>(){public Long doInRedis(RedisConnection connection){return connection.xDel(stream.getBytes(StandardCharsets.UTF_8),id);}});}
}
