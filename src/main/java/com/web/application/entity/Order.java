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
	    query = "SELECT od.id, od.total_price, od.size AS size_vn, p.name AS product_name, " +
	            "JSON_VALUE(p.images, '$[0]') AS product_img " +
	            "FROM orders od " +
	            "INNER JOIN product p " +
	            "ON od.product_id = p.id " +
	            "WHERE od.status = ?1 " +
	            "AND od.buyer = ?2"
	)

@NamedNativeQuery(
	    name = "userGetDetailById",
	    resultSetMapping = "orderDetailDto",
	    query = "SELECT od.id, od.total_price, od.price as product_price, " +
	            "od.receiver_name, od.receiver_phone, od.receiver_address, od.status, od.size AS size_vn, " +
	            "p.name AS product_name, " +
	            "JSON_VALUE(p.images, '$[0]') AS product_img " +
	            "FROM orders od " +
	            "INNER JOIN product p " +
	            "ON od.product_id = p.id " +
	            "WHERE od.id = ?1 " +
	            "AND od.buyer = ?2"
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
    
    // Phương thức để thêm item vào order
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
    
    // Phương thức để xóa item khỏi order
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
    
    // Phương thức tính tổng giá trị đơn hàng
    public void calculateTotalPrice() {
        this.totalPrice = items.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
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
}
