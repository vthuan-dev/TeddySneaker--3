package com.web.application.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.web.application.config.Contant;
import com.web.application.entity.Category;
import com.web.application.exception.BadRequestExp;
import com.web.application.exception.InternalServerExp;
import com.web.application.exception.NotFoundExp;
import com.web.application.model.mapper.CategoryMapper;
import com.web.application.model.request.CreateCategoryRequest;
import com.web.application.repository.CategoryRepository;
import com.web.application.service.CategoryService;

import java.sql.Timestamp;
import java.util.*;

@Component
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public Page<Category> adminGetListCategory(String id, String name, String status, String createAt, int page) {
        page--;
        if (page <= 0) {
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, Contant.LIMIT_CATEGORY, Sort.by("created_at").descending());
        return categoryRepository.adminGetListCategory(id, name, status, createAt, pageable);
    }

    @Override
    public void updateOrderCategory(int[] ids) {

        for (int id: ids){
            Optional<Category> category = categoryRepository.findById((long) id);
            categoryRepository.save(category.get());
        }
    }

    @Override
    public long getCountCategories() {
        return categoryRepository.count();
    }

    @Override
    public List<Category> getListCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category getCategoryById(long id) {
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isEmpty()) {
            throw new NotFoundExp("Danh mục không tồn tại!");
        }
        return category.get();
    }

    @Override
    public Category createCategory(CreateCategoryRequest createCategoryRequest) {
        Category category = categoryRepository.findByName(createCategoryRequest.getName());
        if (category != null) {
            throw new BadRequestExp("Tên danh mục đã tồn tại trong hệ thống. Vui lòng chọn tên khác!");
        }
        category = CategoryMapper.toCategory(createCategoryRequest);
        categoryRepository.save(category);
        return category;
    }

    @Override
    public void updateCategory(CreateCategoryRequest createCategoryRequest, long id) {
        Optional<Category> result = categoryRepository.findById(id);
        if (result.isEmpty()) {
            throw new NotFoundExp("Danh mục không tồn tại!");
        }
        Category category = result.get();
        category.setName(createCategoryRequest.getName());
        category.setStatus(createCategoryRequest.isStatus());
        category.setModifiedAt(new Timestamp(System.currentTimeMillis()));
        try {
            categoryRepository.save(category);
        } catch (Exception e) {
            throw new InternalServerExp("Lỗi khi chỉnh sửa danh mục");
        }
    }

    @Override
    public void deleteCategory(long id) {
        Optional<Category> result = categoryRepository.findById(id);
        if (result.isEmpty()) {
            throw new NotFoundExp("Danh mục không tồn tại!");
        }

        //Check product in category
        long count = categoryRepository.checkProductInCategory(id);
        if (count > 0) {
            throw new BadRequestExp("Có sản phẩm thuộc danh mục không thể xóa!");
        }

        try {
            categoryRepository.deleteById(id);
        } catch (Exception ex) {
            throw new InternalServerExp("Lỗi khi xóa danh mục!");
        }
    }


}
