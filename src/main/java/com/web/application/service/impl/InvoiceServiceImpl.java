package com.web.application.service.impl;

import com.web.application.service.InvoiceService;
import com.web.application.repository.InvoiceRepository;
import com.web.application.entity.Invoice;
import com.web.application.dto.InvoiceDTO;
import com.web.application.exception.NotFoundExp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Override
    public Page<Invoice> getInvoices(String fromDate, String toDate, 
                                   String status, int page) {
        Timestamp fromTimestamp = null;
        Timestamp toTimestamp = null;
        
        try {
            if (fromDate != null && !fromDate.isEmpty()) {
                fromTimestamp = new Timestamp(new SimpleDateFormat("yyyy-MM-dd")
                    .parse(fromDate).getTime());
            }
            if (toDate != null && !toDate.isEmpty()) {
                toTimestamp = new Timestamp(new SimpleDateFormat("yyyy-MM-dd")
                    .parse(toDate).getTime());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Pageable pageable = PageRequest.of(page, 10, 
            Sort.by("createdAt").descending());
            
        return invoiceRepository.findByFilters(fromTimestamp, toTimestamp, 
            status, pageable);
    }

    @Override
    public InvoiceDTO getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
            .orElseThrow(() -> new NotFoundExp("Không tìm thấy hóa đơn"));
            
        return convertToDTO(invoice);
    }

    @Override
    public void deleteInvoice(Long id) {
        if (!invoiceRepository.existsById(id)) {
            throw new NotFoundExp("Không tìm thấy hóa đơn");
        }
        invoiceRepository.deleteById(id);
    }

    @Override
    public void updatePaymentStatus(Long id, int status) {
        Invoice invoice = invoiceRepository.findById(id)
            .orElseThrow(() -> new NotFoundExp("Không tìm thấy hóa đơn"));
        invoice.setPaymentStatus(status);
        invoice.setModifiedAt(new Timestamp(System.currentTimeMillis()));
        invoiceRepository.save(invoice);
    }

    private InvoiceDTO convertToDTO(Invoice invoice) {
        return new InvoiceDTO(
            invoice.getId(),
            invoice.getOrder().getId(),
            invoice.getOrder().getBuyer().getFullName(),
            invoice.getTotalAmount(),
            invoice.getPaymentStatus(),
            invoice.getPaymentMethod(),
            invoice.getCreatedAt()
        );
    }
} 