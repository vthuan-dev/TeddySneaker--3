package com.web.application.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.web.application.dto.ChartDTO;
import com.web.application.dto.ProductInfoDTO;
import com.web.application.dto.ShortProductInfoDTO;
import com.web.application.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    //Lấy sản phẩm theo tên
    Product findByName(String name);

    @Query(value = """
    	    SELECT pro.*, 
    	           COALESCE(SUM(ps.quantity), 0) AS total_quantity
    	    FROM product pro
    	    RIGHT JOIN (
    	        SELECT DISTINCT p.*
    	        FROM product p
    	        INNER JOIN product_category pc ON p.id = pc.product_id
    	        INNER JOIN category c ON c.id = pc.category_id
    	        WHERE p.id LIKE CONCAT('%',?1,'%')
    	          AND p.name LIKE CONCAT('%',?2,'%')
    	          AND c.id LIKE CONCAT('%',?3,'%')
    	          AND p.brand_id LIKE CONCAT('%',?4,'%')
    	    ) AS tb1 ON pro.id = tb1.id
    	    LEFT JOIN product_size ps ON ps.product_id = pro.id
    	    GROUP BY pro.id, pro.name, pro.description, pro.price, pro.sale_price, 
             pro.slug, pro.images, pro.image_feedback, pro.product_view, 
             pro.total_sold, pro.status, pro.created_at, pro.modified_at, 
             pro.brand_id""", nativeQuery = true)
    	Page<Product> adminGetListProducts(String id, String name, String category, String brand, Pageable pageable);


//    @Query(value = "SELECT NEW dto.model.com.nhutkhuong1.application.ProductInfoDTO(p.id, p.name, p.slug, p.price ,p.images ->> '$[0]', p.total_sold) " +
//            "FROM product p " +
//            "WHERE p.status = 1 " +
//            "ORDER BY p.created_at DESC limit ?1",nativeQuery = true)
//    List<ProductInfoDTO> getListBestSellProducts(int limit);

    //Lấy sản phẩm được bán nhiều
    @Query(nativeQuery = true,name = "getListBestSellProducts")
    List<ProductInfoDTO> getListBestSellProducts(int limit);

    //Lấy sản phẩm mới nhất
    @Query(nativeQuery = true,name = "getListNewProducts")
    List<ProductInfoDTO> getListNewProducts(int limit);

    //Lấy sản phẩm được xem nhiều
    @Query(nativeQuery = true,name = "getListViewProducts")
    List<ProductInfoDTO> getListViewProducts(int limit);

    //Lấy sản phẩm liên quan
    @Query(nativeQuery = true, name = "getRelatedProducts")
    List<ProductInfoDTO> getRelatedProducts(String id, int limit);

    //Lấy sản phẩm
    @Query(name = "getAllProduct", nativeQuery = true)
    List<ShortProductInfoDTO> getListProduct();

    //Lấy sản phẩm có sẵn size
    @Query(nativeQuery = true, name = "getAllBySizeAvailable")
    List<ShortProductInfoDTO> getAvailableProducts();

    //Trừ một sản phẩm đã bán
    @Transactional
    @Modifying
    @Query(value = "UPDATE product SET total_sold = total_sold - 1 WHERE id = ?1", nativeQuery = true)
    void minusOneProductTotalSold(String productId);

    //Cộng một sản phẩm đã bán
    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "Update product set total_sold = total_sold + 1 where id = ?1")
    void plusOneProductTotalSold(String productId);

    //Tìm kiến sản phẩm theo size
    @Query(nativeQuery = true, name = "searchProductBySize")
    List<ProductInfoDTO> searchProductBySize(List<Long> brands, List<Long> categories, long minPrice, long maxPrice, List<Integer> sizes, int limit, int offset);

    //Đếm số sản phẩm
    @Query(nativeQuery = true, value = "SELECT COUNT(DISTINCT d.id) " +
            "FROM (" +
            "SELECT DISTINCT product.id " +
            "FROM product " +
            "INNER JOIN product_category " +
            "ON product.id = product_category.product_id " +
            "WHERE product.status = 1 AND product.brand_id IN (?1) AND product_category.category_id IN (?2) " +
            "AND product.price > ?3 AND product.price < ?4) as d " +
            "INNER JOIN product_size " +
            "ON product_size.product_id = d.id " +
            "WHERE product_size.size IN (?5)")
    int countProductBySize(List<Long> brands, List<Long> categories, long minPrice, long maxPrice, List<Integer> sizes);

    //Tìm kiến sản phẩm k theo size
    @Query(nativeQuery = true, name = "searchProductAllSize")
    List<ProductInfoDTO> searchProductAllSize(List<Long> brands, List<Long> categories, long minPrice, long maxPrice, int limit, int offset);

    //Đếm số sản phẩm
    @Query(nativeQuery = true, value = "SELECT COUNT(DISTINCT product.id) " +
            "FROM product " +
            "INNER JOIN product_category " +
            "ON product.id = product_category.product_id " +
            "WHERE product.status = 1 AND product.brand_id IN (?1) AND product_category.category_id IN (?2) " +
            "AND product.price > ?3 AND product.price < ?4 ")
    int countProductAllSize(List<Long> brands, List<Long> categories, long minPrice, long maxPrice);

    //Tìm kiến sản phẩm theo tên và tên danh mục
    @Query(nativeQuery = true, name = "searchProductByKeyword")
    List<ProductInfoDTO> searchProductByKeyword(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    //Đếm số sản phẩm
    @Query(nativeQuery = true, value = "SELECT count(DISTINCT product.id) " +
            "FROM product " +
            "INNER JOIN product_category " +
            "ON product.id = product_category.product_id " +
            "INNER JOIN category " +
            "ON category.id = product_category.category_id " +
            "WHERE product.status = 1 AND (product.name LIKE CONCAT('%',:keyword,'%') OR category.name LIKE CONCAT('%',:keyword,'%')) ")
    int countProductByKeyword(@Param("keyword") String keyword);

    @Query(value = "SELECT p.name AS label, SUM(oi.quantity) AS value " +
            "FROM product p " +
            "INNER JOIN order_items oi ON p.id = oi.product_id " +
            "INNER JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.status = 3 " +
            "AND MONTH(o.created_at) = :month " +
            "AND YEAR(o.created_at) = :year " +
            "GROUP BY p.id, p.name " +
            "ORDER BY SUM(oi.quantity) DESC", 
            nativeQuery = true)
    List<ChartDTO> getProductOrders(Pageable pageable, @Param("month") int month, @Param("year") int year);
}
