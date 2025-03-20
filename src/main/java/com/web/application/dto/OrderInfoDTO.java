package com.web.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderInfoDTO {
    private Long id;
    private Integer status;
    private BigDecimal totalPrice;
    private String createdAt;
    
    private String productName;
    private String productImg;
    private Integer sizeVn;
}