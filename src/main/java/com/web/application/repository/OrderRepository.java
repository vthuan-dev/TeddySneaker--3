package com.web.application.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.web.application.dto.OrderDetailDTO;
import com.web.application.dto.OrderInfoDTO;
import com.web.application.entity.Order;

import java.util.List;

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

    int countByProductId(String id);
}
