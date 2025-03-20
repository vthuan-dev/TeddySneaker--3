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
import com.web.application.service.OrderService;
import com.web.application.entity.Order;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import com.web.application.model.request.CheckoutRequestDTO;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

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
            System.out.println("Received add to cart request - Product: " + 
                             request.getProductId() + ", Size: " + request.getSize()); // Debug log
            
            // Kiểm tra xác thực
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            
            // Validate request
            if (request.getSize() == null || request.getSize() < 35 || request.getSize() > 42) {
                return ResponseEntity.badRequest().body("Invalid size");
            }
            
            if (request.getQuantity() <= 0) {
                return ResponseEntity.badRequest().body("Invalid quantity");
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUser().getId();
            
            // Lấy thông tin sản phẩm
            Product product = productService.getProductById(request.getProductId());
            if (product == null) {
                return ResponseEntity.badRequest().body("Product not found");
            }
            
            // Thêm vào giỏ hàng
            Cart cart = cartService.addProductToCart(userId, product, request.getQuantity(), request.getSize());
            
            // Trả về thông tin giỏ hàng mới
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

    @PostMapping("/api/cart/checkout")
    @ResponseBody
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequestDTO request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUser().getId();
            
            // Tạo đơn hàng từ giỏ hàng
            Order order = orderService.createOrderFromCart(userId, request);
            
            // Xóa giỏ hàng sau khi đặt hàng thành công
            cartService.clearCart(userId);
            
            return ResponseEntity.ok(new OrderCreatedResponse(order.getId(), order.getTotalPrice()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Getter
    @Setter
    public static class AddToCartRequest {
        @NotNull(message = "Product ID is required")
        private String productId;
        
        @Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;
        
        @NotNull(message = "Size is required")
        @Min(value = 35, message = "Size must be between 35 and 42")
        @Max(value = 42, message = "Size must be between 35 and 42")
        private Integer size;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class OrderCreatedResponse {
        private Long orderId;
        private Double totalAmount;
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