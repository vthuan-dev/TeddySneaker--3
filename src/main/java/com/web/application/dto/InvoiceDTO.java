package com.web.application.dto;

import lombok.*;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDTO {
    private Long id;
    private Long orderId;
    private String customerName;
    private Double totalAmount;
    private Integer paymentStatus;
    private String paymentMethod;
    private Timestamp createdAt;
} 