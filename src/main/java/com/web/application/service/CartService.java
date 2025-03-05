package com.web.application.service;

import com.web.application.dto.CartResponseDTO;
import com.web.application.dto.CartSummaryDTO;
import com.web.application.entity.Cart;
import com.web.application.entity.Product;

public interface CartService {
    Cart getCartByUserId(Long userId);
    Cart addProductToCart(Long userId, Product product, int quantity);
    Cart removeProductFromCart(Long userId, Long productId);
    void clearCart(Long userId);
    
    // Thêm phương thức mới
    CartResponseDTO getCartDetails(Long userId);
    
    CartSummaryDTO getCartSummary(Long userId);
} 