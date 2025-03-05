package com.web.application.dto;

public class CartSummaryDTO {
    private int totalItems;
    private Double totalPrice;

    public CartSummaryDTO() {} // Thêm constructor mặc định

    // Constructor
    public CartSummaryDTO(int totalItems, Double totalPrice) {
        this.totalItems = totalItems;
        this.totalPrice = totalPrice;
    }

    // Getters and Setters
    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }
} 