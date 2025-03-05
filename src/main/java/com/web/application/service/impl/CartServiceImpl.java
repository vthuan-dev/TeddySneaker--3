package com.web.application.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.web.application.entity.Cart;
import com.web.application.entity.CartItem;
import com.web.application.entity.Product;
import com.web.application.entity.User;
import com.web.application.repository.CartRepository;
import com.web.application.service.CartService;
import com.web.application.dto.CartResponseDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Override
    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId);
    }

    @Override
    public Cart addProductToCart(Long userId, Product product, int quantity) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            cart = new Cart();
            User user = new User();
            user.setId(userId);
            cart.setUser(user);
            cart.setItems(new ArrayList<>());
        }
        
        Optional<CartItem> existingItem = cart.getItems().stream()
            .filter(item -> item.getProduct().getId().equals(product.getId()))
            .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(existingItem.get().getQuantity() + quantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            newItem.setCart(cart);
            cart.getItems().add(newItem);
        }

        // Tính toán total_price
        double totalPrice = cart.getItems().stream()
            .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
            .sum();
        cart.setTotalPrice(totalPrice);

        return cartRepository.save(cart);
    }

    @Override
    public Cart removeProductFromCart(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart != null) {
            cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
            return cartRepository.save(cart);
        }
        return null;
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart != null) {
            cart.getItems().clear();
            cartRepository.save(cart);
        }
    }

    @Override
    public CartResponseDTO getCartDetails(Long userId) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            return new CartResponseDTO(); // Trả về giỏ hàng trống
        }

        CartResponseDTO cartResponse = new CartResponseDTO();
        cartResponse.setId(cart.getId());
        cartResponse.setUserId(cart.getUser().getId());
        cartResponse.setTotalPrice(cart.getTotalPrice());
        
        // Chuyển đổi các CartItem thành CartItemDTO
        List<CartResponseDTO.CartItemDTO> itemDTOs = cart.getItems().stream()
            .map(item -> {
                CartResponseDTO.CartItemDTO itemDTO = new CartResponseDTO.CartItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setProductId(item.getProduct().getId());
                itemDTO.setQuantity(item.getQuantity());
                return itemDTO;
            })
            .collect(Collectors.toList());
        
        cartResponse.setItems(itemDTOs);
        
        return cartResponse;
    }
} 