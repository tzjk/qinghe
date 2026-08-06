package com.qinghe.life.vo;

import lombok.Data;

@Data
public class SeckillOrderStatusVO {
    private Long orderId;
    private String status;

    public static SeckillOrderStatusVO of(Long orderId, String status) {
        SeckillOrderStatusVO view = new SeckillOrderStatusVO();
        view.setOrderId(orderId);
        view.setStatus(status);
        return view;
    }
}
