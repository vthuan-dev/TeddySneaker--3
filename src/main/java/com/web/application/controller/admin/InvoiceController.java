package com.web.application.controller.admin;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.web.application.dto.InvoiceDTO;
import com.web.application.entity.Invoice;
import com.web.application.model.request.CreateInvoiceRequest;
import com.web.application.service.InvoiceService;
import com.web.application.exception.NotFoundExp;

@Controller
public class InvoiceController {
    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/admin/invoices")
    public String getInvoicePage(Model model,
            @RequestParam(defaultValue = "", required = false) String invoiceNumber,
            @RequestParam(defaultValue = "", required = false) String orderId,
            @RequestParam(defaultValue = "1", required = false) Integer page) {
        
        Page<Invoice> invoices = invoiceService.adminGetListInvoices(invoiceNumber, orderId, page);
        model.addAttribute("invoices", invoices.getContent());
        model.addAttribute("totalPages", invoices.getTotalPages());
        model.addAttribute("currentPage", invoices.getPageable().getPageNumber() + 1);

        return "admin/invoice/list";
    }

    @GetMapping("/api/admin/invoices/{id}")
    public ResponseEntity<Object> getInvoiceDetail(@PathVariable long id) {
        InvoiceDTO invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(invoice);
    }

    @PostMapping("/api/admin/invoices")
    public ResponseEntity<Object> createInvoice(@Valid @RequestBody CreateInvoiceRequest createInvoiceRequest) {
        Invoice invoice = invoiceService.createInvoice(createInvoiceRequest);
        return ResponseEntity.ok(invoice);
    }

    @PutMapping("/api/admin/invoices/{id}")
    public ResponseEntity<Object> updateInvoice(@Valid @RequestBody CreateInvoiceRequest updateInvoiceRequest,
            @PathVariable long id) {
        invoiceService.updateInvoice(updateInvoiceRequest, id);
        return ResponseEntity.ok("Cập nhật hóa đơn thành công!");
    }

    @DeleteMapping("/api/admin/invoices/{id}")
    public ResponseEntity<Object> deleteInvoice(@PathVariable long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.ok("Xóa hóa đơn thành công!");
    }

    @GetMapping("/api/admin/invoices/export/{orderId}")
    @CrossOrigin
    public ResponseEntity<byte[]> exportInvoice(@PathVariable Long orderId) {
        try {
            byte[] pdfBytes = invoiceService.generateInvoicePdf(orderId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("invoice-" + orderId + ".pdf")
                    .build());
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (NotFoundExp e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 