package com.fibiyo.ecommerce.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetAiImageAsProductRequest {

    @NotNull(message = "Ürün ID boş olamaz")
    private Long productId;

    // Kullanıcı tarafından seçilen AI görselinin URL'si
    @NotBlank(message = "Kullanılacak AI görsel URL'si boş olamaz")
    @Size(max = 1024, message = "Görsel URL'si çok uzun") // URL sınırı
    private String aiImageUrl;
}