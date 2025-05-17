package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.CategoryRequest;
import com.fibiyo.ecommerce.application.dto.CategoryResponse;
import com.fibiyo.ecommerce.domain.entity.Category;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-17T12:02:50+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250508-0718, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class CategoryMapperImpl implements CategoryMapper {

    @Override
    public CategoryResponse toCategoryResponse(Category category) {
        if ( category == null ) {
            return null;
        }

        CategoryResponse categoryResponse = new CategoryResponse();

        categoryResponse.setParentCategoryId( categoryParentCategoryId( category ) );
        categoryResponse.setParentCategoryName( categoryParentCategoryName( category ) );
        categoryResponse.setActive( category.isActive() );
        categoryResponse.setDescription( category.getDescription() );
        categoryResponse.setId( category.getId() );
        categoryResponse.setImageUrl( category.getImageUrl() );
        categoryResponse.setName( category.getName() );
        categoryResponse.setSlug( category.getSlug() );

        return categoryResponse;
    }

    @Override
    public List<CategoryResponse> toCategoryResponseList(List<Category> categories) {
        if ( categories == null ) {
            return null;
        }

        List<CategoryResponse> list = new ArrayList<CategoryResponse>( categories.size() );
        for ( Category category : categories ) {
            list.add( toCategoryResponse( category ) );
        }

        return list;
    }

    @Override
    public Category toCategory(CategoryRequest categoryRequest) {
        if ( categoryRequest == null ) {
            return null;
        }

        Category category = new Category();

        category.setDescription( categoryRequest.getDescription() );
        category.setImageUrl( categoryRequest.getImageUrl() );
        category.setName( categoryRequest.getName() );

        return category;
    }

    @Override
    public void updateCategoryFromRequest(CategoryRequest request, Category category) {
        if ( request == null ) {
            return;
        }

        if ( request.getDescription() != null ) {
            category.setDescription( request.getDescription() );
        }
        if ( request.getImageUrl() != null ) {
            category.setImageUrl( request.getImageUrl() );
        }
        if ( request.getName() != null ) {
            category.setName( request.getName() );
        }
    }

    private Long categoryParentCategoryId(Category category) {
        if ( category == null ) {
            return null;
        }
        Category parentCategory = category.getParentCategory();
        if ( parentCategory == null ) {
            return null;
        }
        Long id = parentCategory.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String categoryParentCategoryName(Category category) {
        if ( category == null ) {
            return null;
        }
        Category parentCategory = category.getParentCategory();
        if ( parentCategory == null ) {
            return null;
        }
        String name = parentCategory.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
