package com.qinghe.life.service;

import com.qinghe.life.dto.OrderCreateDTO;
import com.qinghe.life.vo.OrderCreateVO;

public interface OrderService {
    OrderCreateVO create(OrderCreateDTO request);
}
