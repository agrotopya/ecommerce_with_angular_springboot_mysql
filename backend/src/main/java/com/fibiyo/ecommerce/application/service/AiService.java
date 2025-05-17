package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.AiImageGenerationRequest;
import com.fibiyo.ecommerce.application.dto.AiImageGenerationResponse; // Eklendi
import com.fibiyo.ecommerce.application.dto.request.ProductEnhancementRequestDto;
import com.fibiyo.ecommerce.application.dto.response.ProductEnhancementResponseDto;
import com.fibiyo.ecommerce.domain.entity.User;
import org.springframework.lang.NonNull;
// List importu zaten vardı, tekrar eklenmesine gerek yok.

public interface AiService {

    AiImageGenerationResponse generateProductImage(@NonNull AiImageGenerationRequest request, @NonNull User requestingUser); // Dönüş tipi güncellendi

    ProductEnhancementResponseDto getEnhancedProductDetails(@NonNull ProductEnhancementRequestDto requestDto, @NonNull User requestingUser);

    // String summarizeReviews(List<String> reviews); // Sonraki özellik
}
