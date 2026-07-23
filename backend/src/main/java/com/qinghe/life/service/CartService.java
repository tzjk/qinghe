package com.qinghe.life.service;

import com.qinghe.life.dto.CartAddDTO;
import com.qinghe.life.dto.CartSelectedDTO;
import com.qinghe.life.dto.CartUpdateDTO;
import com.qinghe.life.vo.CartItemVO;
import com.qinghe.life.vo.CartSummaryVO;

public interface CartService {
    CartSummaryVO summary();
    CartItemVO add(CartAddDTO request);
    CartItemVO updateQuantity(Long id, CartUpdateDTO request);
    CartItemVO updateSelected(Long id, CartSelectedDTO request);
    void delete(Long id);
    void clear();
}
