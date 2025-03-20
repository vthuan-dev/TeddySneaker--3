package com.web.application.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.web.application.entity.ProductSize;

@Repository
public interface ProductSizeRepository extends JpaRepository<ProductSize, Long> {

	// Lấy size của sản phẩm
	@Query(nativeQuery = true, value = "SELECT ps.size FROM product_size ps WHERE ps.product_id = ?1 AND ps.quantity > 0")
	List<Integer> findAllSizeOfProduct(String id);

	List<ProductSize> findByProductId(String id);

	@Query("SELECT COALESCE(SUM(ps.quantity), 0) FROM ProductSize ps WHERE ps.productId = :productId")
	public long getTotalQuantityByProductId(@Param("productId") String productId);

	// Kiểm trả size sản phẩm
	@Query("SELECT ps FROM ProductSize ps WHERE ps.productId = :productId AND ps.size = :size AND ps.quantity > 0")
	ProductSize checkProductAndSizeAvailable(@Param("productId") String productId, @Param("size") Integer size);

	// Trừ 1 sản phẩm theo size
	@Transactional
	@Modifying
	@Query(nativeQuery = true, value = "Update product_size set quantity = quantity - 1 where product_id = ?1 and size = ?2")
	void minusOneProductBySize(String id, Integer size);

	// Cộng 1 sản phẩm theo size
	@Transactional
	@Modifying
	@Query(nativeQuery = true, value = "Update product_size set quantity = quantity + 1 where product_id = ?1 and size = ?2")
	public void plusOneProductBySize(String id, Integer size);

//    @Query(value = "SELECT * FROM product_size ps WHERE ps.size = ?1 AND ps.product_id = ?2",nativeQuery = true)
//    Optional<ProductSize> getProductSizeBySize(int size,String productId);

	@Transactional
	@Modifying
	@Query(nativeQuery = true, value = "Delete from product_size where product_id = ?1")
	public void deleteByProductId(String id);
}
