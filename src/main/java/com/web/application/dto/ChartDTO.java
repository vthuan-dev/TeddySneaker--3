package com.web.application.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ChartDTO {
    private String label;
    private int value;

    public ChartDTO(String label, int value) {
        this.label = label;
        this.value = value;
    }
}
