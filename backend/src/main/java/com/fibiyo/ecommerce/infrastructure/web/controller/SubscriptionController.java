package com.fibiyo.ecommerce.infrastructure.web.controller;

import com.fibiyo.ecommerce.application.dto.StripeCheckoutSessionResponse;
import com.fibiyo.ecommerce.application.dto.SubscriptionStatusResponse;
import com.fibiyo.ecommerce.application.service.SubscriptionService;
import com.fibiyo.ecommerce.domain.enums.SubscriptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/subscriptions")
@PreAuthorize("isAuthenticated()") // Abonelik işlemleri login gerektirir
public class SubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    // Kullanıcının mevcut abonelik durumunu getir
    @GetMapping("/my-status")
    public ResponseEntity<SubscriptionStatusResponse> getMySubscriptionStatus() {
         logger.info("GET /api/subscriptions/my-status requested");
         SubscriptionStatusResponse status = subscriptionService.getMySubscriptionStatus();
         return ResponseEntity.ok(status);
    }

    // Abonelik başlatmak/yükseltmek için ödeme oturumu oluştur
    @PostMapping("/checkout-session")
    public ResponseEntity<StripeCheckoutSessionResponse> createSubscriptionCheckout(
            // RequestParam olarak alalım veya basit bir DTO
            @RequestParam SubscriptionType targetSubscription) {
         logger.info("POST /api/subscriptions/checkout-session requested for type: {}", targetSubscription);
         // Gerekirse burada user'ın current rolüne göre sadece SELLER_PLUS veya CUSTOMER_PLUS seçebilmesi kontrolü eklenebilir.
         StripeCheckoutSessionResponse response = subscriptionService.createSubscriptionCheckoutSession(targetSubscription);
         return ResponseEntity.ok(response);
     }

      // TODO: Abonelik iptali için endpoint eklenecek
      // @PostMapping("/cancel")
      // public ResponseEntity<?> cancelSubscription() { ... }

}