package com.fibiyo.ecommerce.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCheckoutSessionRequest {
    @NotNull(message = "Sipariş ID boş olamaz")
    private Long orderId;
    // Frontend'e yönlendirme yapılacak başarı ve iptal URL'leri
    // Bunlar frontend'den de gelebilir veya backend'de sabit olabilir.
     private String successUrl = "http://localhost:4200/orders/success?session_id={CHECKOUT_SESSION_ID}"; // Frontend URL'i + placeholder
     private String cancelUrl = "http://localhost:4200/orders/cancel";
}