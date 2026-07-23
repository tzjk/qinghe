package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.dto.CartAddDTO;
import com.qinghe.life.dto.CartSelectedDTO;
import com.qinghe.life.dto.CartUpdateDTO;
import com.qinghe.life.entity.Cart;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.CartMapper;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.service.CartService;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.CartItemVO;
import com.qinghe.life.vo.CartShopGroupVO;
import com.qinghe.life.vo.CartSummaryVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    private static final String ON_SALE = "ON_SALE";

    private final CartMapper cartMapper;
    private final GoodsMapper goodsMapper;
    private final ShopMapper shopMapper;

    public CartServiceImpl(CartMapper cartMapper, GoodsMapper goodsMapper, ShopMapper shopMapper) {
        this.cartMapper = cartMapper;
        this.goodsMapper = goodsMapper;
        this.shopMapper = shopMapper;
    }

    @Override
    public CartSummaryVO summary() {
        Long userId = currentUserId();
        List<Cart> carts = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId)
                .orderByAsc(Cart::getShopId)
                .orderByAsc(Cart::getId));
        CartSummaryVO summary = new CartSummaryVO();
        if (carts.isEmpty()) {
            return summary;
        }
        Map<Long, Goods> goodsById = goodsMapper.selectBatchIds(carts.stream()
                        .map(Cart::getGoodsId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Goods::getId, goods -> goods));
        Map<Long, Shop> shopsById = shopMapper.selectBatchIds(carts.stream()
                        .map(Cart::getShopId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Shop::getId, shop -> shop));
        Map<Long, CartShopGroupVO> groups = new LinkedHashMap<Long, CartShopGroupVO>();
        for (Cart cart : carts) {
            Goods goods = goodsById.get(cart.getGoodsId());
            Shop shop = shopsById.get(cart.getShopId());
            if (goods == null || shop == null) {
                continue;
            }
            CartItemVO item = CartItemVO.from(cart, goods);
            CartShopGroupVO group = groups.get(cart.getShopId());
            if (group == null) {
                group = new CartShopGroupVO(shop.getId(), shop.getName());
                groups.put(cart.getShopId(), group);
            }
            group.getItems().add(item);
            summary.setTotalCount(summary.getTotalCount() + item.getQuantity());
            if (Boolean.TRUE.equals(item.getSelected())) {
                summary.setSelectedCount(summary.getSelectedCount() + item.getQuantity());
                summary.setSelectedAmount(summary.getSelectedAmount().add(item.getSubtotal()));
            }
        }
        summary.setShopGroups(new ArrayList<CartShopGroupVO>(groups.values()));
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartItemVO add(CartAddDTO request) {
        Long userId = currentUserId();
        requireSaleableGoods(request.getGoodsId(), request.getQuantity());
        int updated = cartMapper.increaseQuantityWithinCurrentStock(userId, request.getGoodsId(), request.getQuantity());
        if (updated == 0) {
            Cart existing = findByUserAndGoods(userId, request.getGoodsId());
            if (existing != null) {
                throw new BusinessException(400, "商品库存不足或已不可销售");
            }
            try {
                int inserted = cartMapper.insertFromCurrentGoods(userId, request.getGoodsId(), request.getQuantity());
                if (inserted == 0) {
                    requireSaleableGoods(request.getGoodsId(), request.getQuantity());
                    throw new BusinessException(400, "商品库存不足");
                }
            } catch (DuplicateKeyException exception) {
                if (cartMapper.increaseQuantityWithinCurrentStock(userId, request.getGoodsId(), request.getQuantity()) == 0) {
                    throw new BusinessException(400, "商品库存不足或已不可销售");
                }
            }
        }
        return toItem(requireOwnedCartByGoods(userId, request.getGoodsId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartItemVO updateQuantity(Long id, CartUpdateDTO request) {
        Long userId = currentUserId();
        Cart cart = requireOwnedCart(id, userId);
        Goods goods = requireSaleableGoods(cart.getGoodsId(), request.getQuantity());
        if (request.getQuantity() > goods.getStock()) {
            throw new BusinessException(400, "商品库存不足");
        }
        cart.setQuantity(request.getQuantity());
        cart.setShopId(goods.getShopId());
        cartMapper.update(cart, Wrappers.<Cart>lambdaUpdate()
                .eq(Cart::getId, id).eq(Cart::getUserId, userId));
        return toItem(requireOwnedCart(id, userId));
    }

    @Override
    public CartItemVO updateSelected(Long id, CartSelectedDTO request) {
        Long userId = currentUserId();
        Cart cart = requireOwnedCart(id, userId);
        cartMapper.update(null, Wrappers.<Cart>lambdaUpdate()
                .eq(Cart::getId, id).eq(Cart::getUserId, userId)
                .set(Cart::getSelected, Boolean.TRUE.equals(request.getSelected()) ? 1 : 0));
        return toItem(requireOwnedCart(id, userId));
    }

    @Override
    public void delete(Long id) {
        Long userId = currentUserId();
        int deleted = cartMapper.delete(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getId, id).eq(Cart::getUserId, userId));
        if (deleted == 0) {
            throw new BusinessException(404, "购物车记录不存在或无权操作");
        }
    }

    @Override
    public void clear() {
        cartMapper.delete(Wrappers.<Cart>lambdaQuery().eq(Cart::getUserId, currentUserId()));
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return userId;
    }

    private Cart requireOwnedCart(Long id, Long userId) {
        Cart cart = cartMapper.selectOne(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getId, id).eq(Cart::getUserId, userId));
        if (cart == null) {
            throw new BusinessException(404, "购物车记录不存在或无权操作");
        }
        return cart;
    }

    private Cart requireOwnedCartByGoods(Long userId, Long goodsId) {
        Cart cart = findByUserAndGoods(userId, goodsId);
        if (cart == null) {
            throw new BusinessException(500, "购物车写入失败");
        }
        return cart;
    }

    private Cart findByUserAndGoods(Long userId, Long goodsId) {
        return cartMapper.selectOne(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId).eq(Cart::getGoodsId, goodsId));
    }

    private Goods requireSaleableGoods(Long goodsId, Integer quantity) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (!ON_SALE.equals(goods.getSaleStatus())) {
            throw new BusinessException(400, "商品已下架");
        }
        Shop shop = shopMapper.selectById(goods.getShopId());
        if (shop == null || !Integer.valueOf(1).equals(shop.getStatus())) {
            throw new BusinessException(400, "商铺不存在或未营业");
        }
        if (quantity > goods.getStock()) {
            throw new BusinessException(400, "商品库存不足");
        }
        return goods;
    }

    private CartItemVO toItem(Cart cart) {
        Goods goods = goodsMapper.selectById(cart.getGoodsId());
        if (goods == null) {
            throw new BusinessException(404, "商品不存在");
        }
        return CartItemVO.from(cart, goods);
    }
}
