package com.web.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Long id;
    private String productId;
    private String productName;
    private String productImg;
    private Integer quantity;
    private Integer sizeVn;
    private BigDecimal price;
    private BigDecimal subtotal;
} 