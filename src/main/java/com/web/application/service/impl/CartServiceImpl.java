package com.web.application.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.web.application.entity.Cart;
import com.web.application.entity.CartItem;
import com.web.application.entity.Product;
import com.web.application.entity.User;
import com.web.application.repository.CartRepository;
import com.web.application.service.CartService;
import com.web.application.dto.CartResponseDTO;
import com.web.application.dto.CartSummaryDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Override
    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId);
    }

    @Override
    public Cart addProductToCart(Long userId, Product product, int quantity, Integer size) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            cart = new Cart();
            User user = new User();
            user.setId(userId);
            cart.setUser(user);
            cart.setItems(new ArrayList<>());
            cart.setTotalPrice(0.0);
        }
        
        Optional<CartItem> existingItem = cart.getItems().stream()
            .filter(item -> item.getProduct().getId().equals(product.getId()) 
                       && Objects.equals(item.getSize(), size))
            .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            item.setPrice(Double.valueOf(product.getPrice()));
        } else {
            CartItem newItem = new CartItem();
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            newItem.setCart(cart);
            newItem.setPrice(Double.valueOf(product.getPrice()));
            newItem.setSize(size);
            cart.getItems().add(newItem);
        }

        // Tính toán total_price sử dụng giá từ CartItem
        double totalPrice = cart.getItems().stream()
            .mapToDouble(item -> {
                Double itemPrice = item.getPrice();
                return (itemPrice != null ? itemPrice : 0.0) * item.getQuantity();
            })
            .sum();
        cart.setTotalPrice(totalPrice);

        return cartRepository.save(cart);
    }

    @Override
    public Cart removeProductFromCart(Long userId, String productId) {
        Cart cart = getCartByUserId(userId);
        if (cart != null) {
            cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
            
            // Cập nhật lại tổng giá sau khi xóa sản phẩm
            double totalPrice = cart.getItems().stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();
            cart.setTotalPrice(totalPrice);
            
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
            return new CartResponseDTO();
        }

        CartResponseDTO cartResponse = new CartResponseDTO();
        cartResponse.setId(cart.getId());
        cartResponse.setUserId(cart.getUser().getId());
        cartResponse.setTotalPrice(cart.getTotalPrice());
        
        List<CartResponseDTO.CartItemDTO> itemDTOs = cart.getItems().stream()
            .map(item -> {
                CartResponseDTO.CartItemDTO itemDTO = new CartResponseDTO.CartItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setProductId(item.getProduct().getId());
                itemDTO.setProductName(item.getProduct().getName());
                itemDTO.setPrice(Double.valueOf(item.getProduct().getPrice()));
                itemDTO.setProductImage(item.getProduct().getImages().get(0));
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setSize(item.getSize());
                return itemDTO;
            })
            .collect(Collectors.toList());
        
        cartResponse.setItems(itemDTOs);
        
        return cartResponse;
    }

    @Override
    public CartSummaryDTO getCartSummary(Long userId) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null || cart.getItems().isEmpty()) {
            return new CartSummaryDTO(0, 0.0);
        }
        
        // Tính tổng số lượng sản phẩm
        int totalItems = cart.getItems().stream()
            .mapToInt(CartItem::getQuantity)
            .sum();
        
        // Tính tổng tiền (sử dụng giá từ CartItem thay vì Product)
        double totalPrice = cart.getItems().stream()
            .mapToDouble(item -> item.getPrice() * item.getQuantity())
            .sum();
        
        return new CartSummaryDTO(totalItems, totalPrice);
    }

    @Override
    public Cart updateCartItemQuantity(Long userId, String productId, Integer size, int quantity) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart != null) {
            Optional<CartItem> itemToUpdate = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId) 
                           && Objects.equals(item.getSize(), size))
                .findFirst();
                
            if (itemToUpdate.isPresent()) {
                CartItem item = itemToUpdate.get();
                item.setQuantity(quantity);
                
                // Cập nhật lại tổng giá
                double totalPrice = cart.getItems().stream()
                    .mapToDouble(i -> i.getPrice() * i.getQuantity())
                    .sum();
                cart.setTotalPrice(totalPrice);
                
                return cartRepository.save(cart);
            }
        }
        return cart;
    }
} 