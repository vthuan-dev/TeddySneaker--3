package com.web.application.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
    
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    
    private Integer size;
    
    private Integer quantity;
    
    private Double price;
    
    private Double subtotal;
    
    // Constructor với Product để dễ dàng tạo từ CartItem
    public OrderItem(Product product, Integer size, Integer quantity, Double price) {
        this.product = product;
        this.size = size;
        this.quantity = quantity;
        this.price = price;
        this.subtotal = price * quantity;
    }
    
    // Phương thức tính toán subtotal
    public void calculateSubtotal() {
        this.subtotal = this.price * this.quantity;
    }
}