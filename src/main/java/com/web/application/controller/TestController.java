package com.web.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.web.application.entity.Order;
import com.web.application.entity.OrderItem;
import com.web.application.entity.Product;
import com.web.application.entity.User;
import com.web.application.repository.OrderRepository;
import com.web.application.repository.ProductRepository;
import com.web.application.repository.UserRepository;

import java.sql.Timestamp;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @PostMapping("/create-test-order")
    public ResponseEntity<?> createTestOrder() {
        try {
            // Tạo đơn hàng mới
            Order order = new Order();
            order.setReceiverName("Nguyễn Văn Test");
            order.setReceiverPhone("0123456789");
            order.setReceiverAddress("123 Đường Test, Hà Nội");
            order.setStatus(1); // Chờ lấy hàng
            order.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            
            // Thiết lập buyer - thay đổi ID nếu cần
            User buyer = userRepository.findById(1L).orElse(null);
            order.setBuyer(buyer);
            
            // Thêm một item vào đơn hàng - thay đổi ID sản phẩm nếu cần
            Product product = productRepository.findById("1").orElse(null);
            if (product != null) {
                OrderItem item = new OrderItem();
                item.setProduct(product);
                item.setQuantity(1);
                item.setSize(40);
                item.setPrice(Double.valueOf(product.getPrice()));
                item.calculateSubtotal();
                order.addOrderItem(item);
            }
            
            // Tính tổng tiền
            order.calculateTotalPrice();
            
            // Lưu đơn hàng
            Order savedOrder = orderRepository.save(order);
            
            return ResponseEntity.ok("Đã tạo đơn hàng test với ID: " + savedOrder.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
} 