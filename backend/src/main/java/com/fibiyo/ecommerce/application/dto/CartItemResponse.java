package com.fibiyo.ecommerce.application.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CartItemResponse {
    private Long cartItemId; // CartItem ID'si
    private Integer quantity;
    private LocalDateTime addedAt;
    private Long productId;
    private String productName;
    private String productSlug;
    private BigDecimal productPrice;
    private Integer productStock; // Stoku göstermek frontend'de uyarı için iyi olabilir
    private String productImageUrl;
    private BigDecimal itemTotal; // quantity * productPrice
}