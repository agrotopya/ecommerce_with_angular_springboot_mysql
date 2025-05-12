package com.fibiyo.ecommerce.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String sku;
    private String mainImageUrl; // Ana görsel URL'si
    private List<ProductImageDto> productImages; // Diğer tüm görseller
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isApproved;
    private boolean isActive;
    private BigDecimal averageRating;
    private int reviewCount;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private Long sellerId;
    private String sellerUsername;
    private int salesCount;
    private String reviewSummaryAi;
    private String aiGeneratedImageUrl;
    private List<String> tags; // Product entity'sinde tags alanı varsa
    private List<ProductAttributeDto> attributes; // Product entity'sinde attributes alanı varsa
    private Boolean isFeatured; // Product entity'sinde isFeatured alanı varsa
}
