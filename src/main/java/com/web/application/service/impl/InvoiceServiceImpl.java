package com.web.application.service.impl;

import com.web.application.service.InvoiceService;
import com.web.application.repository.InvoiceRepository;
import com.web.application.repository.OrderRepository;
import com.web.application.entity.Invoice;
import com.web.application.entity.Order;
import com.web.application.entity.OrderItem;
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
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.math.BigDecimal;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.property.VerticalAlignment;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.kernel.colors.ColorConstants;

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

    @Override
    public Invoice findByOrderId(Long orderId) {
        return invoiceRepository.findByOrderId(orderId)
            .orElse(null);
    }

    private InvoiceDTO convertToDTO(Invoice invoice) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setOrderId(invoice.getOrder().getId());
        dto.setSubtotal(invoice.getSubtotal());
        dto.setDiscount(invoice.getDiscount());
        dto.setTax(invoice.getTax());
        dto.setTotal(invoice.getTotal());
        dto.setPaymentMethod(invoice.getPaymentMethod());
        dto.setPaymentStatus(invoice.getPaymentStatus());
        dto.setNote(invoice.getNote());
        dto.setCreatedAt(invoice.getCreatedAt());
        return dto;
    }

    @Override
    public byte[] generateInvoicePdf(Long orderId) throws Exception {
        try {
            // Tìm đơn hàng trước
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundExp("Không tìm thấy đơn hàng với ID: " + orderId));
                
            // Tìm hóa đơn theo orderId
            Invoice invoice = findByOrderId(orderId);
            
            // Nếu chưa có hóa đơn, tạo mới
            if (invoice == null) {
                CreateInvoiceRequest request = new CreateInvoiceRequest();
                request.setOrderId(orderId);
                
                // Tính tổng tiền từ tất cả các items
                double subtotal = order.getItems().stream()
                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                    .sum();
                    
                request.setSubtotal(BigDecimal.valueOf(subtotal));
                request.setDiscount(BigDecimal.ZERO);
                request.setTax(BigDecimal.ZERO);
                request.setTotal(BigDecimal.valueOf(order.getTotalPrice()));
                request.setPaymentMethod("Tiền mặt");
                request.setPaymentStatus(1);
                
                invoice = createInvoice(request);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            try {
                // Thêm một trang mới vào tài liệu
                pdf.addNewPage();
                
                // Sử dụng font mặc định thay vì font với encoding IDENTITY-H
                PdfFont font = PdfFontFactory.createFont("Helvetica", PdfEncodings.CP1252);
                document.setFont(font);

                // Thêm logo - bỏ qua nếu không tìm thấy
                try {
                    // Đường dẫn tương đối đến logo
                    String logoPath = "static/images/logo.jpg";
                    
                    ImageData imageData = ImageDataFactory.create(
                        getClass().getClassLoader().getResource(logoPath)
                    );
                    Image logo = new Image(imageData);
                    logo.setWidth(100);
                    document.add(logo);
                } catch (Exception e) {
                    // Bỏ qua lỗi logo, chỉ ghi log
                    System.out.println("Không thể tải logo: " + e.getMessage());
                    // Vẫn thêm một đoạn văn bản thay cho logo để đảm bảo có nội dung
                    document.add(new Paragraph("SOUVENIR SHOP"));
                }
                
                // Thêm thông tin hóa đơn
                document.add(new Paragraph("HOA DON BAN HANG").setBold().setFontSize(16).setTextAlignment(TextAlignment.CENTER));
                document.add(new Paragraph("Ma hoa don: " + invoice.getInvoiceNumber()));
                
                // Format ngày tạo
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                document.add(new Paragraph("Ngay tao: " + sdf.format(invoice.getCreatedAt())));
                
                // Thêm thông tin đơn hàng
                if (order != null) {
                    document.add(new Paragraph("Thong tin don hang:").setBold());
                    document.add(new Paragraph("Nguoi nhan: " + order.getReceiverName()));
                    document.add(new Paragraph("So dien thoai: " + order.getReceiverPhone()));
                    document.add(new Paragraph("Dia chi: " + order.getReceiverAddress()));
                    
                    // Tạo bảng chi tiết sản phẩm
                    if (order.getItems() != null && !order.getItems().isEmpty()) {
                        float[] columnWidths = {3f, 1f, 1f, 2f, 2f};
                        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                            .useAllAvailableWidth();
                        
                        // Tiêu đề bảng
                        table.addHeaderCell(new Cell().add(new Paragraph("San pham").setBold()));
                        table.addHeaderCell(new Cell().add(new Paragraph("Loai/Mau").setBold()));
                        table.addHeaderCell(new Cell().add(new Paragraph("So luong").setBold()));
                        table.addHeaderCell(new Cell().add(new Paragraph("Don gia").setBold()));
                        table.addHeaderCell(new Cell().add(new Paragraph("Thanh tien").setBold()));
                        
                        // Thêm từng sản phẩm vào bảng
                        for (OrderItem item : order.getItems()) {
                            if (item.getProduct() != null) {
                                table.addCell(new Cell().add(new Paragraph(item.getProduct().getName())));
                                table.addCell(new Cell().add(new Paragraph(String.valueOf(item.getSize()))));
                                table.addCell(new Cell().add(new Paragraph(String.valueOf(item.getQuantity()))));
                                table.addCell(new Cell().add(new Paragraph(String.format("%,.0f VND", item.getPrice()))));
                                table.addCell(new Cell().add(new Paragraph(String.format("%,.0f VND", item.getPrice() * item.getQuantity()))));
                            } else {
                                // Thêm hàng trống nếu không có sản phẩm
                                table.addCell(new Cell().add(new Paragraph("N/A")));
                                table.addCell(new Cell().add(new Paragraph("N/A")));
                                table.addCell(new Cell().add(new Paragraph("N/A")));
                                table.addCell(new Cell().add(new Paragraph("N/A")));
                                table.addCell(new Cell().add(new Paragraph("N/A")));
                            }
                        }
                        
                        document.add(table);
                    } else {
                        document.add(new Paragraph("Khong co thong tin chi tiet san pham").setFontColor(ColorConstants.RED));
                    }
                } else {
                    document.add(new Paragraph("Khong tim thay thong tin don hang").setFontColor(ColorConstants.RED));
                }
                
                // Thêm thông tin tổng tiền
                document.add(new Paragraph("Tong tien hang: " + String.format("%,.0f VND", invoice.getSubtotal())));
                document.add(new Paragraph("Giam gia: " + String.format("%,.0f VND", invoice.getDiscount())));
                document.add(new Paragraph("Thue VAT: " + String.format("%,.0f VND", invoice.getTax())));
                document.add(new Paragraph("Tong thanh toan: " + String.format("%,.0f VND", invoice.getTotal())).setBold());
            } finally {
                document.close();
            }
            
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalServerExp("Có lỗi xảy ra: " + e.getMessage());
        }
    }
} 