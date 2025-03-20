package com.web.application.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CreateVariantRequest {

    @NotEmpty(message = "Product ID không được để trống")
    @JsonProperty("product_id")
    private String productId;

    @NotNull(message = "Variant ID không được để trống")
    @JsonProperty("variant_id")
    private Integer variantId;

    @NotEmpty(message = "Tên biến thể không được để trống")
    @JsonProperty("variant_name")
    private String variantName;
    
    @NotEmpty(message = "Loại biến thể không được để trống")
    @JsonProperty("variant_type")
    private String variantType;

    @NotNull(message = "Số lượng không được để trống")
    @JsonProperty("count")
    private Integer count;
} 