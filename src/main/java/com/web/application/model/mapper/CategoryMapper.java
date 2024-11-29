package com.web.application.model.mapper;

import com.github.slugify.Slugify;
import com.web.application.dto.CategoryDTO;
import com.web.application.entity.Category;
import com.web.application.model.request.CreateCategoryRequest;

import java.sql.Timestamp;

public class CategoryMapper {
    public static CategoryDTO toCategoryDTO(Category category){
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId(category.getId());
        categoryDTO.setName(category.getName());
        categoryDTO.setStatus(category.isStatus());
        categoryDTO.setCreatedAt(category.getCreatedAt());
        return categoryDTO;
    }

    public static Category toCategory(CreateCategoryRequest createCategoryRequest){
        Category category = new Category();
        category.setName(createCategoryRequest.getName());
        category.setStatus(createCategoryRequest.isStatus());
        category.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        Slugify slug = new Slugify();
        category.setSlug(slug.slugify(createCategoryRequest.getName()));

        return category;
    }
}
