package com.web.application.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.web.application.dto.ChartDTO;
import com.web.application.entity.Category;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Category findByName(String name);

    @Query(value = "SELECT count(category_id) FROM product_category WHERE category_id = ?1", nativeQuery = true)
    int checkProductInCategory(long id);

    @Query(value = "SELECT * FROM category c " +
            "WHERE c.id LIKE CONCAT('%',?1,'%') " +
            "AND c.name LIKE CONCAT('%',?2,'%') " +
            "AND c.status LIKE CONCAT('%',?3,'%')" +
            "AND c.created_at LIKE CONCAT('%',?4,'%')", nativeQuery = true)
    Page<Category> adminGetListCategory(String id, String name, String status, String createAt, Pageable pageable);

    @Query(name = "getProductOrderCategories",nativeQuery = true)
    List<ChartDTO> getListProductOrderCategories();

}
