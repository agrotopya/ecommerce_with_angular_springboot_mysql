package com.fibiyo.ecommerce.application.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data // Sipariş kalem yanıtı
public class OrderItemResponse {
    private Long id; // OrderItem ID'si
    private Integer quantity;
    private BigDecimal priceAtPurchase; // Ürünün o anki fiyatı
    private BigDecimal itemTotal; // Miktar * fiyat
    private Long productId;
    private String productName; // Kolaylık için
    private String productSlug; // Kolaylık için
    private String productImageUrl; // Kolaylık için
}