package com.fibiyo.ecommerce.infrastructure.web.controller;

import com.fibiyo.ecommerce.application.dto.ApiResponse;
import com.fibiyo.ecommerce.application.dto.CategoryRequest;
import com.fibiyo.ecommerce.application.dto.CategoryResponse;
import com.fibiyo.ecommerce.application.service.CategoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // MultipartFile importu eklendi

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // --- Herkese Açık Endpoint'ler ---

    @GetMapping("/active") // Sadece aktif kategorileri listele
    public ResponseEntity<List<CategoryResponse>> getActiveCategories() {
        logger.info("GET /api/categories/active requested");
        List<CategoryResponse> categories = categoryService.findAllActiveCategories();
        return ResponseEntity.ok(categories);
    }

     @GetMapping("/slug/{slug}") // Slug ile kategori bulma
     public ResponseEntity<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
         logger.info("GET /api/categories/slug/{} requested", slug);
         CategoryResponse category = categoryService.findCategoryBySlug(slug);
         return ResponseEntity.ok(category);
     }

    @GetMapping("/{id}") // ID ile kategori bulma (aktif/pasif fark etmez)
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
         logger.info("GET /api/categories/{} requested", id);
        CategoryResponse category = categoryService.findCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/{parentId}/subcategories") // Alt kategorileri listeleme
    public ResponseEntity<List<CategoryResponse>> getSubCategories(@PathVariable Long parentId) {
         logger.info("GET /api/categories/{}/subcategories requested", parentId);
        List<CategoryResponse> subCategories = categoryService.findSubCategories(parentId);
        return ResponseEntity.ok(subCategories);
    }

     @GetMapping("/roots") // Kök kategorileri listeleme
     public ResponseEntity<List<CategoryResponse>> getRootCategories() {
         logger.info("GET /api/categories/roots requested");
         List<CategoryResponse> rootCategories = categoryService.findRootCategories();
         return ResponseEntity.ok(rootCategories);
     }


    // --- Admin Yetkisi Gerektiren Endpoint'ler ---

    @PostMapping
    // @PreAuthorize("hasRole('ADMIN')") // Sadece ADMIN rolüne sahip kullanıcılar erişebilir
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest categoryRequest) {
        logger.info("POST /api/categories requested");
        CategoryResponse createdCategory = categoryService.createCategory(categoryRequest);
        return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest categoryRequest) {
         logger.info("PUT /api/categories/{} requested", id);
        CategoryResponse updatedCategory = categoryService.updateCategory(id, categoryRequest);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteCategory(@PathVariable Long id) {
         logger.warn("DELETE /api/categories/{} requested", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(new ApiResponse(true, "Kategori başarıyla silindi."));
    }

    @PostMapping("/{categoryId}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> uploadCategoryImage(
            @PathVariable Long categoryId,
            @RequestParam("file") MultipartFile file) {
        logger.info("POST /api/categories/{}/image - Uploading image for category", categoryId);
        // CategoryService'e yeni bir metod ekleyeceğiz: updateCategoryImage
        CategoryResponse updatedCategory = categoryService.updateCategoryImage(categoryId, file);
        return ResponseEntity.ok(updatedCategory);
    }

    // YENİ EKLENECEK METOD: Admin için tüm kategorileri sayfalama ve arama ile getirme
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CategoryResponse>> getAllCategoriesForAdmin(
            @RequestParam(name = "name", required = false) String name, // Arama terimi (kategori adı için)
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            // Gerekirse sıralama parametreleri de eklenebilir: @RequestParam(defaultValue = "id,asc") String[] sort
    ) {
        logger.info("GET /api/categories/admin/all requested with name: [{}], page: [{}], size: [{}]", name, page, size);
        // Pageable nesnesi oluştur (Sıralama eklemek isterseniz: PageRequest.of(page, size, Sort.by(sortParam)))
        Pageable pageable = PageRequest.of(page, size); 
        
        // CategoryService'inizde bu parametreleri alıp uygun veriyi çeken bir metod olmalı
        // Bu metod, CategoryRepository'nizi kullanarak arama ve sayfalama yapmalı.
        Page<CategoryResponse> categoriesPage = categoryService.findAllForAdmin(name, pageable); // Servis metodunuzun adını buna göre ayarlayın
        
        return ResponseEntity.ok(categoriesPage);
    }
}
