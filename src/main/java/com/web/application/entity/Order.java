package com.web.application.entity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.web.application.dto.OrderDetailDTO;
import com.web.application.dto.OrderInfoDTO;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SqlResultSetMappings(
        value = {
                @SqlResultSetMapping(
                        name = "orderInfoDTO",
                        classes = @ConstructorResult(
                                targetClass = OrderInfoDTO.class,
                                columns = {
                                        @ColumnResult(name = "id", type = Long.class),
                                        @ColumnResult(name = "total_price", type = Long.class),
                                        @ColumnResult(name = "size_vn", type = Integer.class),
                                        @ColumnResult(name = "product_name", type = String.class),
                                        @ColumnResult(name = "product_img", type = String.class)
                                }
                        )
                ),
                @SqlResultSetMapping(
                        name = "orderDetailDto",
                        classes = @ConstructorResult(
                                targetClass = OrderDetailDTO.class,
                                columns = {
                                        @ColumnResult(name = "id", type = Long.class),
                                        @ColumnResult(name = "total_price", type = Long.class),
                                        @ColumnResult(name = "product_price", type = Long.class),
                                        @ColumnResult(name = "receiver_name", type = String.class),
                                        @ColumnResult(name = "receiver_phone", type = String.class),
                                        @ColumnResult(name = "receiver_address", type = String.class),
                                        @ColumnResult(name = "status", type = Integer.class),
                                        @ColumnResult(name = "size_vn", type = Integer.class),
                                        @ColumnResult(name = "product_name", type = String.class),
                                        @ColumnResult(name = "product_img", type = String.class)
                                }
                        )
                )
        }
)
@NamedNativeQuery(
        name = "getListOrderOfPersonByStatus",
        resultSetMapping = "orderInfoDTO",
        query = "SELECT o.id, o.total_price, oi.size AS size_vn, p.name AS product_name, " +
                "JSON_VALUE(p.images, '$[0]') AS product_img " +
                "FROM orders o " +
                "INNER JOIN order_items oi ON o.id = oi.order_id " +
                "INNER JOIN product p ON oi.product_id = p.id " +
                "WHERE o.status = ?1 " +
                "AND o.buyer = ?2"
)

@NamedNativeQuery(
        name = "userGetDetailById",
        resultSetMapping = "orderDetailDto",
        query = "SELECT o.id, o.total_price, oi.price as product_price, " +
                "o.receiver_name, o.receiver_phone, o.receiver_address, o.status, oi.size AS size_vn, " +
                "p.name AS product_name, " +
                "JSON_VALUE(p.images, '$[0]') AS product_img " +
                "FROM orders o " +
                "INNER JOIN order_items oi ON o.id = oi.order_id " +
                "INNER JOIN product p ON oi.product_id = p.id " +
                "WHERE o.id = ?1 " +
                "AND o.buyer = ?2"
)

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "receiver_phone")
    private String receiverPhone;

    @Column(name = "receiver_address")
    private String receiverAddress;

    @Column(name = "total_price")
    private Double totalPrice;

    @ManyToOne
    @JoinColumn(name = "buyer")
    private User buyer;

    @Column(name = "status")
    private int status;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "modified_at")
    private Timestamp modifiedAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "modified_by")
    private User modifiedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "promotion", columnDefinition = "json")
    private UsedPromotion promotion;
    
    @Column(name = "note")
    private String note;
    
    // Thêm mối quan hệ one-to-many với OrderItem
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    
    // Add helper method
    public void addOrderItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
    
    // Calculate total price
    public void calculateTotalPrice() {
        double total = 0;
        for (OrderItem item : items) {
            total += item.getPrice() * item.getQuantity();
        }
        
        // Áp dụng giảm giá nếu có
        if (this.promotion != null) {
            if (promotion.getDiscountType() == 1) {
                // Giảm giá theo phần trăm
                double discount = (total * promotion.getDiscountValue()) / 100;
                if (promotion.getMaximumDiscountValue() > 0) {
                    discount = Math.min(discount, promotion.getMaximumDiscountValue());
                }
                total -= discount;
            } else {
                // Giảm giá trực tiếp
                total -= promotion.getDiscountValue();
            }
        }
        
        this.totalPrice = total;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsedPromotion {
        private String couponCode;
        private int discountType;
        private long discountValue;
        private long maximumDiscountValue;

        public String getCouponCode() {
            return couponCode;
        }

        public void setCouponCode(String couponCode) {
            this.couponCode = couponCode;
        }

        public int getDiscountType() {
            return discountType;
        }

        public void setDiscountType(int discountType) {
            this.discountType = discountType;
        }

        public long getDiscountValue() {
            return discountValue;
        }

        public void setDiscountValue(long discountValue) {
            this.discountValue = discountValue;
        }

        public long getMaximumDiscountValue() {
            return maximumDiscountValue;
        }

        public void setMaximumDiscountValue(long maximumDiscountValue) {
            this.maximumDiscountValue = maximumDiscountValue;
        }
    }

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_CONFIRMED = 1;
    public static final int STATUS_SHIPPING = 2;
    public static final int STATUS_COMPLETED = 3;
    public static final int STATUS_CANCELLED = 4;
}
