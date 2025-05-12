package com.fibiyo.ecommerce.application.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List; // List importu eklendi

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String sku;
    private String mainImageUrl; // imageUrl -> mainImageUrl
    private List<ProductImageDto> productImages; // Eklendi
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isApproved;
    private boolean isActive;
    private BigDecimal averageRating;
    private int reviewCount;
    private String reviewSummaryAi; // AI Özeti
    private String aiGeneratedImageUrl; // AI Görsel

    // İlişkili bilgiler
    private Long categoryId;
    private String categoryName; // Kategori Adı
    private String categorySlug; // Kategori Slug

    private Long sellerId;
    private String sellerUsername; // Satıcı Kullanıcı Adı
}
