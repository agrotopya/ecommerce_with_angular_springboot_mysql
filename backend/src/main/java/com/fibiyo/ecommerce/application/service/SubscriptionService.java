package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.StripeCheckoutSessionResponse; // Ödeme session yanıtı
import com.fibiyo.ecommerce.application.dto.SubscriptionStatusResponse;
import com.fibiyo.ecommerce.domain.enums.SubscriptionType;

/**
 * Kullanıcı aboneliklerini yöneten servis.
 */
public interface SubscriptionService {

    /**
     * Mevcut kullanıcının abonelik durumunu getirir.
     */
    SubscriptionStatusResponse getMySubscriptionStatus();

    /**
     * Belirtilen abonelik tipi için Stripe ödeme oturumu oluşturur.
     * Başarılı ödeme sonrası webhook ile abonelik aktive edilir.
     *
     * @param targetSubscription Hedeflenen abonelik tipi (CUSTOMER_PLUS veya SELLER_PLUS).
     * @return Ödeme başlatmak için Stripe session bilgileri.
     */
    StripeCheckoutSessionResponse createSubscriptionCheckoutSession(SubscriptionType targetSubscription);

    /**
     * Stripe webhook'undan gelen başarılı abonelik ödemesi sonrası kullanıcı aboneliğini aktive/günceller.
     * Bu metod PaymentService.handleStripeWebhook içinden çağrılabilir veya ayrı bir webhook endpoint'i olabilir.
     *
     * @param stripeSessionId Stripe Checkout Session ID'si (veya PaymentIntent ID'si).
     * @param userId          Aboneliği güncellenecek kullanıcının ID'si (Metadata'dan alınır).
     * @param subscriptionType Satın alınan abonelik tipi (Metadata'dan veya session'dan alınır).
     */
    void activateSubscriptionFromPayment(String stripeSessionId, Long userId, SubscriptionType subscriptionType);

    /**
     * Kullanıcının aboneliğini iptal eder (süresi dolana kadar devam eder veya hemen biter - kurala bağlı).
     * (Bu genellikle Stripe Dashboard veya ayrı bir metod ile yapılır.)
     */
     // void cancelMySubscription();

     /**
     * Abonelik süresi dolmuş kullanıcıları kontrol edip FREE'ye düşüren periyodik görev (Scheduled Task).
      */
     void checkAndExpireSubscriptions(); // @Scheduled ile çalıştırılır
 }