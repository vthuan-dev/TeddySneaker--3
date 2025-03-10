package com.web.application.controller.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.web.application.entity.Cart;
import com.web.application.entity.Product;
import com.web.application.security.CustomUserDetails;
import com.web.application.service.CartService;
import com.web.application.service.ProductService;
import com.web.application.dto.CartResponseDTO;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import com.web.application.dto.CartSummaryDTO;
import org.springframework.ui.Model;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @GetMapping("/cart")
    public String viewCart(Model model) {
        return "cart/cart";
    }

    @GetMapping("/api/cart")
    @ResponseBody
    public ResponseEntity<CartResponseDTO> getCartApi() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUser().getId();

            CartResponseDTO cartDetails = cartService.getCartDetails(userId);
            if (cartDetails == null) {
                cartDetails = new CartResponseDTO(); // Return empty cart instead of null
            }
            return ResponseEntity.ok(cartDetails);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponseDTO> addProductToCart(
            @RequestParam String productId, 
            @RequestParam int quantity,
            @RequestParam(required = false) Integer size) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
                throw new RuntimeException("User not authenticated properly");
            }
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUser().getId();
            
            Product product = productService.getProductById(productId);
            Cart cart = cartService.addProductToCart(userId, product, quantity, size);

            // Chuyển đổi Cart thành CartResponseDTO
            CartResponseDTO response = new CartResponseDTO();
            response.setId(cart.getId());
            response.setUserId(cart.getUser().getId());
            response.setTotalPrice(cart.getTotalPrice());
            response.setItems(cart.getItems().stream().map(item -> {
                CartResponseDTO.CartItemDTO itemDTO = new CartResponseDTO.CartItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setProductId(item.getProduct().getId());
                itemDTO.setProductName(item.getProduct().getName());
                itemDTO.setPrice(item.getPrice());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setSize(item.getSize());
                if (!item.getProduct().getImages().isEmpty()) {
                    itemDTO.setProductImage(item.getProduct().getImages().get(0));
                }
                return itemDTO;
            }).collect(Collectors.toList()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new RuntimeException("User not authenticated properly");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();
        
        cartService.clearCart(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/cart/summary")
    @ResponseBody
    public ResponseEntity<CartSummaryDTO> getCartSummary() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUser().getId();

            CartSummaryDTO summary = cartService.getCartSummary(userId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/api/cart/remove/{productId}")
    @ResponseBody
    public ResponseEntity<?> removeProductFromCart(@PathVariable String productId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUser().getId();
            
            // Xóa sản phẩm khỏi giỏ hàng - passing productId as String directly
            cartService.removeProductFromCart(userId, productId);
            
            // Lấy thông tin giỏ hàng mới sau khi xóa
            CartSummaryDTO summary = cartService.getCartSummary(userId);
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body("Error removing product from cart: " + e.getMessage());
        }
    }

    @PostMapping("/api/cart/add")
    @ResponseBody
    public ResponseEntity<?> addToCart(@RequestBody AddToCartRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUser().getId();
            
            Product product = productService.getProductById(request.getProductId());
            Cart cart = cartService.addProductToCart(userId, product, request.getQuantity(), request.getSize());
            
            // Convert to DTO and return
            CartSummaryDTO summary = cartService.getCartSummary(userId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body("Error adding product to cart: " + e.getMessage());
        }
    }

    @PostMapping("/api/cart/update")
    @ResponseBody
    public ResponseEntity<?> updateCartItem(@RequestBody UpdateCartRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUser().getId();
            
            Cart cart = cartService.updateCartItemQuantity(userId, request.getProductId(), 
                                                         request.getSize(), request.getQuantity());
            
            CartSummaryDTO summary = cartService.getCartSummary(userId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body("Error updating cart: " + e.getMessage());
        }
    }

    // Add this class inside CartController or as a separate file
    public static class AddToCartRequest {
        private String productId;
        private int quantity;
        private Integer size;

        // Getters and setters
        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }
    }

    // Add this class inside CartController
    public static class UpdateCartRequest {
        private String productId;
        private Integer size;
        private int quantity;

        // Getters and setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
} 