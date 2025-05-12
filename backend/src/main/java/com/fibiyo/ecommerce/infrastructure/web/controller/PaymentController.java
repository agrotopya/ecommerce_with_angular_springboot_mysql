package com.fibiyo.ecommerce.infrastructure.web.controller;

import com.fibiyo.ecommerce.application.dto.ApiResponse; // Generic response
import com.fibiyo.ecommerce.application.dto.CreateCheckoutSessionRequest;
import com.fibiyo.ecommerce.application.dto.StripeCheckoutSessionResponse;
import com.fibiyo.ecommerce.application.exception.BadRequestException;
import com.fibiyo.ecommerce.application.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Stripe Checkout Session oluşturma endpoint'i (Login gerektirir)
    @PostMapping("/create-checkout-session")
    @PreAuthorize("isAuthenticated()") // Müşteri veya satıcı rolü fark etmez, login yeterli
    public ResponseEntity<StripeCheckoutSessionResponse> createCheckoutSession(
            @Valid @RequestBody CreateCheckoutSessionRequest request) {
        logger.info("POST /api/payments/create-checkout-session requested for order ID: {}", request.getOrderId());
        StripeCheckoutSessionResponse response = paymentService.createCheckoutSession(request);
        return ResponseEntity.ok(response);
    }


    // Stripe Webhook Endpoint'i (GÜVENLİK ÇOK ÖNEMLİ!)
    // Bu endpoint public olmalı ama İMZA DOĞRULAMASI ŞART!
    // Genellikle /webhook gibi daha belirgin bir path kullanılır.
    @PostMapping("/stripe/webhook")
    public ResponseEntity<ApiResponse> handleStripeWebhook(
            @RequestBody String payload, // Request body'yi String olarak al
            @RequestHeader("Stripe-Signature") String sigHeader) { // Stripe'ın gönderdiği header
        logger.info("Received Stripe Webhook request");
        try {
             paymentService.handleStripeWebhook(payload, sigHeader);
              return ResponseEntity.ok(new ApiResponse(true,"Webhook processed successfully.")); // Stripe'a 200 OK dönmek yeterli
          } catch (BadRequestException e) {
             logger.error("Webhook processing error (Bad Request): {}", e.getMessage());
               return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, e.getMessage()));
          } catch (Exception e) {
              logger.error("Webhook processing error (Internal Server Error): {}", e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(false, "Webhook processing failed."));
         }

    }

    // TODO: Refund endpoint (Admin)

}