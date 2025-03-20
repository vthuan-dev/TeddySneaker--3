package com.web.application.service;

import java.util.List;

import com.web.application.dto.CartResponseDTO;
import com.web.application.dto.CartSummaryDTO;
import com.web.application.entity.Cart;
import com.web.application.entity.CartItem;
import com.web.application.entity.Product;

public interface CartService {
    Cart getCartByUserId(Long userId);
    Cart addProductToCart(Long userId, Product product, int quantity, Integer size);
    Cart removeProductFromCart(Long userId, String productId, Integer size);
    void clearCart(Long userId);
    
    // Thêm phương thức mới
    CartResponseDTO getCartDetails(Long userId);
    
    /**
     * Lấy thông tin tổng quan về giỏ hàng của người dùng
     * @param userId ID của người dùng
     * @return CartSummaryDTO chứa tổng số lượng sản phẩm và tổng tiền
     */
    CartSummaryDTO getCartSummary(Long userId);

    Cart updateCartItemQuantity(Long userId, String productId, Integer size, int quantity);

    List<CartItem> findByUserId(Long userId);
} 