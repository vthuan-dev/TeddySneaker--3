package com.web.application.entity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.web.application.dto.ChartDTO;
import com.web.application.dto.ProductInfoDTO;
import com.web.application.dto.ShortProductInfoDTO;

import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
        value ={
                @SqlResultSetMapping(
                        name ="productInfoDto",
                        classes =@ConstructorResult(
                                targetClass = ProductInfoDTO.class,
                                columns ={
                                        @ColumnResult(name = "id", type = String.class),
                                        @ColumnResult(name = "name", type = String.class),
                                        @ColumnResult(name = "slug", type = String.class),
                                        @ColumnResult(name = "price", type = Long.class),
                                        @ColumnResult(name = "views",type = Integer.class),
                                        @ColumnResult(name = "images", type = String.class),
                                        @ColumnResult(name = "total_sold", type = Integer.class)
                                }
                        )
                ),
                @SqlResultSetMapping(
                        name = "shortProductInfoDTO",
                        classes = @ConstructorResult(
                                targetClass = ShortProductInfoDTO.class,
                                columns = {
                                        @ColumnResult(name = "id", type = String.class),
                                        @ColumnResult(name = "name", type = String.class)
                                }
                        )
                ),
                @SqlResultSetMapping(
                        name = "productInfoAndAvailableSize",
                        classes = @ConstructorResult(
                                targetClass = ShortProductInfoDTO.class,
                                columns = {
                                        @ColumnResult(name = "id", type = String.class),
                                        @ColumnResult(name = "name", type = String.class),
                                        @ColumnResult(name = "price", type = Long.class),
                                        @ColumnResult(name = "availableSizes", type = String.class),

                                }
                        )
                ),
                @SqlResultSetMapping(
                        name = "chartProductDTO",
                        classes = @ConstructorResult(
                                targetClass = ChartDTO.class,
                                columns = {
                                        @ColumnResult(name = "label",type = String.class),
                                        @ColumnResult(name = "value",type = Integer.class)
                                }
                        )
                )
        }
)

@NamedNativeQuery(
        name = "getListNewProducts",
        resultSetMapping = "productInfoDto",
        query = "SELECT p.id, p.name, p.sale_price as price, p.product_view as views, p.slug, p.total_sold, " +
                "JSON_VALUE(p.images, '$[0]') AS images " +
                "FROM product p WHERE p.status = 1 " +
                "ORDER BY p.created_at DESC OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY"
)

@NamedNativeQuery(
        name = "getListBestSellProducts",
        resultSetMapping = "productInfoDto",
        query = "SELECT p.id, p.name, p.sale_price AS price, p.product_view AS views, p.slug, p.total_sold, " +
                "JSON_VALUE(p.images, '$[0]') AS images " +
                "FROM product p " +
                "WHERE p.status = 1 " +
                "ORDER BY p.total_sold DESC " +
                "OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY"
)

@NamedNativeQuery(
        name = "getListViewProducts",
        resultSetMapping = "productInfoDto",
        query = "SELECT p.id, p.name, p.sale_price AS price, p.product_view AS views, p.slug, p.total_sold, " +
                "JSON_VALUE(p.images, '$[0]') AS images " +
                "FROM product p " +
                "WHERE p.status = 1 " +
                "ORDER BY p.product_view DESC " +
                "OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY"
)

@NamedNativeQuery(
        name = "getRelatedProducts",
        resultSetMapping = "productInfoDto",
        query = "SELECT TOP (?2) p.id, p.name, p.sale_price AS price, p.product_view AS views, p.slug, p.total_sold, " +
                "JSON_VALUE(p.images, '$[0]') AS images " +
                "FROM product p " +
                "WHERE p.status = 1 " +
                "AND p.id != ?1 " +
                "ORDER BY NEWID()"
)

@NamedNativeQuery(
        name = "getAllProduct",
        resultSetMapping = "shortProductInfoDTO",
        query = "SELECT p.id, p.name FROM product p"
)
@NamedNativeQuery(
	    name = "getAllBySizeAvailable",
	    resultSetMapping = "productInfoAndAvailableSize",
	    query = "SELECT p.id, p.name, p.sale_price AS price, STRING_AGG(ps.size, ',') AS availableSizes " +
	            "FROM product p LEFT JOIN product_size ps ON ps.product_id = p.id " +
	            "WHERE ps.quantity > 0 GROUP BY p.id, p.name, p.sale_price"
)

