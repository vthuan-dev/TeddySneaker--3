package com.web.application.service;

import org.springframework.data.domain.Page;

import com.web.application.dto.InvoiceDTO;
import com.web.application.entity.Invoice;
import com.web.application.model.request.CreateInvoiceRequest;

public interface InvoiceService {
    // Lấy danh sách hóa đơn cho admin
    Page<Invoice> adminGetListInvoices(String invoiceNumber, String orderId, Integer page);
    
    // Lấy chi tiết hóa đơn
    InvoiceDTO getInvoiceById(Long id);
    
    // Tạo hóa đơn mới
    Invoice createInvoice(CreateInvoiceRequest request);
    
    // Cập nhật hóa đơn
    void updateInvoice(CreateInvoiceRequest request, Long id);
    
    // Xóa hóa đơn
    void deleteInvoice(Long id);
    
    Invoice findByOrderId(Long orderId);
    
    byte[] generateInvoicePdf(Long orderId) throws Exception;
} 