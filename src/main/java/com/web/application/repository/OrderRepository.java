package com.web.application.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.web.application.dto.OrderDetailDTO;
import com.web.application.dto.OrderInfoDTO;
import com.web.application.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query(value = "SELECT DISTINCT o FROM Order o LEFT JOIN o.items i WHERE " +
            "(:id IS NULL OR CAST(o.id AS string) LIKE %:id%) AND " +
            "(:name IS NULL OR o.receiverName LIKE %:name%) AND " +
            "(:phone IS NULL OR o.receiverPhone LIKE %:phone%) AND " +
            "(:status IS NULL OR CAST(o.status AS string) LIKE %:status%) AND " +
            "(:product IS NULL OR i.product.id LIKE %:product%) AND " +
            "(:createdAt IS NULL OR CAST(CAST(o.createdAt AS date) AS string) = :createdAt) AND " +
            "(:modifiedAt IS NULL OR CAST(CAST(o.modifiedAt AS date) AS string) = :modifiedAt)")
    Page<Order> adminGetListOrder(
            @Param("id") String id,
            @Param("name") String name,
            @Param("phone") String phone,
            @Param("status") String status,
            @Param("product") String product,
            @Param("createdAt") String createdAt,
            @Param("modifiedAt") String modifiedAt,
            Pageable pageable
    );

    @Query(nativeQuery = true, name = "getListOrderOfPersonByStatus")
    List<OrderInfoDTO> getListOrderOfPersonByStatus(int status, long userId);

    @Query(nativeQuery = true, name = "userGetDetailById")
    OrderDetailDTO userGetDetailById(long id, long userId);

    @Query(value = "SELECT COUNT(*) FROM orders WHERE product_id = ?1", nativeQuery = true)
    int countByProductId(String productId);

    @Query("SELECT o FROM Order o WHERE o.buyer.id = :userId AND o.status = :status")
    List<Order> findByBuyerIdAndStatus(@Param("userId") long userId, @Param("status") int status);

    List<Order> findByBuyerIdOrderByCreatedAtDesc(long userId);
    List<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(long userId, int status);

    Optional<Order> findByIdAndBuyerId(long id, long buyerId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") int status);

    @Query(value = "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i WHERE " +
            "(:id IS NULL OR CAST(o.id AS string) LIKE %:id%) AND " +
            "(:name IS NULL OR o.receiverName LIKE %:name%) AND " +
            "(:phone IS NULL OR o.receiverPhone LIKE %:phone%) AND " +
            "(:status IS NULL OR CAST(o.status AS string) = :status) AND " +
            "(:product IS NULL OR i.product.id = :product) AND " +
            "(:createdAt IS NULL OR DATE(o.createdAt) = DATE(:createdAt)) AND " +
            "(:modifiedAt IS NULL OR DATE(o.modifiedAt) = DATE(:modifiedAt))")
    Page<Order> findByConditions(
            @Param("id") String id,
            @Param("name") String name, 
            @Param("phone") String phone,
            @Param("status") String status,
            @Param("product") String product,
            @Param("createdAt") String createdAt,
            @Param("modifiedAt") String modifiedAt,
            Pageable pageable);
}

// LIST_ORDER_STATUSz