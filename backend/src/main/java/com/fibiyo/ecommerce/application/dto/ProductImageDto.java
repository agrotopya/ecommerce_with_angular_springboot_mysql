package com.fibiyo.ecommerce.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDto {
    private Long id;
    private String imageUrl;
    private String altText;
    private boolean isPrimary;
    private int displayOrder;
    private LocalDateTime uploadedAt;
    // product_id genellikle DTO'da direkt olarak gönderilmez,
    // ürünün bir parçası olarak veya ayrı bir yükleme isteğinde belirtilir.
}
