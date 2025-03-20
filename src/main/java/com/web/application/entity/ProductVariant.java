package com.web.application.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "product_variant")
@IdClass(ProductVariantId.class)
public class ProductVariant {
    
    @Id
    @Column(name = "variant_id")
    private Integer variantId;

    @Id
    @Column(name = "product_id")
    private String productId;

    @Column(name = "variant_name")
    private String variantName;
    
    @Column(name = "variant_type")
    private String variantType;

    @Column(name = "quantity")
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;
} 