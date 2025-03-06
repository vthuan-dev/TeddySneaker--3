package com.web.application.model.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceRequest {
    
    @NotNull(message = "Order ID không được để trống")
    private Long orderId;

    @NotNull(message = "Tổng tiền không được để trống")
    @Min(value = 0, message = "Tổng tiền phải lớn hơn 0")
    private BigDecimal subtotal;

    private BigDecimal discount;
    
    private BigDecimal tax;

    @NotNull(message = "Tổng tiền không được để trống")
    @Min(value = 0, message = "Tổng tiền phải lớn hơn 0")
    private BigDecimal total;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;

    @NotNull(message = "Trạng thái thanh toán không được để trống")
    private Integer paymentStatus; // 0: Chưa thanh toán, 1: Đã thanh toán

    private String note;
} 