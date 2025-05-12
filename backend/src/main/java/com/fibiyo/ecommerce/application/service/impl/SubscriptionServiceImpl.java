package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.dto.StripeCheckoutSessionResponse;
import com.fibiyo.ecommerce.application.dto.SubscriptionStatusResponse;
import com.fibiyo.ecommerce.application.exception.BadRequestException;
import com.fibiyo.ecommerce.application.exception.ForbiddenException;
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException;
import com.fibiyo.ecommerce.application.service.NotificationService; // Bildirim için
import com.fibiyo.ecommerce.application.service.SubscriptionService;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.domain.enums.NotificationType;
import com.fibiyo.ecommerce.domain.enums.SubscriptionType;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session; // Stripe Session
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled; // Periyodik görev için
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal; // Fiyatlar için
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit; // Süre ekleme için
import java.util.HashMap; // Fiyat map'i için
import java.util.List;
import java.util.Map; // Fiyat map'i için

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // Sabit Fiyatlar (Daha dinamik bir yapı için DB'den okunabilir)
    private static final Map<SubscriptionType, Long> SUBSCRIPTION_PRICES_CENTS = new HashMap<>();
    static {
        // Fiyatları KURUŞ cinsinden belirle (Stripe cent/kuruş bekler)
         SUBSCRIPTION_PRICES_CENTS.put(SubscriptionType.CUSTOMER_PLUS, 9990L); // 99.90 TL
         SUBSCRIPTION_PRICES_CENTS.put(SubscriptionType.SELLER_PLUS, 29990L); // 299.90 TL
         // FREE için fiyat 0
    }
     // Abonelik Süreleri (Gün)
      private static final Map<SubscriptionType, Long> SUBSCRIPTION_DURATIONS_DAYS = new HashMap<>();
     static {
         SUBSCRIPTION_DURATIONS_DAYS.put(SubscriptionType.CUSTOMER_PLUS, 30L); // 30 gün
         SUBSCRIPTION_DURATIONS_DAYS.put(SubscriptionType.SELLER_PLUS, 30L); // 30 gün
      }

      // TODO: Bu sabitleri konfigürasyon dosyasına veya veritabanına taşımak daha iyi olur.

    @Autowired
    public SubscriptionServiceImpl(UserRepository userRepository, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // Helper
    private User getCurrentUser() {
         String username = SecurityContextHolder.getContext().getAuthentication().getName();
         if ("anonymousUser".equals(username)) throw new ForbiddenException("Login required.");
         return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("Current user not found."));
    }


    @Override
    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getMySubscriptionStatus() {
        User currentUser = getCurrentUser();
        logger.debug("Fetching subscription status for User ID: {}", currentUser.getId());
        boolean isActive = currentUser.getSubscriptionType() != SubscriptionType.FREE &&
                            (currentUser.getSubscriptionExpiryDate() == null || currentUser.getSubscriptionExpiryDate().isAfter(LocalDateTime.now()));
        return new SubscriptionStatusResponse(
                currentUser.getSubscriptionType(),
                currentUser.getSubscriptionExpiryDate(),
                isActive
        );
    }

    @Override
    // Bu kısım transactional değil, sadece Stripe'a istek atar. Aktivasyon webhook ile transactional olur.
    public StripeCheckoutSessionResponse createSubscriptionCheckoutSession(SubscriptionType targetSubscription) {
        User currentUser = getCurrentUser();
        logger.info("User ID: {} creating Stripe Checkout Session for Subscription Type: {}", currentUser.getId(), targetSubscription);

        // Geçerli abonelik tipleri
        if (targetSubscription == SubscriptionType.FREE || targetSubscription == null) {
            throw new BadRequestException("Geçersiz abonelik tipi seçildi.");
        }
        // Zaten bu aboneliğe sahip mi veya daha iyisine?
        // if (currentUser.getSubscriptionType() == targetSubscription || (currentUser.getSubscriptionType() == SubscriptionType.SELLER_PLUS && targetSubscription == SubscriptionType.CUSTOMER_PLUS)) {
         //   throw new BadRequestException("Zaten bu veya daha üst bir aboneliğe sahipsiniz.");
        //}

         Long priceCents = SUBSCRIPTION_PRICES_CENTS.get(targetSubscription);
         if (priceCents == null || priceCents <= 0) {
             logger.error("Price not found or invalid for subscription type: {}", targetSubscription);
              throw new RuntimeException("Abonelik fiyatı bulunamadı."); // Konfigürasyon hatası
          }

          // Basitlik için ödeme periyodu şimdilik yok, tek seferlik gibi işlem başlatalım.
         // Gerçek abonelik (Recurring) için Stripe Subscriptions API kullanılmalı!
         // https://stripe.com/docs/billing/subscriptions/checkout
         // Bu örnekte Checkout'un PAYMENT mode'unu kullanacağız, webhook gelince süreyi biz ayarlayacağız.
        try {
            // TODO: Frontend success/cancel URL'lerini request ile almak daha iyi olur.
            String successUrl = "http://localhost:4200/profile?subscription=success";
             String cancelUrl = "http://localhost:4200/profile?subscription=cancel";

             SessionCreateParams params = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                     .setMode(SessionCreateParams.Mode.PAYMENT) // Recurring için .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(successUrl)
                     .setCancelUrl(cancelUrl)
                     .setLocale(SessionCreateParams.Locale.TR)
                     .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                     .setQuantity(1L)
                                     .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                              .setCurrency("try")
                                             .setUnitAmount(priceCents) // Kuruş cinsinden fiyat
                                              .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                       .setName("Fibiyo " + targetSubscription.name() + " Abonelik")
                                                       .build())
                                             // Recurring için: .setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder().setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH).build())
                                              .build())
                                     .build()
                     )
                      // Metadata'ya kullanıcı ID ve hedef abonelik tipini ekle (Webhook için)
                     .putMetadata("user_id", currentUser.getId().toString())
                      .putMetadata("target_subscription", targetSubscription.name())
                     // Payment Intent Metadata (Çok önemli!)
                      .setPaymentIntentData(
                            SessionCreateParams.PaymentIntentData.builder()
                                     .putMetadata("user_id", currentUser.getId().toString())
                                     .putMetadata("target_subscription", targetSubscription.name())
                                    .build())
                     .setCustomerEmail(currentUser.getEmail())
                     .build();

            Session session = Session.create(params);
             logger.info("Stripe Checkout Session created for Subscription: {}. Session ID: {}", targetSubscription, session.getId());

             return new StripeCheckoutSessionResponse(session.getId(), session.getUrl());

         } catch (StripeException e) {
             logger.error("Error creating Stripe session for Subscription {}: {}", targetSubscription, e.getMessage(), e);
              throw new RuntimeException("Abonelik ödeme oturumu oluşturulamadı: " + e.getMessage(), e);
        }
     }

    @Override
    @Transactional // Kullanıcıyı güncelleme işlemi var
    public void activateSubscriptionFromPayment(String stripeObjectId, Long userId, SubscriptionType subscriptionType) {
        // Bu metod webhook tarafından çağrılır (veya ödeme servisi bunu çağırır).
        // İmza doğrulamasının webhook endpointinde yapıldığı varsayılır.
         logger.info("Activating subscription {} for User ID: {} based on Stripe object ID: {}",
                 subscriptionType, userId, stripeObjectId);

         User user = userRepository.findById(userId).orElse(null);
         if (user == null) {
             logger.error("[SUBSCRIPTION-ERROR] User not found with ID {} during subscription activation (Stripe Object ID: {}).", userId, stripeObjectId);
              // Kritik durum, logla.
             return;
         }

         // Abonelik tipini ve geçerlilik tarihini ayarla
         // Mevcut aboneliğe gün eklenmeli mi, yoksa başlangıçtan mı hesaplanmalı? Şimdilik başlangıçtan hesaplayalım.
         Long durationDays = SUBSCRIPTION_DURATIONS_DAYS.get(subscriptionType);
         if(durationDays == null) {
             logger.error("[SUBSCRIPTION-ERROR] Duration not found for subscription type: {}", subscriptionType);
              return; // Konfigürasyon hatası
         }

         LocalDateTime now = LocalDateTime.now();
         LocalDateTime expiryDate = now.plusDays(durationDays); // Süreyi ekle

         // Kullanıcının mevcut abonelik durumunu logla (güncelleme öncesi)
          logger.info("Updating User ID: {} subscription from {} (expires: {}) to {} (expires: {})",
                  userId, user.getSubscriptionType(), user.getSubscriptionExpiryDate(), subscriptionType, expiryDate);


         user.setSubscriptionType(subscriptionType);
         user.setSubscriptionExpiryDate(expiryDate);

         // Abonelik türüne göre özel güncellemeler (örn: kota resetleme)
         if (subscriptionType == SubscriptionType.SELLER_PLUS) {
             // TODO: Satıcı Plus aboneliği avantajlarını uygula (örn: imageGenQuota'yı artır?)
              user.setImageGenQuota(100); // Örnek kota
             logger.info("Set image generation quota to {} for Seller Plus User ID: {}", user.getImageGenQuota(), userId);
         } else if (subscriptionType == SubscriptionType.CUSTOMER_PLUS){
             // TODO: Müşteri Plus aboneliği avantajlarını uygula
         }


         userRepository.save(user);
          logger.info("[SUBSCRIPTION-SUCCESS] User ID: {} subscription successfully activated/updated to {} until {}",
                  userId, subscriptionType, expiryDate);

        // Kullanıcıya bildirim gönder
         try {
            notificationService.createNotification(user,
                   "Fibiyo " + subscriptionType.name() + " aboneliğiniz başarıyla başlatıldı. Geçerlilik tarihi: " + expiryDate.toLocalDate(),
                    "/profile", // Profil sayfasına link
                   NotificationType.SUBSCRIPTION_UPDATE);
        } catch (Exception e){
              logger.error("[SUBSCRIPTION-ERROR] Failed to send subscription activation notification for User ID {}: {}", userId, e.getMessage());
         }
    }


    @Override
    @Scheduled(cron = "0 0 3 * * ?") // Her gece saat 03:00'da çalışır (Cron expression)
    @Transactional // Toplu güncelleme transactional olmalı
    public void checkAndExpireSubscriptions() {
         LocalDateTime now = LocalDateTime.now();
         logger.info("Running scheduled task: Checking for expired subscriptions at {}", now);

        // Süresi dolmuş ve FREE olmayan abonelikleri bul
        List<User> expiredUsers = userRepository.findBySubscriptionTypeNotAndSubscriptionExpiryDateBefore(SubscriptionType.FREE, now);

        if (!expiredUsers.isEmpty()) {
            logger.warn("Found {} expired subscriptions to downgrade.", expiredUsers.size());
             for (User user : expiredUsers) {
                 SubscriptionType oldType = user.getSubscriptionType();
                 LocalDateTime oldExpiry = user.getSubscriptionExpiryDate();

                // FREE'ye düşür
                 user.setSubscriptionType(SubscriptionType.FREE);
                 user.setSubscriptionExpiryDate(null);
                 // Avantajları geri al (örn: kotayı düşür)
                  if(oldType == SubscriptionType.SELLER_PLUS) {
                     user.setImageGenQuota(3); // Varsayılan kotaya dön
                 }
                  //... diğer avantajları geri alma

                  userRepository.save(user);
                  logger.info("Downgraded subscription for User ID: {} from {} (Expired: {}) to FREE.",
                         user.getId(), oldType, oldExpiry);

                  // Kullanıcıya bildirim gönder
                  try{
                      notificationService.createNotification(user,
                              "Fibiyo " + oldType.name() + " aboneliğinizin süresi dolduğu için FREE plana geçiş yapıldı.",
                              "/profile",
                             NotificationType.SUBSCRIPTION_UPDATE);
                 } catch (Exception e){
                       logger.error("Failed to send subscription expiry notification for User ID {}: {}", user.getId(), e.getMessage());
                   }
              }
         } else {
            logger.info("No expired subscriptions found to downgrade.");
        }
    }


    // TODO: Abonelik iptali (cancelMySubscription) mantığı eklenecek.
     // Bu, ya expiry date'i değiştirmez sadece bir flag set eder ya da
     // Stripe Subscriptions API ile iptal edilir ve webhook ile DB güncellenir.

}