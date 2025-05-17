package com.fibiyo.ecommerce.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductEnhancementRequestDto {

    @NotBlank(message = "Current product name cannot be blank.")
    @Size(min = 3, max = 255, message = "Product name must be between 3 and 255 characters.")
    private String currentName;

    @Size(max = 5000, message = "Product description cannot exceed 5000 characters.")
    private String currentDescription; // Opsiyonel olabilir

    private String categoryName; // Opsiyonel, Gemini'ye bağlam sağlamak için

    // İleride eklenebilir:
    // private Double currentPrice;
    // private List<String> currentKeywords;
}
