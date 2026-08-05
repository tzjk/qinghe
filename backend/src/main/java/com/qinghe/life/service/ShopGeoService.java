package com.qinghe.life.service;

import com.qinghe.life.entity.Shop;
import com.qinghe.life.utils.RedisKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class ShopGeoService {
    private static final Logger log = LoggerFactory.getLogger(ShopGeoService.class);
    private final StringRedisTemplate redisTemplate;
    public ShopGeoService(StringRedisTemplate redisTemplate) { this.redisTemplate = redisTemplate; }
    public void syncAfterCommit(final Shop shop) {
        if (shop == null || shop.getId() == null) return;
        Runnable work = new Runnable() { @Override public void run() { try { String id = String.valueOf(shop.getId()); if (!Integer.valueOf(1).equals(shop.getStatus()) || shop.getLongitude() == null || shop.getLatitude() == null) redisTemplate.opsForGeo().remove(RedisKeys.shopGeo(), id); else redisTemplate.opsForGeo().add(RedisKeys.shopGeo(), new Point(shop.getLongitude().doubleValue(), shop.getLatitude().doubleValue()), id); } catch (Exception e) { log.warn("同步附近店铺 GEO 失败，shopId={}, type={}", shop.getId(), e.getClass().getSimpleName()); } } };
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() { @Override public void afterCommit() { work.run(); } }); else work.run();
    }
}
