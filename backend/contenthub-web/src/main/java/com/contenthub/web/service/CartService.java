package com.contenthub.web.service;

import com.contenthub.common.domain.dos.CartDO;
import java.util.List;

public interface CartService {
    List<CartDO> getCartByUserId(Integer userId);
    List<CartDO> getCart();

    int addToCart(Integer productId,int quantity);

    int deleteCartItem(Integer productId);

}
