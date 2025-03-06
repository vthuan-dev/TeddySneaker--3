package com.web.application.repository;

import com.web.application.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    @Query(value = "SELECT * FROM invoices i " +
            "WHERE i.created_at >= ?1 " +
            "AND i.created_at <= ?2 " +
            "AND i.payment_status LIKE CONCAT('%',?3,'%')", 
            nativeQuery = true)
    Page<Invoice> findByFilters(Timestamp fromDate, Timestamp toDate, 
                              String status, Pageable pageable);
} 