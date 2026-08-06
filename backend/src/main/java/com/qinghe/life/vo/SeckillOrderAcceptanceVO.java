package com.qinghe.life.vo;

import lombok.Data;

@Data
public class SeckillOrderAcceptanceVO {
    private Long orderId;
    private String status;

    public static SeckillOrderAcceptanceVO accepted(Long orderId) {
        SeckillOrderAcceptanceVO view = new SeckillOrderAcceptanceVO();
        view.setOrderId(orderId);
        view.setStatus("ACCEPTED");
        return view;
    }
}
