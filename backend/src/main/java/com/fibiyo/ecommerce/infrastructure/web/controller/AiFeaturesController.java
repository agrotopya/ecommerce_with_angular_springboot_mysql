package com.fibiyo.ecommerce.infrastructure.web.controller;

import com.fibiyo.ecommerce.application.dto.AiImageGenerationRequest;
import com.fibiyo.ecommerce.application.dto.AiImageGenerationResponse;
import com.fibiyo.ecommerce.application.dto.request.ProductEnhancementRequestDto;
import com.fibiyo.ecommerce.application.dto.response.ProductEnhancementResponseDto;
import com.fibiyo.ecommerce.application.service.AiService;
import com.fibiyo.ecommerce.domain.entity.User; // User import edildi
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // AuthenticationPrincipal import edildi
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiFeaturesController {

    private final AiService aiService;

    @PostMapping("/generate-image")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<AiImageGenerationResponse> generateImage(
            @Valid @RequestBody AiImageGenerationRequest request,
            @AuthenticationPrincipal User currentUser) { // İstek yapan kullanıcıyı al
        // AiService.generateProductImage metodu User parametresi bekliyordu.
        AiImageGenerationResponse response = aiService.generateProductImage(request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/enhance-product-details")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ProductEnhancementResponseDto> enhanceProductDetails(
            @Valid @RequestBody ProductEnhancementRequestDto requestDto,
            @AuthenticationPrincipal User currentUser) { // İstek yapan kullanıcıyı al
        // Yorum satırları kaldırıldı ve currentUser eklendi
        ProductEnhancementResponseDto response = aiService.getEnhancedProductDetails(requestDto, currentUser);
        return ResponseEntity.ok(response);
    }
}
