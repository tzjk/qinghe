package com.qinghe.life.service;

/**
 * 扫描并提交已过支付期限的待支付订单。调度器只负责在持有分布式锁时调用本服务。
 */
public interface OrderTimeoutCancelService {
    int cancelExpiredOrders();
}
