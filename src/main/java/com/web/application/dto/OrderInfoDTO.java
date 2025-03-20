package com.web.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
    
    private Integer totalItems = 1;
    
    private List<OrderItemDTO> items = new ArrayList<>();
    
    public OrderInfoDTO(Long id, BigDecimal totalPrice, Integer sizeVn, 
                        String productName, String productImg) {
        this.id = id;
        this.totalPrice = totalPrice;
        this.sizeVn = sizeVn;
        this.productName = productName;
        this.productImg = productImg;
        this.totalItems = 1;
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }
}