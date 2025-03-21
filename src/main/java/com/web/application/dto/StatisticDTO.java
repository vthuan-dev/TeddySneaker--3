package com.web.application.dto;

import java.util.Date;

public interface StatisticDTO {
    Date getCreatedAt();
    Integer getQuantity();
    Long getSales();
    Long getProfit();
}
