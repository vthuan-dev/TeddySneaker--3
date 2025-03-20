package com.web.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

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
    
    // Thông tin sản phẩm đầu tiên (để tương thích ngược)
    private String productName;
    private String productImg;
    private Integer sizeVn;
    
    // Danh sách tất cả các mặt hàng trong đơn hàng
    private List<OrderItemDTO> items = new ArrayList<>();
    
    public OrderDetailDTO (long id, long totalPrice, long productPrice, String receiverName, String receiverPhone, String receiverAddress, int status, int sizeVn, String productName, String productImg) {
        this.id = id;
        this.totalPrice = BigDecimal.valueOf(totalPrice);
        this.productPrice = BigDecimal.valueOf(productPrice);
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.status = status;
        this.sizeVn = sizeVn;
        this.productName = productName;
        this.productImg = productImg;
    }
}
