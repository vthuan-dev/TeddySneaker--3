package com.web.application.service.impl;

import com.web.application.service.InvoiceService;
import com.web.application.repository.InvoiceRepository;
import com.web.application.repository.OrderRepository;
import com.web.application.entity.Invoice;
import com.web.application.entity.Order;
import com.web.application.dto.InvoiceDTO;
import com.web.application.model.request.CreateInvoiceRequest;
import com.web.application.exception.BadRequestExp;
import com.web.application.exception.NotFoundExp;
import com.web.application.exception.InternalServerExp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.util.UUID;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public Page<Invoice> adminGetListInvoices(String invoiceNumber, String orderId, Integer page) {
        try {
            page = (page == null || page < 1) ? 0 : page - 1;
            Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
            return invoiceRepository.findAll(pageable);
        } catch (Exception e) {
            throw new InternalServerExp("Có lỗi xảy ra: " + e.getMessage());
        }
    }

    @Override
    public InvoiceDTO getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundExp("Không tìm thấy hóa đơn"));
        return convertToDTO(invoice);
    }

    @Override
    public Invoice createInvoice(CreateInvoiceRequest request) {
        // Kiểm tra order có tồn tại không
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NotFoundExp("Không tìm thấy đơn hàng"));

        // Kiểm tra đã có hóa đơn cho order này chưa
        if (invoiceRepository.existsByOrderId(request.getOrderId())) {
            throw new BadRequestExp("Đơn hàng này đã có hóa đơn");
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV" + UUID.randomUUID().toString().substring(0, 8));
        invoice.setOrder(order);
        invoice.setSubtotal(request.getSubtotal());
        invoice.setDiscount(request.getDiscount());
        invoice.setTax(request.getTax());
        invoice.setTotal(request.getTotal());
        invoice.setPaymentMethod(request.getPaymentMethod());
        invoice.setPaymentStatus(request.getPaymentStatus());
        invoice.setNote(request.getNote());
        invoice.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        return invoiceRepository.save(invoice);
    }

    @Override
    public void updateInvoice(CreateInvoiceRequest request, Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundExp("Không tìm thấy hóa đơn"));

        // Cập nhật thông tin
        invoice.setSubtotal(request.getSubtotal());
        invoice.setDiscount(request.getDiscount());
        invoice.setTax(request.getTax());
        invoice.setTotal(request.getTotal());
        invoice.setPaymentMethod(request.getPaymentMethod());
        invoice.setPaymentStatus(request.getPaymentStatus());
        invoice.setNote(request.getNote());

        invoiceRepository.save(invoice);
    }

    @Override
    public void deleteInvoice(Long id) {
        if (!invoiceRepository.existsById(id)) {
            throw new NotFoundExp("Không tìm thấy hóa đơn");
        }
        invoiceRepository.deleteById(id);
    }

    private InvoiceDTO convertToDTO(Invoice invoice) {
        return new InvoiceDTO(
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getOrder().getId(),
            invoice.getOrder().getReceiverName(),
            invoice.getCreatedAt(),
            invoice.getSubtotal(),
            invoice.getDiscount(),
            invoice.getTax(),
            invoice.getTotal(),
            invoice.getPaymentMethod(),
            invoice.getPaymentStatus(),
            invoice.getNote()
        );
    }
} 