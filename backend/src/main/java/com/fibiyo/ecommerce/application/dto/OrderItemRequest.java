package com.fibiyo.ecommerce.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data // Siparişteki ürün kalemi isteği
public class OrderItemRequest {
    @NotNull(message = "Ürün ID boş olamaz")
    private Long productId;

    @NotNull(message = "Miktar boş olamaz")
    @Min(value = 1, message = "Miktar en az 1 olmalıdır")
    private Integer quantity;
}