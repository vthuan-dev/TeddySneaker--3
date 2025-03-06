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

    @Override
    public byte[] generateInvoicePdf(Long orderId) throws Exception {
        // Tìm hóa đơn theo orderId
        Invoice invoice = findByOrderId(orderId);
        
        // Nếu chưa có hóa đơn, tạo mới
        if (invoice == null) {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundExp("Không tìm thấy đơn hàng"));
                
            CreateInvoiceRequest request = new CreateInvoiceRequest();
            request.setOrderId(orderId);
            request.setSubtotal(BigDecimal.valueOf(order.getTotalPrice()));
            request.setDiscount(BigDecimal.ZERO);
            request.setTax(BigDecimal.ZERO);
            request.setTotal(BigDecimal.valueOf(order.getTotalPrice()));
            request.setPaymentMethod("Tiền mặt");
            request.setPaymentStatus(1); // Chưa thanh toán
            
            invoice = createInvoice(request);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Thêm font Arial
        PdfFont font = PdfFontFactory.createFont("fonts/arial.ttf", PdfEncodings.IDENTITY_H, true);
        document.setFont(font);

        // Thêm logo
        String logoPath = "static/images/logo.jpg"; // Đặt logo trong resources/static/images/
        ImageData imageData = ImageDataFactory.create(getClass().getClassLoader().getResource(logoPath));
        Image logo = new Image(imageData);
        logo.setWidth(100); // Điều chỉnh kích thước logo
        logo.setHeight(100);
        
        // Tạo bảng để căn chỉnh logo và tiêu đề
        Table headerTable = new Table(2);
        headerTable.setWidth(UnitValue.createPercentValue(100));
        
        // Cột 1: Logo
        Cell logoCell = new Cell().add(logo).setBorder(null);
        headerTable.addCell(logoCell);
        
        // Cột 2: Tiêu đề
        Cell titleCell = new Cell().setBorder(null);
        titleCell.add(new Paragraph("TEDDY SNEAKER")
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold());
        titleCell.add(new Paragraph("HÓA ĐƠN BÁN HÀNG")
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER));
        headerTable.addCell(titleCell);
        
        document.add(headerTable);

        // Thông tin hóa đơn
        document.add(new Paragraph("\n"));
        document.add(new Paragraph(String.format("Số hóa đơn: %s", invoice.getInvoiceNumber())));
        document.add(new Paragraph(String.format("Ngày tạo: %s", 
                new SimpleDateFormat("dd/MM/yyyy HH:mm").format(invoice.getCreatedAt()))));
        
        // Thông tin khách hàng
        Order order = invoice.getOrder();
        document.add(new Paragraph("\nTHÔNG TIN KHÁCH HÀNG")
                .setBold());
        document.add(new Paragraph(String.format("Họ tên: %s", order.getReceiverName())));
        document.add(new Paragraph(String.format("Địa chỉ: %s", order.getReceiverAddress())));
        document.add(new Paragraph(String.format("Điện thoại: %s", order.getReceiverPhone())));

        // Thông tin sản phẩm
        document.add(new Paragraph("\nCHI TIẾT ĐƠN HÀNG")
                .setBold());
        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 20, 20, 20}));
        table.setWidth(UnitValue.createPercentValue(100));
        
        // Header của bảng
        table.addHeaderCell(new Cell().add(new Paragraph("Sản phẩm").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Đơn giá").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Số lượng").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Thành tiền").setBold()));

        // Thêm sản phẩm vào bảng
        table.addCell(new Cell().add(new Paragraph(order.getProduct().getName())));
        table.addCell(new Cell().add(new Paragraph(String.format("%,.0f đ", (double)order.getPrice()))));
        table.addCell(new Cell().add(new Paragraph("1")));
        table.addCell(new Cell().add(new Paragraph(String.format("%,.0f đ", (double)order.getTotalPrice()))));

        document.add(table);

        // Tổng tiền
        document.add(new Paragraph("\nTHÔNG TIN THANH TOÁN")
                .setBold());
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}));
        summaryTable.setWidth(UnitValue.createPercentValue(100));

        summaryTable.addCell(new Cell().add(new Paragraph("Tổng tiền hàng:")).setBorder(null));
        summaryTable.addCell(new Cell().add(new Paragraph(String.format("%,.0f đ", invoice.getSubtotal().doubleValue())))
                .setTextAlignment(TextAlignment.RIGHT).setBorder(null));

        summaryTable.addCell(new Cell().add(new Paragraph("Giảm giá:")).setBorder(null));
        summaryTable.addCell(new Cell().add(new Paragraph(String.format("%,.0f đ", invoice.getDiscount().doubleValue())))
                .setTextAlignment(TextAlignment.RIGHT).setBorder(null));

        summaryTable.addCell(new Cell().add(new Paragraph("Thuế:")).setBorder(null));
        summaryTable.addCell(new Cell().add(new Paragraph(String.format("%,.0f đ", invoice.getTax().doubleValue())))
                .setTextAlignment(TextAlignment.RIGHT).setBorder(null));

        summaryTable.addCell(new Cell().add(new Paragraph("Tổng cộng:").setBold()).setBorder(null));
        summaryTable.addCell(new Cell().add(new Paragraph(String.format("%,.0f đ", invoice.getTotal().doubleValue())).setBold())
                .setTextAlignment(TextAlignment.RIGHT).setBorder(null));

        document.add(summaryTable);

        // Chữ ký
        document.add(new Paragraph("\n\n"));
        Table signatureTable = new Table(2);
        signatureTable.setWidth(UnitValue.createPercentValue(100));
        
        signatureTable.addCell(new Cell()
                .add(new Paragraph("Người mua hàng\n\n\n(Ký, ghi rõ họ tên)"))
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(null));
        
        signatureTable.addCell(new Cell()
                .add(new Paragraph("Người bán hàng\n\n\n(Ký, ghi rõ họ tên)"))
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(null));
        
        document.add(signatureTable);

        // Footer
        document.add(new Paragraph("\n\nCảm ơn quý khách đã mua hàng!")
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic());

        document.close();
        return baos.toByteArray();
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