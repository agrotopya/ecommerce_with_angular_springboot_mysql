package com.fibiyo.ecommerce.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Kategori adı boş olamaz")
    @Size(max = 255)
    private String name;

    @Size(max = 255) // Slug frontend'den gelmeyecek, backend'de oluşturulacak.
    // private String slug;

    private String description;

    private String imageUrl;

    private Long parentCategoryId; // Üst kategori ID'si (opsiyonel)

    @NotNull // Aktiflik durumu belirtilmeli
    private Boolean isActive = true; // Varsayılan
}