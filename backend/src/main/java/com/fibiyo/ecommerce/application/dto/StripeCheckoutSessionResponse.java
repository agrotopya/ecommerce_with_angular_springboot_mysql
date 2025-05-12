package com.fibiyo.ecommerce.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StripeCheckoutSessionResponse {
    private String sessionId; // Stripe session ID
     private String checkoutUrl; // Direkt yönlendirme URL'i (opsiyonel)
}