package com.web.application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequestDTO {
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String note;
    private String couponCode;
    private Double totalPrice;
} 