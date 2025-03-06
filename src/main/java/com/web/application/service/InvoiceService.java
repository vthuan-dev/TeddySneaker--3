package com.web.application.service;

import org.springframework.data.domain.Page;

import com.web.application.dto.InvoiceDTO;
import com.web.application.entity.Invoice;

public interface InvoiceService {
    Page<Invoice> getInvoices(String fromDate, String toDate, 
                             String status, int page);
    InvoiceDTO getInvoiceById(Long id);
    void deleteInvoice(Long id);
    void updatePaymentStatus(Long id, int status);
} 