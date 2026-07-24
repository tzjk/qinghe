package com.qinghe.life.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "catalog.cache")
public class CatalogCacheProperties {
    private long shopTtlMinutes;
    private long goodsTtlMinutes;
    private long listTtlMinutes;
    private long nullTtlMinutes;
    private long ttlJitterMinutes;
    private long lockWaitMillis;
    private long lockLeaseSeconds;
    private int lockMaxRetries;

    public long getShopTtlMinutes() { return shopTtlMinutes; }
    public void setShopTtlMinutes(long shopTtlMinutes) { this.shopTtlMinutes = shopTtlMinutes; }
    public long getGoodsTtlMinutes() { return goodsTtlMinutes; }
    public void setGoodsTtlMinutes(long goodsTtlMinutes) { this.goodsTtlMinutes = goodsTtlMinutes; }
    public long getListTtlMinutes() { return listTtlMinutes; }
    public void setListTtlMinutes(long listTtlMinutes) { this.listTtlMinutes = listTtlMinutes; }
    public long getNullTtlMinutes() { return nullTtlMinutes; }
    public void setNullTtlMinutes(long nullTtlMinutes) { this.nullTtlMinutes = nullTtlMinutes; }
    public long getTtlJitterMinutes() { return ttlJitterMinutes; }
    public void setTtlJitterMinutes(long ttlJitterMinutes) { this.ttlJitterMinutes = ttlJitterMinutes; }
    public long getLockWaitMillis() { return lockWaitMillis; }
    public void setLockWaitMillis(long lockWaitMillis) { this.lockWaitMillis = lockWaitMillis; }
    public long getLockLeaseSeconds() { return lockLeaseSeconds; }
    public void setLockLeaseSeconds(long lockLeaseSeconds) { this.lockLeaseSeconds = lockLeaseSeconds; }
    public int getLockMaxRetries() { return lockMaxRetries; }
    public void setLockMaxRetries(int lockMaxRetries) { this.lockMaxRetries = lockMaxRetries; }
}
