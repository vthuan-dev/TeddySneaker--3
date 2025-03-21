package com.web.application.entity;

import java.sql.Timestamp;

import com.web.application.dto.ChartDTO;

import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
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
                        name = "chartCategoryDTO",
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
        name = "getProductOrderCategories",
        resultSetMapping = "chartCategoryDTO",
        query = "SELECT c.name AS label, COUNT(oi.quantity) AS value " +
                "FROM category c " +
                "INNER JOIN product_category pc ON pc.category_id = c.id " +
                "INNER JOIN product p ON p.id = pc.product_id " +
                "INNER JOIN order_items oi ON oi.product_id = p.id " +
                "INNER JOIN orders o ON o.id = oi.order_id " +
                "WHERE o.status = 3 " +
                "GROUP BY c.name"
)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "name",nullable = false,length = 300)
    private String name;
    @Column(name = "slug",nullable = false)
    private String slug;
    @Column(name = "status",columnDefinition = "BOOLEAN")
    private boolean status;
    @Column(name = "created_at")
    private Timestamp createdAt;
    @Column(name = "modified_at")
    private Timestamp modifiedAt;
}
