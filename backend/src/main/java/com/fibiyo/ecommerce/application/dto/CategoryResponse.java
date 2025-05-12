package com.fibiyo.ecommerce.application.dto;

import lombok.Data;

@Data
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private boolean isActive;

    private Long parentCategoryId;
    private String parentCategoryName;
}

