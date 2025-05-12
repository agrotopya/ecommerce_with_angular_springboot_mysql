package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.CreateCheckoutSessionRequest;
import com.fibiyo.ecommerce.application.dto.StripeCheckoutSessionResponse;
// Gerekirse diğer DTO importları (örn: RefundRequest, RefundResponse)

/**
 * Ödeme işlemleriyle ilgili servis arayüzü.
 */
public interface PaymentService {

    /**
     * Belirli bir sipariş için Stripe Checkout Session oluşturur.
     * Frontend bu session ID'si ile Stripe Checkout'a yönlendirme yapar.
     *
     * @param request Sipariş ID'sini ve yönlendirme URL'lerini içeren istek.
     * @return Oluşturulan Stripe session bilgilerini içeren yanıt.
     * @throws ResourceNotFoundException Sipariş bulunamazsa.
     * @throws ForbiddenException        Kullanıcının sipariş için yetkisi yoksa.
     * @throws BadRequestException       Sipariş durumu ödeme için uygun değilse veya Stripe API hatası olursa.
     */
    StripeCheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request);

    /**
     * Stripe'dan gelen webhook event'lerini işler.
     * İmza doğrulaması yapar ve event tipine göre sipariş durumunu günceller.
     * (Örn: checkout.session.completed, payment_intent.payment_failed)
     *
     * @param payload   Stripe'dan gelen ham request body'si (JSON string).
     * @param sigHeader Stripe'ın gönderdiği 'Stripe-Signature' header değeri.
     * @throws BadRequestException İmza geçersizse veya payload hatalıysa.
     * @throws RuntimeException    Beklenmedik bir hata oluşursa.
     */
    void handleStripeWebhook(String payload, String sigHeader);


    /**
     * Belirli bir ödeme için iade işlemi başlatır (İleride eklenecek).
     *
     * @param paymentId Veya orderId/transactionId olabilir.
     * @param reason    İade sebebi.
     * @param amount    İade edilecek tutar (kısmi iade için).
     */
    // void initiateRefund(Long paymentId, String reason, BigDecimal amount);

}