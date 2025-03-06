package com.web.application.entity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Table(name = "invoices") 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "total_amount")
    private Double totalAmount;
    
    @Column(name = "payment_status")
    private Integer paymentStatus; // 0: Chưa thanh toán, 1: Đã thanh toán
    
    @Column(name = "payment_method") 
    private String paymentMethod;
    
    @Column(name = "created_at")
    private Timestamp createdAt;
    
    @Column(name = "modified_at")
    private Timestamp modifiedAt;
} 