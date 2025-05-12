package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.CategoryRequest;
import com.fibiyo.ecommerce.application.dto.CategoryResponse;
import com.fibiyo.ecommerce.domain.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget; // Güncelleme için
import org.mapstruct.NullValuePropertyMappingStrategy; // Null değerleri nasıl handle edeceğini belirtmek için

import java.util.List;

@Mapper(componentModel = "spring", // Spring bean olarak oluşturulsun
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE) // Request'teki null alanlar entity'deki mevcut değerleri ezmesin (update için)
public interface CategoryMapper {

    // Category -> CategoryResponse
    @Mapping(source = "parentCategory.id", target = "parentCategoryId")
    @Mapping(source = "parentCategory.name", target = "parentCategoryName") // Üst kategori adını map'le
    CategoryResponse toCategoryResponse(Category category);

    List<CategoryResponse> toCategoryResponseList(List<Category> categories);

    // CategoryRequest -> Category (Yeni kategori oluşturma)
    // Slug'ı request'ten almıyoruz, servis'te oluşturulacak.
    Category toCategory(CategoryRequest categoryRequest);

    // CategoryRequest'ten mevcut Category'yi güncelleme
    // Slug'ı güncelleme (gerekirse servis katmanında yapılmalı)
    void updateCategoryFromRequest(CategoryRequest request, @MappingTarget Category category);
}