package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.dto.CreateCheckoutSessionRequest;
import com.fibiyo.ecommerce.application.dto.StripeCheckoutSessionResponse;
import com.fibiyo.ecommerce.application.exception.BadRequestException;
import com.fibiyo.ecommerce.application.exception.ForbiddenException;
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException;
import com.fibiyo.ecommerce.application.service.EmailService;
import com.fibiyo.ecommerce.application.service.NotificationService; // Bildirim göndermek için
import com.fibiyo.ecommerce.application.service.PaymentService;
import com.fibiyo.ecommerce.application.service.SubscriptionService;
import com.fibiyo.ecommerce.domain.entity.*; // Tüm ilgili entity'ler
import com.fibiyo.ecommerce.domain.enums.NotificationType;
import com.fibiyo.ecommerce.domain.enums.OrderStatus;
import com.fibiyo.ecommerce.domain.enums.PaymentStatus;
import com.fibiyo.ecommerce.domain.enums.SubscriptionType;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.*; // Gerekli repository'ler
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*; // Stripe modelleri: Event, Session, PaymentIntent vb.
import com.stripe.model.checkout.Session;

import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository; // Sepeti temizlemek için
    private final CartItemRepository cartItemRepository; // Sepeti temizlemek için
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService; 
    private SubscriptionService subscriptionService;


    @Value("${stripe.webhook.secret}")
    private String endpointSecret; // Stripe webhook secret key


    @Autowired
    public PaymentServiceImpl(OrderRepository orderRepository,
                              PaymentRepository paymentRepository,
                              CartRepository cartRepository,
                              CartItemRepository cartItemRepository,
                              UserRepository userRepository,
                              NotificationService notificationService,
                              EmailService emailService,
                              SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    // Helper - Mevcut giriş yapmış kullanıcıyı getirir
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if ("anonymousUser".equals(username)) {
            throw new ForbiddenException("Bu işlem için giriş yapmalısınız.");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + username));
    }


    @Override
    // Tamamı transaction içinde olamaz çünkü Stripe API çağrısı dışarıda.
    // Ama DB okuma ve kontroller burada yapılır.
    public StripeCheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request) {
        User customer = getCurrentUser();
        Long orderId = request.getOrderId();
        logger.info("User ID: {} creating Stripe Checkout Session for Order ID: {}", customer.getId(), orderId);

        // @Transactional readOnly=true kullanmak yerine doğrudan repoyu çağırıyoruz
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Yetki Kontrolü
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("Bu sipariş için ödeme başlatma yetkiniz yok.");
        }

        // Durum Kontrolleri
        if (order.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new BadRequestException("Bu siparişin ödemesi zaten yapılmış.");
        }
         if (order.getStatus().name().startsWith("CANCELLED")) {
            throw new BadRequestException("İptal edilmiş bir sipariş için ödeme yapamazsınız.");
        }
         if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Sipariş ödeme için uygun durumda değil (Durum: " + order.getStatus() + ").");
        }
         if (order.getOrderItems() == null || order.getOrderItems().isEmpty()){
              throw new BadRequestException("Siparişte ödenecek ürün bulunmuyor.");
         }


               // ... (Metodun başındaki kodlar) ...
               BigDecimal finalAmount = order.getFinalAmount();
               if (finalAmount == null) {
                   throw new IllegalStateException("Order final amount is null for order ID: " + order.getId());
               }
               
               BigDecimal amountInCents = finalAmount.multiply(new BigDecimal("100"));
               
        // --- Stripe Session Oluşturma (Güncellenmiş SDK v20+ Kullanımı) ---
        try {
            // Siparişin son tutarını kuruş/cent'e çevir
            long finalAmountCents = order.getFinalAmount().multiply(BigDecimal.valueOf(100)).longValue();
            if (finalAmountCents <= 50) { // Stripe'ın minimum ödeme limiti olabilir (örn: 0.50 TRY/USD) Kontrol etmek lazım.
                logger.warn("Calculated amount ({}) is below Stripe minimum for order ID: {}", finalAmountCents/100.0, orderId);
                // Kullanıcıya minimum limit hatası döndürmek veya farklı bir akış izlemek gerekebilir.
                // Şimdilik Stripe API'sine bırakalım, API hata dönecektir.
             }

            SessionCreateParams params = SessionCreateParams.builder() // <--- Değişiklik burada
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(request.getSuccessUrl())
                .setCancelUrl(request.getCancelUrl())
                 // Locale set ederken enum kullanalım
                .setLocale(SessionCreateParams.Locale.TR)
                .addLineItem(
                     SessionCreateParams.LineItem.builder() // <--- Değişiklik burada
                        .setQuantity(1L)
                        .setPriceData(
                             SessionCreateParams.LineItem.PriceData.builder() // <--- Değişiklik burada
                                .setCurrency("try")
                                .setUnitAmount(finalAmountCents)
                                .setProductData(
                                     SessionCreateParams.LineItem.PriceData.ProductData.builder() // <--- Değişiklik burada
                                        .setName("Fibiyo Sipariş #" + order.getId())
                                         // .setDescription(...) // Opsiyonel
                                        .build()
                                )
                                .build()
                        )
                        .build()
                 )
                 // Metadata
                .putMetadata("order_id", order.getId().toString())
                 .putMetadata("customer_id", customer.getId().toString())
                 // Payment Intent Metadata
                .setPaymentIntentData(
                     SessionCreateParams.PaymentIntentData.builder() // <--- Değişiklik burada
                             .putMetadata("order_id", order.getId().toString())
                            .build()
                )
                 .setCustomerEmail(customer.getEmail())
                .build(); // <--- En sonda .build()

            Session session = Session.create(params);
            logger.info("Stripe Checkout Session created: {} for Order ID: {}", session.getId(), orderId);

            return new StripeCheckoutSessionResponse(session.getId(), session.getUrl());

         } catch (StripeException e) {
             String stripeErrorMessage = e.getMessage();
             if (e.getStripeError() != null && e.getStripeError().getMessage() != null) {
                 stripeErrorMessage = e.getStripeError().getMessage();
             }
             logger.error("Error creating Stripe session for Order ID: {}. Stripe Error Details: {} (Raw StripeException message: {})", orderId, stripeErrorMessage, e.getMessage(), e);
             throw new BadRequestException("Ödeme oturumu oluşturulamadı. Detay: " + stripeErrorMessage); // Hata mesajına Stripe detayını ekle
        } catch (Exception e){
             logger.error("Unexpected error during Stripe session creation for Order ID: {}", orderId, e);
             throw new RuntimeException("Ödeme oturumu oluşturulurken beklenmedik bir hata oluştu.");
         }
         
    }


    @Override
    @Transactional // İçeride DB işlemleri olduğu için Transactional olmalı
    public void handleStripeWebhook(String payload, String sigHeader) {
        Event event;
        final String webhookSecret = this.endpointSecret; // Final yapalım

        if (webhookSecret == null || webhookSecret.isBlank()) {
            logger.error("STRIPE_WEBHOOK_ERROR: Stripe webhook secret ('stripe.webhook.secret') is not configured!");
            throw new InternalError("Webhook secret configuration is missing.");
        }

        // 1. İmza Doğrulama
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            logger.error("STRIPE_WEBHOOK_SIGNATURE_ERROR: Webhook signature verification failed! IP: [Check Request Logs if needed]. Check webhook secret.", e);
            throw new BadRequestException("Invalid webhook signature.");
        } catch (Exception e) {
            logger.error("STRIPE_WEBHOOK_PAYLOAD_ERROR: Error parsing webhook event payload. Payload snippet: '{}'", payload.substring(0, Math.min(payload.length(), 200)), e);
            throw new BadRequestException("Invalid webhook payload.");
        }

        // 2. Event Datasından Obje Alma (Güvenli Deneme)
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        Optional<StripeObject> objectOptional = dataObjectDeserializer.getObject();

        if (objectOptional.isEmpty()) {
             // Bazı event tiplerinde data->object null olabilir (örn: sadece ping eventleri)
             // Veya deserialize hatası olmuş olabilir. Stripe dokümantasyonuna göre kontrol edilmeli.
             logger.warn("STRIPE_WEBHOOK_DATA_WARN: Event data object is not present for Event ID: {}, Type: {}", event.getId(), event.getType());
              // Kritik bir event değilse devam edebiliriz, loglamak yeterli.
             // return; // Eğer işlenmesi gereken eventler için bu oluyorsa sorun var demektir.
         }

         StripeObject stripeObject = objectOptional.orElse(null); // Varsa al, yoksa null


        logger.info("STRIPE_WEBHOOK_RECEIVED: EventId='{}', Type='{}', DataObject='{}'",
                event.getId(), event.getType(), stripeObject != null ? stripeObject.getClass().getSimpleName() : "N/A");

        // 3. Event Tipine Göre İşleme (if-else if ile tip kontrolü daha güvenli olabilir)
        String eventType = event.getType();

         try {
             // --- Ödeme Başarılı: Checkout Session Tamamlandı ---
             if ("checkout.session.completed".equals(eventType)) {
                 if (stripeObject instanceof Session session) { // Gelen obje Session mı?
                    logger.info("Processing checkout.session.completed for Session ID: {}", session.getId());

                    // Metadata'yı al (hem session hem payment intent)
                     String orderIdStr = session.getMetadata().get("order_id");
                     String customerIdStr = session.getMetadata().get("customer_id"); // Bilgi amaçlı
                     String targetSubscriptionStr = session.getMetadata().get("target_subscription");

                    // Eğer orderId session metadata'da yoksa, payment intent'e bak
                    if (orderIdStr == null && targetSubscriptionStr == null) {
                         logger.debug("Order/Subscription ID not in session metadata for {}, checking PaymentIntent metadata...", session.getId());
                         PaymentIntent pi = session.getPaymentIntentObject(); // Bu null olabilir
                         if(pi != null && pi.getMetadata() != null) {
                              orderIdStr = pi.getMetadata().get("order_id");
                              targetSubscriptionStr = pi.getMetadata().get("target_subscription");
                              logger.debug("Found in PaymentIntent metadata - OrderId: {}, Subscription: {}", orderIdStr, targetSubscriptionStr);
                         }
                     }


                    // NE İÇİN ÖDEME YAPILDI?
                    if (targetSubscriptionStr != null) {
                        // Bu bir ABONELİK ödemesi
                         logger.info("Webhook indicates a SUBSCRIPTION payment (Type: {}) for User ID: {}. Session ID: {}",
                                 targetSubscriptionStr, customerIdStr != null ? customerIdStr : "N/A", session.getId());
                         try {
                             SubscriptionType targetSubscription = SubscriptionType.valueOf(targetSubscriptionStr.toUpperCase());
                            String userIdStr = session.getMetadata().get("user_id"); // VEYA PaymentIntent meta!

                             // PaymentIntent metadata daha güvenilir olabilir sessiondan sonra geldiği için?
                            if(userIdStr == null && session.getPaymentIntentObject() != null && session.getPaymentIntentObject().getMetadata() != null){
                                userIdStr = session.getPaymentIntentObject().getMetadata().get("user_id");
                             }

                            if (userIdStr != null) {
                                 // SubscriptionService'i çağır
                                subscriptionService.activateSubscriptionFromPayment(session.getId(), Long.parseLong(userIdStr), targetSubscription);
                            } else {
                                logger.error("[WEBHOOK_SUB_ERROR] User ID missing in metadata for subscription payment. Session ID: {}", session.getId());
                             }
                          } catch (IllegalArgumentException e){
                              logger.error("[WEBHOOK_SUB_ERROR] Invalid target_subscription value '{}' in metadata for Session ID: {}", targetSubscriptionStr, session.getId());
                           } catch (Exception e) { // activateSubscription içindeki hatalar
                                logger.error("[WEBHOOK_SUB_ERROR] Error processing subscription activation for Session ID {}: {}", session.getId(), e.getMessage(), e);
                            }

                    } else if (orderIdStr != null) {
                         // Bu bir SİPARİŞ ödemesi
                         logger.info("Webhook indicates an ORDER payment (Order ID: {}) for User ID: {}. Session ID: {}",
                                 orderIdStr, customerIdStr != null ? customerIdStr : "N/A", session.getId());
                         try {
                             fulfillOrder(Long.parseLong(orderIdStr), session); // Siparişi tamamlama metodunu çağır
                         } catch (Exception e){ // fulfillOrder içindeki hatalar
                               logger.error("[WEBHOOK_ORDER_ERROR] Error fulfilling order ID {} from session {}: {}", orderIdStr, session.getId(), e.getMessage(), e);
                          }

                     } else {
                         // Ne sipariş ne abonelik? Metadata eksik veya hatalı.
                          logger.error("[WEBHOOK_METADATA_ERROR] Metadata missing for both order_id and target_subscription in Session ID: {}", session.getId());
                     }

                 } else { // Gelen obje Session değilse
                    logger.error("STRIPE_WEBHOOK_TYPE_ERROR: Expected Session object for checkout.session.completed, but got: {}", stripeObject != null ? stripeObject.getClass().getName() : "null");
                 }
             }
             // --- Ödeme Başarısız ---
              else if ("payment_intent.payment_failed".equals(eventType)) {
                 if (stripeObject instanceof PaymentIntent paymentIntentFailed) {
                    logger.warn("Processing payment_intent.payment_failed for PaymentIntent ID: {}", paymentIntentFailed.getId());
                     String failedOrderIdStr = paymentIntentFailed.getMetadata().get("order_id"); // Sadece siparişler için mi metadata eklemiştik? Kontrol et!
                    if (failedOrderIdStr != null) {
                        try {
                             handleFailedPayment(Long.parseLong(failedOrderIdStr), paymentIntentFailed);
                        } catch (Exception e) {
                             logger.error("[WEBHOOK_FAILED_PAYMENT_ERROR] Error handling failed payment for Order ID {}: {}", failedOrderIdStr, e.getMessage(), e);
                         }
                     } else {
                         logger.error("[WEBHOOK_METADATA_ERROR] Order ID missing in metadata for failed PaymentIntent ID: {}", paymentIntentFailed.getId());
                        // Abonelik başarısız ödemeleri için de benzer bir handle metodu olabilir.
                     }
                 } else {
                     logger.error("STRIPE_WEBHOOK_TYPE_ERROR: Expected PaymentIntent object for payment_intent.payment_failed, but got: {}", stripeObject != null ? stripeObject.getClass().getName() : "null");
                 }
              }
             // --- Ödeme İadesi ---
             else if ("charge.refunded".equals(eventType)) {
                 if (stripeObject instanceof Charge charge) {
                    logger.info("Processing charge.refunded for Charge ID: {}", charge.getId());
                    // İade işleminin DB'ye yansıtılması lazım (Payment ve Order status güncellemesi)
                    // Charge'dan PaymentIntent ID -> Metadata -> Order ID alınabilir.
                    // TODO: handleRefundedPayment(charge);
                 } else {
                    logger.error("STRIPE_WEBHOOK_TYPE_ERROR: Expected Charge object for charge.refunded, but got: {}", stripeObject != null ? stripeObject.getClass().getName() : "null");
                }
            }
              // --- Diğer Önemli Eventler (Örn: payment_intent.succeeded) ---
              else if ("payment_intent.succeeded".equals(eventType)) {
                    // Bazen checkout.session.completed yerine bu event daha önce gelebilir.
                    // Aynı siparişin iki kere işlenmemesi için fulfillOrder içindeki idempotency kontrolü önemli.
                    // Veya bu event'i sadece loglayıp checkout.session.completed'i bekleyebiliriz.
                    logger.info("Received payment_intent.succeeded event: PI_ID='{}'. Currently handled by checkout.session.completed.",
                           ((PaymentIntent)stripeObject).getId());
               }

            // --- Diğer Event Tipleri ---
             else {
                logger.warn("STRIPE_WEBHOOK_UNHANDLED: Unhandled event type: {}", eventType);
            }

        } catch (Exception e) { // Webhook iş mantığı sırasındaki genel hatalar
            logger.error("STRIPE_WEBHOOK_HANDLER_ERROR: Uncaught error processing Event ID: {}. Type: {}. Error: {}",
                     event.getId(), eventType, e.getMessage(), e);
            // Burada 500 hatası dönebiliriz veya loglayıp 200 OK dönerek tekrar denenmesini önleyebiliriz.
            // Genelde loglayıp 200 dönmek daha iyidir, webhook akışını kesmez.
        }
    } // handleStripeWebhook sonu
    // --- Özel (Protected/Private) Metodlar ---

    // Ödeme başarılı olduğunda çağrılır (checkout.session.completed)
    @Transactional
    protected void fulfillOrder(Long orderId, @NotNull Session session) {
        Order order = orderRepository.findById(orderId)
                .orElse(null);

        if (order == null) {
            logger.error("[FULFILLMENT-ERROR] Order not found with ID {} from webhook (SessionID: {}).", orderId, session.getId());
            // Çok kritik durum, logla ve belki admin'e bildir. Webhook tekrar denememeli.
            return;
        }

         // idempotency: Eğer zaten tamamlandıysa tekrar işleme
        if (order.getPaymentStatus() == PaymentStatus.COMPLETED && order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            logger.warn("[FULFILLMENT-INFO] Order ID: {} already fulfilled (Status: {}, PaymentStatus: {}). Ignoring duplicate webhook (SessionID: {}).",
                    orderId, order.getStatus(), order.getPaymentStatus(), session.getId());
            return;
        }

        // 1. Sipariş Durumlarını Güncelle
         order.setStatus(OrderStatus.PROCESSING); // Artık sipariş işleniyor
         order.setPaymentStatus(PaymentStatus.COMPLETED); // Ödeme tamamlandı
        // Opsiyonel: Payment Method'u Stripe olarak set edebiliriz veya daha detaylı (card vb.)
         // order.setPaymentMethod("STRIPE_" + session.getPaymentMethodTypes().stream().findFirst().orElse("CARD").toUpperCase());
         order.setPaymentMethod("STRIPE"); // Şimdilik basit tutalım


         // 2. Ödeme Kaydı Oluştur (Referans amaçlı)
         Payment payment = new Payment();
        payment.setOrder(order);
         // Stripe tutarı cent/kuruş cinsinden döner, biz BigDecimal kullanıyoruz
         payment.setAmount(BigDecimal.valueOf(session.getAmountTotal()).divide(BigDecimal.valueOf(100))); // veya order.getFinalAmount()
         payment.setPaymentMethod("STRIPE");
        payment.setStatus(PaymentStatus.COMPLETED);
         payment.setTransactionId(session.getPaymentIntent()); // İlişkili Payment Intent ID
        payment.setCurrency(session.getCurrency() != null ? session.getCurrency().toUpperCase() : "TRY"); // Para birimi
        payment.setGatewayResponse("Stripe Session ID: " + session.getId() + ", Status: " + session.getPaymentStatus());
         paymentRepository.save(payment);

         // 3. Siparişi Kaydet
        Order updatedOrder = orderRepository.save(order);

         // 4. !!! Sepeti Temizle !!!
        User customer = updatedOrder.getCustomer();
         if (customer != null) {
            cartRepository.findByUserId(customer.getId()).ifPresent(cart -> {
                logger.info("Clearing cart ID: {} for user ID: {} after successful payment.", cart.getId(), customer.getId());
                cartItemRepository.deleteByCartId(cart.getId()); // Tüm cart item'ları sil
                // Opsiyonel: Sepetin kendisini de silebiliriz ama genellikle kalır. cartRepository.delete(cart);
             });
        } else {
             logger.error("[FULFILLMENT-ERROR] Cannot clear cart. Customer is null for fulfilled Order ID: {}", orderId);
        }

        // 5. Müşteriye Bildirim Gönder
       // ... (Order durumu PROCESSING, Payment COMPLETED yapıldıktan sonra) ...
// 5. Müşteriye Sipariş Onay E-postası Gönder
try {
    String subject = "Fibiyo Siparişiniz #" + orderId + " Onaylandı";
    // TODO: Daha detaylı bir sipariş özeti HTML e-postası oluşturulabilir.
     String textBody = "Merhaba " + customer.getFirstName() + ",\n\n"
              + "#" + orderId + " numaralı siparişiniz başarıyla alındı ve ödemeniz onaylandı.\n"
             + "Sipariş detaylarınızı hesabınızdan takip edebilirsiniz.\n\n"
              + "Teşekkürler!";
      emailService.sendSimpleMessage(customer.getEmail(), subject, textBody);
  } catch (Exception e) {
       logger.error("[FULFILLMENT-ERROR] Failed to send order confirmation email for Order ID {}: {}", orderId, e.getMessage(), e);
  }


// 6- Müşteriye bildirim gönder
try {
    notificationService.createNotification( // Inject edilmiş olmalı
        updatedOrder.getCustomer(),
         "#" + orderId + " numaralı siparişiniz alındı ve ödemeniz onaylandı. Hazırlanmaya başlandı.",
         "/orders/my/" + orderId, // Frontend'deki ilgili sayfanın linki
        NotificationType.ORDER_UPDATE);
 } catch (Exception e) {
     logger.error("[FULFILLMENT-ERROR] Failed to send order confirmation notification for Order ID {}: {}", orderId, e.getMessage(), e);
}

// ...

        logger.info("[FULFILLMENT-SUCCESS] Order ID: {} processed successfully. New Status: {}, PaymentStatus: {}",
                 orderId, updatedOrder.getStatus(), updatedOrder.getPaymentStatus());
    }

    // Ödeme başarısız olduğunda çağrılır (payment_intent.payment_failed)
    @Transactional
    protected void handleFailedPayment(Long orderId, @NotNull PaymentIntent paymentIntent) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
             logger.error("[PAYMENT-FAILED-ERROR] Order not found with ID {} from failed payment webhook (PaymentIntent ID: {}).", orderId, paymentIntent.getId());
            return;
        }

        // Sadece ödeme bekleyen siparişlerin durumunu değiştir, diğerlerini logla.
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setPaymentStatus(PaymentStatus.FAILED);
             // Opsiyonel: Sipariş durumunu CANCELLED yapabiliriz veya kullanıcıya tekrar deneme şansı vermek için PENDING_PAYMENT'da bırakabiliriz.
            // order.setStatus(OrderStatus.PENDING_PAYMENT); // Tekrar deneyebilsin
            Order updatedOrder = orderRepository.save(order);

            // Ödeme kaydı (FAILED)
             Payment payment = new Payment();
             payment.setOrder(order);
             payment.setAmount(order.getFinalAmount()); // Veya paymentIntent.getAmount() / 100
             payment.setPaymentMethod("STRIPE");
            payment.setStatus(PaymentStatus.FAILED);
            payment.setTransactionId(paymentIntent.getId());
             payment.setCurrency(paymentIntent.getCurrency() != null ? paymentIntent.getCurrency().toUpperCase() : "TRY");
            payment.setGatewayResponse("Failed: " + Optional.ofNullable(paymentIntent.getLastPaymentError()).map(StripeError::getMessage).orElse("N/A"));
            paymentRepository.save(payment);

         // ... (Order durumu güncellendikten ve Payment kaydı oluşturulduktan sonra) ...

 // Müşteriye Bildirim
 try {
    notificationService.createNotification(
             order.getCustomer(),
             "#" + orderId + " numaralı siparişiniz için ödeme alınamadı. Lütfen tekrar deneyin.",
            "/orders/my/" + orderId,
            NotificationType.ORDER_UPDATE); // Veya özel bir PAYMENT_FAILED tipi
 } catch (Exception e){
     logger.error("[PAYMENT-FAILED-ERROR] Failed to send payment failed notification for Order ID {}: {}", orderId, e.getMessage(), e);
  }

             logger.warn("[PAYMENT-FAILED] Payment failed for Order ID: {}. PaymentIntent ID: {}. Last Error: {}",
                    orderId, paymentIntent.getId(), payment.getGatewayResponse());
         } else {
            logger.warn("[PAYMENT-FAILED-INFO] Received failed payment event for Order ID: {} which is not PENDING_PAYMENT (Current: {}). Ignoring status update. PI_ID: {}",
                     orderId, order.getStatus(), paymentIntent.getId());
         }
    }

     // ... (İleride eklenecek Refund metotları vb.) ...

}
