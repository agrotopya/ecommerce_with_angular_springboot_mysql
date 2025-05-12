package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.AiImageGenerationRequest;
import com.fibiyo.ecommerce.domain.entity.User;
import org.springframework.lang.NonNull; // Argümanların null olmamasını belirtmek iyi pratik
import java.util.List;


public interface AiService {
    List<String> generateProductImage(@NonNull AiImageGenerationRequest request, @NonNull User requestingUser);
    // String summarizeReviews(List<String> reviews); // Sonraki özellik
}


