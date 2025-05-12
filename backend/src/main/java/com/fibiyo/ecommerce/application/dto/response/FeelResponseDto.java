package com.fibiyo.ecommerce.application.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FeelResponseDto {

    private Long id;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private Long productId;
    private String productName;
    private String productSlug;
    private Long sellerId;
    private String sellerUsername; // Changed from sellerName to sellerUsername for consistency
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long views;
    private Long likes;
    private boolean isActive;
}

