package com.fibiyo.ecommerce.application.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "Ürün adı boş olamaz")
    @Size(max = 255)
    private String name;

    private String description; // Null olabilir

    @NotNull(message = "Fiyat boş olamaz")
    @DecimalMin(value = "0.01", message = "Fiyat en az 0.01 olmalıdır")
    @Digits(integer = 10, fraction = 2, message = "Fiyat formatı geçersiz")
    private BigDecimal price;

    @NotNull(message = "Stok miktarı boş olamaz")
    @Min(value = 0, message = "Stok miktarı negatif olamaz")
    private Integer stock;

    @Size(max = 100, message = "SKU en fazla 100 karakter olabilir")
    private String sku; // Opsiyonel

    @Size(max = 1024, message = "Görsel URL'si en fazla 1024 karakter olabilir")
    private String imageUrl; // Opsiyonel

    @NotNull(message = "Kategori ID boş olamaz")
    private Long categoryId;

    // Seller ID genellikle SecurityContext'ten alınır, request'te gelmez.
    // isActive, isApproved gibi durumlar Admin veya Seller servisleri tarafından yönetilir.
}