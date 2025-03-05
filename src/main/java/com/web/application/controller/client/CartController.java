package com.web.application.controller.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.web.application.entity.Cart;
import com.web.application.entity.Product;
import com.web.application.security.CustomUserDetails;
import com.web.application.service.CartService;
import com.web.application.service.ProductService;
import com.web.application.dto.CartResponseDTO;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @GetMapping("")
    public String viewCart(Model model) {
        // Lấy thông tin user đang đăng nhập
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new RuntimeException("User not authenticated properly");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        // Lấy thông tin giỏ hàng
        CartResponseDTO cartDetails = cartService.getCartDetails(userId);
        
        // Thêm thông tin vào model để hiển thị trong view
        model.addAttribute("cart", cartDetails);
        
        return "cart/cart"; // Trả về template cart.html
    }

    @GetMapping("/api/cart")
    @ResponseBody
    public ResponseEntity<CartResponseDTO> getCartApi() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new RuntimeException("User not authenticated properly");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        CartResponseDTO cartDetails = cartService.getCartDetails(userId);
        return ResponseEntity.ok(cartDetails);
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponseDTO> addProductToCart(@RequestParam String productId, @RequestParam int quantity) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new RuntimeException("User not authenticated properly");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();
        
        Product product = productService.getProductById(productId);
        Cart cart = cartService.addProductToCart(userId, product, quantity);

        // Chuyển đổi Cart thành CartResponseDTO
        CartResponseDTO response = new CartResponseDTO();
        response.setId(cart.getId());
        response.setUserId(cart.getUser().getId());
        response.setTotalPrice(cart.getTotalPrice());
        response.setItems(cart.getItems().stream().map(item -> {
            CartResponseDTO.CartItemDTO itemDTO = new CartResponseDTO.CartItemDTO();
            itemDTO.setId(item.getId());
            itemDTO.setProductId(item.getProduct().getId());
            itemDTO.setQuantity(item.getQuantity());
            return itemDTO;
        }).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<Cart> removeProductFromCart(@PathVariable String productId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new RuntimeException("User not authenticated properly");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();
        
        Cart cart = cartService.removeProductFromCart(userId, Long.parseLong(productId));
        return ResponseEntity.ok(cart);
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
} 