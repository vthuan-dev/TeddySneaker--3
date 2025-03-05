package com.web.application.service;

import com.web.application.entity.Cart;
import com.web.application.entity.Product;

public interface CartService {
    Cart getCartByUserId(Long userId);
    Cart addProductToCart(Long userId, Product product, int quantity);
    Cart removeProductFromCart(Long userId, Long productId);
    void clearCart(Long userId);
} 