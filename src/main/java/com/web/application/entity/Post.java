package com.web.application.entity;

import java.sql.Timestamp;
import java.util.List;

import com.web.application.dto.PostDTO;

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
                        name = "postInfoDto",
                        classes = @ConstructorResult(
                                targetClass = PostDTO.class,
                                columns = {
                                        @ColumnResult(name = "id", type = Long.class),
                                        @ColumnResult(name = "slug", type = String.class),
                                        @ColumnResult(name = "title", type = String.class),
                                        @ColumnResult(name = "thumbnail",type = String.class),
                                        @ColumnResult(name = "created_time", type = String.class),
                                        @ColumnResult(name = "published_time", type = String.class),
                                        @ColumnResult(name = "status", type = String.class)
                                }
                        )
                )
        }
)
@NamedNativeQuery(
        name = "adminGetListPost",
        resultSetMapping = "postInfoDto",
        query = "SELECT id, slug, title, thumbnail, " +
                "FORMAT(created_at, 'dd/MM/yyyy HH:mm') AS created_time, " +
                "FORMAT(published_at, 'dd/MM/yyyy HH:mm') AS published_time, " +
                "(CASE WHEN status = true " +
                "THEN 'Công khai' " +
                "ELSE 'Nháp' " +
                "END ) as status " +
                "FROM post " +
                "WHERE title LIKE CONCAT('%',:title,'%') AND status LIKE CONCAT('%',:status,'%') " +
                "ORDER BY created_time DESC " +
                "LIMIT :limit OFFSET :offset "
)

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "post")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "title",nullable = false,length = 300)
    private String title;
    @Column(name = "content",columnDefinition = "TEXT")
    private String content;
    @Column(name = "description",columnDefinition = "TEXT")
    private String description;
    @Column(name = "slug",nullable = false,length = 600)
    private String slug;
    @Column(name = "thumbnail")
    private String thumbnail;
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
    @Column(name = "published_at")
    private Timestamp publishedAt;
    @Column(name = "status",columnDefinition = "int default 0")
    private int status;
    @OneToMany(mappedBy = "post")
    private List<Comment> comments;
}