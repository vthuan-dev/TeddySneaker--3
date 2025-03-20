package com.web.application.model.request;

import java.util.List;
import lombok.Data;

@Data
public class CheckoutRequestDTO {
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String note;
    private String couponCode;
    private Double totalPrice;
    private List<CartItemDTO> cartItems;

    @Data
    public static class CartItemDTO {
        private String productId;
        private Integer size;
        private Integer quantity;
        private Double price;
    }
} 