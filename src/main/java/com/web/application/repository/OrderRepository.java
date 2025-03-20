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
    @Query(value = "SELECT * FROM orders " +
            "WHERE id LIKE CONCAT('%',?1,'%') " +
            "AND receiver_name LIKE CONCAT('%',?2,'%') " +
            "AND receiver_phone LIKE CONCAT('%',?3,'%') " +
            "AND status LIKE CONCAT('%',?4,'%') " +
            "AND product_id LIKE CONCAT('%',?5,'%') " +
            "AND created_at LIKE CONCAT('%',?6,'%') " , nativeQuery = true)
    Page<Order> adminGetListOrder(String id, String name, String phone, String status, String product, String createdAt, String modifiedAt, Pageable pageable);

    @Query(nativeQuery = true, name = "getListOrderOfPersonByStatus")
    List<OrderInfoDTO> getListOrderOfPersonByStatus(int status, long userId);

    @Query(nativeQuery = true, name = "userGetDetailById")
    OrderDetailDTO userGetDetailById(long id, long userId);

    @Query(value = "SELECT COUNT(*) FROM orders WHERE product_id = ?1", nativeQuery = true)
    int countByProductId(String productId);

    @Query("SELECT o FROM Order o WHERE o.buyer.id = :userId AND o.status = :status")
    List<Order> findByBuyerIdAndStatus(@Param("userId") long userId, @Param("status") int status);

    List<Order> findByBuyerIdOrderByCreatedAtDesc(long buyerId);
    List<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(long buyerId, int status);

    Optional<Order> findByIdAndBuyerId(long id, long buyerId);
}

// LIST_ORDER_STATUSz