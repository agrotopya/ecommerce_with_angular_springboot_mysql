package com.fibiyo.ecommerce.application.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CartResponse {
    private Long cartId;
    private Long userId;
    private LocalDateTime updatedAt;
    private List<CartItemResponse> items;
    private BigDecimal cartSubtotal; // Sepet ara toplamı
    private int totalItems; // Toplam ürün adedi (farklı ürün sayısı değil)
}