@NamedNativeQuery(
        name = "searchProductBySize",
        resultSetMapping = "productInfoDto",
        query = "SELECT DISTINCT d.* " +
                "FROM (" +
                "SELECT DISTINCT product.id, product.name, product.slug, product.sale_price AS price, product.product_view AS views, product.total_sold, " +
                "JSON_VALUE(product.images, '$[0]') AS images " +
                "FROM product " +
                "INNER JOIN product_category " +
                "ON product.id = product_category.product_id " +
                "WHERE product.status = 1 " +
                "AND product.brand_id IN (?1) " +
                "AND product_category.category_id IN (?2) " +
                "AND product.sale_price > ?3 " +
                "AND product.sale_price < ?4) AS d " +
                "INNER JOIN product_size " +
                "ON product_size.product_id = d.id " +
                "WHERE product_size.size IN (?5) " +
                "ORDER BY d.id " +
                "OFFSET ?7 ROWS FETCH NEXT ?6 ROWS ONLY"
)

@NamedNativeQuery(
        name = "searchProductAllSize",
        resultSetMapping = "productInfoDto",
        query = "SELECT DISTINCT product.id, product.name, product.slug, product.sale_price AS price, product.product_view AS views, product.total_sold, " +
                "JSON_VALUE(product.images, '$[0]') AS images " +
                "FROM product " +
                "INNER JOIN product_category " +
                "ON product.id = product_category.product_id " +
                "WHERE product.status = 1 " +
                "AND product.brand_id IN (?1) " +
                "AND product_category.category_id IN (?2) " +
                "AND product.sale_price > ?3 " +
                "AND product.sale_price < ?4 " +
                "ORDER BY product.id " +
                "OFFSET ?6 ROWS FETCH NEXT ?5 ROWS ONLY"
)

@NamedNativeQuery(
        name = "searchProductByKeyword",
        resultSetMapping = "productInfoDto",
        query = "SELECT DISTINCT p.id, p.name, p.slug, p.sale_price AS price, p.product_view AS views, p.total_sold, " +
                "JSON_VALUE(p.images, '$[0]') AS images " +
                "FROM product p " +
                "INNER JOIN product_category pc " +
                "ON p.id = pc.product_id " +
                "INNER JOIN category c " +
                "ON c.id = pc.category_id " +
                "WHERE p.status = 1 " +
                "AND (p.name LIKE '%' + :keyword + '%' OR c.name LIKE '%' + :keyword + '%') " +
                "ORDER BY p.id " +
                "OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY"
)

@NamedNativeQuery(
        name = "getProductOrders",
        resultSetMapping = "chartProductDTO",
        query = "SELECT p.name AS label, SUM(o.quantity) AS value " +
                "FROM product p " +
                "INNER JOIN orders o ON p.id = o.product_id " +
                "WHERE o.status = 3 AND MONTH(o.created_at) = ?1 " +
                "AND YEAR(o.created_at) = ?2 " +
                "GROUP BY p.id, p.name " +
                "ORDER BY SUM(o.quantity) DESC"
)


@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "product")
public class Product {
    @Id
    @Column(name = "id")
    private String id;
    @Column(name = "name",nullable = false,length = 300)
    private String name;
    @Column(name = "description",columnDefinition = "TEXT")
    private String description;
    @Column(name = "price")
    private long price;
    @Column(name = "sale_price")
    private long salePrice;
    @Column(name = "slug",nullable = false)
    private String slug;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "images",columnDefinition = "json")
    private ArrayList<String> images;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_feedback",columnDefinition = "json")
    private ArrayList<String> imageFeedBack;
    @Column(name = "product_view")
    private int view;
    @Column(name = "total_sold")
    private long totalSold;
    @Column(name = "status")
    private int status;
    @Column(name = "created_at")
    private Timestamp createdAt;
    @Column(name = "modified_at")
    private Timestamp modifiedAt;

    @ManyToOne
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToMany
    @JoinTable(
            name = "product_category",
            joinColumns =@JoinColumn(name = "product_id"),
            inverseJoinColumns =@JoinColumn(name = "category_id")
    )
    private List<Category> categories;

    @OneToMany(mappedBy = "product")
    private List<Comment> comments;
}
