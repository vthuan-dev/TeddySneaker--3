package com.web.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderDetailDTO {
    private Long id;
    private Integer status;
    private String statusText;
    private BigDecimal totalPrice;
    private BigDecimal productPrice;
    private String createdAt;
    
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    
    private String productName;
    private String productImg;
    private Integer sizeVn;

    public OrderDetailDTO (long id, long totalPrice, long productPrice, String receiverName, String receiverPhone, String receiverAddress, int status, int sizeVn, String productName, String productImg) {
        this.id = id;
        this.totalPrice = BigDecimal.valueOf(totalPrice);
        this.status = status;
        this.sizeVn = sizeVn;
        this.productName = productName;
        this.productImg = productImg;
    }
}
