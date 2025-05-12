package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.CategoryRequest;
import com.fibiyo.ecommerce.application.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> findAllActiveCategories();
    CategoryResponse findCategoryById(Long id);
    CategoryResponse findCategoryBySlug(String slug);
    CategoryResponse createCategory(CategoryRequest categoryRequest);
    CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest);
    void deleteCategory(Long id); // Silme işlemi bir şey döndürmez genelde
    List<CategoryResponse> findSubCategories(Long parentId); // Alt kategorileri bulma
    List<CategoryResponse> findRootCategories(); // Ana kategorileri bulma
}