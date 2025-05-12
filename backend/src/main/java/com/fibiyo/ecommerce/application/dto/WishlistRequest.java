package com.fibiyo.ecommerce.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WishlistRequest {
    @NotNull(message = "Ürün ID boş olamaz")
    private Long productId;
}