package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.dto.CategoryRequest;
import com.fibiyo.ecommerce.application.dto.CategoryResponse;
import com.fibiyo.ecommerce.application.exception.BadRequestException;
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException; // Bunu da oluşturmak lazım
import com.fibiyo.ecommerce.application.mapper.CategoryMapper; // Mapper'ı inject et
import com.fibiyo.ecommerce.application.service.CategoryService;
import com.fibiyo.ecommerce.application.util.SlugUtils; // Slug helper
import com.fibiyo.ecommerce.domain.entity.Category;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper; // Inject the mapper

    @Autowired
    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAllActiveCategories() {
        logger.debug("Fetching all active categories");
        List<Category> categories = categoryRepository.findAllActive(); // Repository'deki custom query'yi kullan
        return categoryMapper.toCategoryResponseList(categories);
    }

     @Override
    @Transactional(readOnly = true)
    public CategoryResponse findCategoryById(Long id) {
         logger.debug("Fetching category by ID: {}", id);
         Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
         return categoryMapper.toCategoryResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse findCategoryBySlug(String slug) {
         logger.debug("Fetching category by slug: {}", slug);
         Category category = categoryRepository.findBySlug(slug)
                 .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        // Aktif olmayanları göstermek istemeyebiliriz. İsteğe bağlı kontrol:
         // if (!category.isActive()) { throw new ResourceNotFoundException(...); }
         return categoryMapper.toCategoryResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {
        logger.info("Attempting to create category with name: {}", categoryRequest.getName());

        String slug = SlugUtils.toSlug(categoryRequest.getName());
        if (categoryRepository.existsBySlug(slug)) {
             logger.warn("Category slug '{}' already exists.", slug);
             throw new BadRequestException("Bu isimle veya benzer bir isimle kategori zaten mevcut.");
        }

        Category category = categoryMapper.toCategory(categoryRequest);
        category.setSlug(slug); // Oluşturulan slug'ı set et

        // Üst kategori varsa kontrol et ve set et
        if (categoryRequest.getParentCategoryId() != null) {
            Category parentCategory = categoryRepository.findById(categoryRequest.getParentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + categoryRequest.getParentCategoryId()));
            category.setParentCategory(parentCategory);
        }

        Category savedCategory = categoryRepository.save(category);
        logger.info("Category '{}' created successfully with ID: {}", savedCategory.getName(), savedCategory.getId());
        return categoryMapper.toCategoryResponse(savedCategory);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest) {
        logger.info("Attempting to update category with ID: {}", id);
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // İsim değişiyorsa slug'ı yeniden oluştur ve unique'liğini kontrol et
        if (!existingCategory.getName().equalsIgnoreCase(categoryRequest.getName())) {
            String newSlug = SlugUtils.toSlug(categoryRequest.getName());
            if (!newSlug.equals(existingCategory.getSlug()) && categoryRepository.existsBySlug(newSlug)) {
                 logger.warn("New category slug '{}' already exists.", newSlug);
                 throw new BadRequestException("Bu isimle veya benzer bir isimle başka bir kategori zaten mevcut.");
             }
             existingCategory.setSlug(newSlug); // Yeni slug'ı ata
         }

        // Mapper ile diğer alanları güncelle (Null değerleri ignore eder)
        categoryMapper.updateCategoryFromRequest(categoryRequest, existingCategory);

         // Üst kategoriyi güncelle/ayarla
        if (categoryRequest.getParentCategoryId() != null) {
             if (!categoryRequest.getParentCategoryId().equals(existingCategory.getParentCategory() != null ? existingCategory.getParentCategory().getId() : null)) {
                Category parentCategory = categoryRepository.findById(categoryRequest.getParentCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + categoryRequest.getParentCategoryId()));
                 // Kendi kendine parent olmasını engelle
                 if (parentCategory.getId().equals(existingCategory.getId())) {
                     throw new BadRequestException("Bir kategori kendisinin üst kategorisi olamaz.");
                 }
                 existingCategory.setParentCategory(parentCategory);
             }
         } else {
             existingCategory.setParentCategory(null); // Parent kaldırıldıysa null yap
         }

        Category updatedCategory = categoryRepository.save(existingCategory);
        logger.info("Category with ID: {} updated successfully.", updatedCategory.getId());
        return categoryMapper.toCategoryResponse(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
         logger.warn("Attempting to delete category with ID: {}. This might affect products.", id);
         Category category = categoryRepository.findById(id)
                 .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Kategori silinince ürünlerin category_id'si SET NULL olacak (schema ve Product entity ayarı)
        // Eğer alt kategorileri de silmek gerekiyorsa ek mantık eklenebilir.
        categoryRepository.delete(category);
        logger.info("Category with ID: {} deleted successfully.", id);
    }

     @Override
    @Transactional(readOnly = true)
     public List<CategoryResponse> findSubCategories(Long parentId) {
         logger.debug("Fetching subcategories for parent ID: {}", parentId);
         List<Category> subCategories = categoryRepository.findByParentCategoryId(parentId);
         return categoryMapper.toCategoryResponseList(subCategories);
     }

     @Override
    @Transactional(readOnly = true)
     public List<CategoryResponse> findRootCategories() {
         logger.debug("Fetching root categories");
         List<Category> rootCategories = categoryRepository.findByParentCategoryIsNull();
         return categoryMapper.toCategoryResponseList(rootCategories);
     }
}