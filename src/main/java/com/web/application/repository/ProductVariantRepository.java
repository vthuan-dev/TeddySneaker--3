package com.web.application.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.web.application.entity.ProductVariant;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    // Lấy biến thể của sản phẩm
    @Query(nativeQuery = true, value = "SELECT pv.variant_id FROM product_variant pv WHERE pv.product_id = ?1 AND pv.quantity > 0")
    List<Integer> findAllVariantsOfProduct(String id);

    List<ProductVariant> findByProductId(String productId);

    @Query("SELECT COALESCE(SUM(pv.quantity), 0) FROM ProductVariant pv WHERE pv.productId = :productId")
    public long getTotalQuantityByProductId(@Param("productId") String productId);

    // Kiểm tra biến thể sản phẩm
    @Query(nativeQuery = true, value = "SELECT * FROM product_variant WHERE product_id = ?1 AND variant_id = ?2 AND quantity > 0")
    ProductVariant checkProductAndVariantAvailable(String productId, Integer variantId);

    // Trừ 1 sản phẩm theo biến thể
    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "UPDATE product_variant SET quantity = quantity - 1 WHERE product_id = ?1 AND variant_id = ?2")
    void minusOneProductByVariant(String productId, Integer variantId);

    // Cộng 1 sản phẩm theo biến thể
    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "Update product_variant set quantity = quantity + 1 where product_id = ?1 and variant_id = ?2")
    public void plusOneProductByVariant(String id, int variantId);

    ProductVariant findByProductIdAndVariantId(String productId, Integer variantId);

    @Modifying
    @Transactional
    void deleteByProductId(String productId);
} 