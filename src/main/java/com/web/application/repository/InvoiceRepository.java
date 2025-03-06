package com.web.application.repository;

import java.sql.Timestamp;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.web.application.entity.Invoice;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    boolean existsByOrderId(Long orderId);
    Optional<Invoice> findByOrderId(Long orderId);

    @Query(value = "SELECT * FROM invoices i " +
            "WHERE (:fromDate IS NULL OR i.created_at >= :fromDate) " +
            "AND (:toDate IS NULL OR i.created_at <= :toDate) " +
            "AND (:status IS NULL OR i.payment_status = :status)", 
            nativeQuery = true)
    Page<Invoice> findByFilters(Timestamp fromDate, Timestamp toDate, 
                              String status, Pageable pageable);
} 