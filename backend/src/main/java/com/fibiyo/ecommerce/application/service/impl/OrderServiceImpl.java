package com.fibiyo.ecommerce.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fibiyo.ecommerce.application.dto.AddressDto;
// import com.fibiyo.ecommerce.application.dto.CartResponse; // createOrder içinde artık kullanılmıyor
import com.fibiyo.ecommerce.application.dto.OrderRequest;
import com.fibiyo.ecommerce.application.dto.OrderResponse;
import com.fibiyo.ecommerce.application.exception.BadRequestException;
import com.fibiyo.ecommerce.application.exception.ForbiddenException;
import com.fibiyo.ecommerce.application.exception.ResourceNotFoundException;
import com.fibiyo.ecommerce.application.mapper.OrderMapper;
import com.fibiyo.ecommerce.application.service.CartService; // Inject edilecek
import com.fibiyo.ecommerce.application.service.EmailService;
import com.fibiyo.ecommerce.application.service.NotificationService; // Inject edilecek
import com.fibiyo.ecommerce.application.service.OrderService;
// import com.fibiyo.ecommerce.application.service.CouponService; // İhtiyaç olursa inject edilebilir
import com.fibiyo.ecommerce.domain.entity.*; // Entity importları
import com.fibiyo.ecommerce.domain.enums.NotificationType; // Enumlar
import com.fibiyo.ecommerce.domain.enums.OrderStatus;
import com.fibiyo.ecommerce.domain.enums.PaymentStatus;
import com.fibiyo.ecommerce.domain.enums.Role; // Yetki kontrolü için
import com.fibiyo.ecommerce.infrastructure.persistence.repository.*; // Repository importları
// Specification importları (gerekirse)
import com.fibiyo.ecommerce.infrastructure.persistence.specification.OrderSpecifications; // Bu sınıfı oluşturmak gerekebilir
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode; // Kupon hesaplama için
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    // --- Injected Dependencies ---
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository; // Gerekli olabilir (itemTotal güncelleme kontrolü vb.)
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final CartService cartService; // Sepet temizleme ve bilgi alma
    private final CartItemRepository cartItemRepository; // Direkt sepet temizleme için (clearCart alternatifi)
    private final NotificationService notificationService; // Bildirim gönderme
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;
    private final EmailService emailService; // Email gönderme (sipariş onayı için)


    // --- Helper Methods ---
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ForbiddenException("Erişim için kimlik doğrulaması gerekli.");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current authenticated user not found: " + username));
    }

    //  private void checkAdminRole() {
    //      User currentUser = getCurrentUser();
    //      if (currentUser.getRole() != Role.ADMIN) {
    //          logger.warn("User '{}' (Role: {}) attempted an admin-only order operation.", currentUser.getUsername(), currentUser.getRole());
    //          throw new ForbiddenException("Bu işlemi gerçekleştirmek için Admin yetkisine sahip olmalısınız.");
    //      }
    //  }

      // Satıcının belirli bir siparişi yönetme yetkisi var mı kontrolü (Ürün bazlı)
    //  private void checkSellerPermissionForOrder(User seller, Order order) {
    //       if (seller.getRole() == Role.ADMIN) return; // Admin her şeye erişebilir
    //       if (seller.getRole() != Role.SELLER) throw new ForbiddenException("Bu işlem için Seller veya Admin yetkisi gerekli.");

    //       boolean sellerHasProductInOrder = order.getOrderItems().stream()
    //               .anyMatch(item -> item.getProduct() != null && item.getProduct().getSeller().getId().equals(seller.getId()));

    //       if (!sellerHasProductInOrder) {
    //           logger.warn("Seller ID {} attempted to access/modify Order ID {} which contains none of their products.", seller.getId(), order.getId());
    //          throw new ForbiddenException("Bu siparişi yönetme yetkiniz yok (size ait ürün içermiyor).");
    //       }
    //   }


    private String convertAddressToJson(AddressDto addressDto) {
        if (addressDto == null) return null;
        try {
            return objectMapper.writeValueAsString(addressDto);
        } catch (JsonProcessingException e) {
            logger.error("Error converting AddressDto to JSON: {}", e.getMessage(), e);
            throw new BadRequestException("Adres bilgisi formatı hatalı.");
        }
    }

    private void restoreStockForOrderItems(List<OrderItem> items) {
        for (OrderItem item : items) {
            Product product = item.getProduct();
            if (product != null) {
                 // Ürünün güncel stoğunu tekrar okuyup üzerine eklemek daha güvenli olabilir (concurrency)
                 // Ama transactional context içinde olduğumuz için şimdilik direkt ekleyelim
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
                logger.debug("Restored {} stock for product ID: {}", item.getQuantity(), product.getId());
            } else {
                logger.warn("Could not restore stock for order item ID: {}. Product not found (likely deleted).", item.getId());
            }
        }
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal amount) {
         if(coupon == null || amount == null) return BigDecimal.ZERO;
         BigDecimal discount = BigDecimal.ZERO;
        if (coupon.getDiscountType() == com.fibiyo.ecommerce.domain.enums.DiscountType.FIXED_AMOUNT) {
            discount = coupon.getDiscountValue();
        } else if (coupon.getDiscountType() == com.fibiyo.ecommerce.domain.enums.DiscountType.PERCENTAGE) {
            discount = amount.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return discount.min(amount); // İndirim, tutarı geçemez
     }


    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, OrderItemRepository orderItemRepository, ProductRepository productRepository, UserRepository userRepository, CouponRepository couponRepository, CartService cartService, CartItemRepository cartItemRepository, NotificationService notificationService, OrderMapper orderMapper, ObjectMapper objectMapper
    , EmailService emailService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.couponRepository = couponRepository;
        this.cartService = cartService;
        this.cartItemRepository = cartItemRepository; // Temizleme için eklendi
        this.notificationService = notificationService; // Bildirim için eklendi
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
        this.emailService = emailService; // Email gönderme için eklendi
        
    }


    // --- Customer Operations Implementation ---

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {
        User customer = getCurrentUser();
        // Sepet entity'sini CartService'deki public metot ile al
        Cart cart = cartService.getOrCreateCartEntityForUser(customer);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Sipariş oluşturmak için sepetinizde ürün bulunmalıdır.");
        }
        logger.info("Creating order for customer ID: {} from cart ID: {}", customer.getId(), cart.getId());

        Order order = new Order();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentMethod(orderRequest.getPaymentMethod());
        order.setShippingAddress(convertAddressToJson(orderRequest.getShippingAddress()));
        order.setBillingAddress(convertAddressToJson(orderRequest.getBillingAddress() != null ? orderRequest.getBillingAddress() : orderRequest.getShippingAddress()));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.ZERO); // TODO: Kargo ücreti hesaplama mantığı eklenecek
        // order.setFinalAmount(...); // -> DB GENERATED VEYA @Formula! BURADA SET ETME!

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Sepet Items üzerinde loop et (DB tutarlılığı için Product'ı tekrar çek)
        for (CartItem cartItem : new ArrayList<>(cart.getItems())) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> {
                         // Bu durum, sepet temizlenmediyse ve ürün silindiyse olabilir.
                        logger.error("Product (ID: {}) from cart item (ID: {}) not found in DB during order creation!",
                                   cartItem.getProduct().getId(), cartItem.getId());
                        return new BadRequestException("Sepetinizdeki bir ürün ('"+cartItem.getProduct().getName()+"') artık mevcut değil. Lütfen sepetinizi güncelleyin.");
                    });

            int quantity = cartItem.getQuantity();

            // Kontroller
            if (!product.isActive() || !product.isApproved()) {
                throw new BadRequestException("Sepetinizdeki ürün '" + product.getName() + "' şu anda satın alınamaz.");
            }
            if (product.getStock() < quantity) {
                 throw new BadRequestException("Sepetinizdeki '" + product.getName() + "' ürünü için stok yetersiz (Kalan: " + product.getStock()+").");
            }

            // OrderItem oluştur
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setPriceAtPurchase(product.getPrice());
            order.addOrderItem(orderItem);

            // Stoğu azalt
            product.setStock(product.getStock() - quantity);
            // productRepository.save(product); // -> Cascade ile Order kaydedilince Product da güncellenmeli mi? Yoksa manuel mi? Manuel yapalım garanti olsun.
            productRepository.save(product); // Explicitly save the product stock change

            totalAmount = totalAmount.add(orderItem.getPriceAtPurchase().multiply(BigDecimal.valueOf(quantity)));
        }

        order.setTotalAmount(totalAmount);

        // Kupon Kontrolü ve Uygulama
        Coupon appliedCoupon = null;
        if (orderRequest.getCouponCode() != null && !orderRequest.getCouponCode().isBlank()) {
            String couponCode = orderRequest.getCouponCode().toUpperCase();
            appliedCoupon = couponRepository.findByCodeAndIsActiveTrueAndExpiryDateAfter(couponCode, LocalDateTime.now())
                     .orElseThrow(() -> new BadRequestException("Geçersiz veya süresi dolmuş kupon kodu: " + couponCode));

             // Min sepet tutarı kontrolü (İndirim uygulanmadan önceki totalAmount'a göre)
            if (totalAmount.compareTo(appliedCoupon.getMinPurchaseAmount()) < 0) {
                 throw new BadRequestException(String.format("Bu kuponu kullanmak için minimum alışveriş tutarı %.2f TL olmalıdır.", appliedCoupon.getMinPurchaseAmount()));
            }
            if (appliedCoupon.isUsageLimitReached()) {
                throw new BadRequestException("Bu kupon kullanım limitine ulaşmış.");
            }

            BigDecimal discount = calculateDiscount(appliedCoupon, totalAmount); // İndirimi hesapla
            order.setCoupon(appliedCoupon);
            order.setDiscountAmount(discount);
             // Kupon kullanımını artır (Transaction sonunda başarılı olursa kaydedilir)
             appliedCoupon.incrementTimesUsed();
             // couponRepository.save(appliedCoupon); // Order save edilirken cascade veya @PreUpdate ile yapılabilir? Şimdilik manuel yapalım.
             couponRepository.save(appliedCoupon);

             logger.info("Coupon '{}' applied. Discount: {}", couponCode, discount);
        }

        // finalAmount'ı manuel olarak hesapla
        BigDecimal finalAmount = totalAmount.subtract(order.getDiscountAmount()).add(order.getShippingFee());
        order.setFinalAmount(finalAmount);

        // Order ve ilişkili OrderItem'ları (ve Product/Coupon güncellemelerini) kaydet
        Order savedOrder = orderRepository.save(order);

        // SEPETİ TEMİZLE (cartItemRepository kullanarak)
        try {
            cartItemRepository.deleteByCartId(cart.getId());
            logger.info("Cart ID: {} cleared after order creation.", cart.getId());
        } catch (Exception e){
             // Sepet temizleme hatası siparişi geri almamalı ama loglanmalı!
            logger.error("Failed to clear cart ID {} after creating Order ID {}: {}", cart.getId(), savedOrder.getId(), e.getMessage());
        }

        // DEBUG İÇİN EKLENDİ: Kaydedilmiş ve tazelenmiş siparişin finalAmount'ını logla
        Order refreshedOrder = orderRepository.findById(savedOrder.getId()).orElse(null);
        if (refreshedOrder != null) {
            logger.info("DEBUG: Order ID: {} (refreshed) - Total: {}, Discount: {}, Shipping: {}, Final: {}",
                refreshedOrder.getId(),
                refreshedOrder.getTotalAmount(),
                refreshedOrder.getDiscountAmount(),
                refreshedOrder.getShippingFee(),
                refreshedOrder.getFinalAmount()); // Bu null mu?
        } else {
            logger.error("DEBUG: Could not re-fetch order ID: {} after saving for debug.", savedOrder.getId());
        }
        // DEBUG SONU

        logger.info("Order ID: {} created successfully for customer ID: {}. Final Amount from savedOrder object: {}",
                   savedOrder.getId(), customer.getId(), savedOrder.getFinalAmount()); // Bu null mu?

        // Bildirim Gönderme (Order Placed) ve Email gönderme buradan kaldırıldı.
        // Bu işlemler ödeme tamamlandıktan sonra (fulfillOrder içinde) yapılmalı.

        // TODO: Ödeme Başlatma (eğer otomatik olacaksa)
        // paymentService.createCheckoutSession(...); // Bu çağrı zaten frontend'den geliyor.

        return orderMapper.toOrderResponse(savedOrder);
        
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> findMyOrders(Pageable pageable) {
        User customer = getCurrentUser();
        logger.debug("Fetching orders for customer ID: {}", customer.getId());
        Page<Order> orderPage = orderRepository.findByCustomerIdOrderByOrderDateDesc(customer.getId(), pageable);
        return orderPage.map(orderMapper::toOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse findMyOrderById(Long orderId) {
        User customer = getCurrentUser();
        logger.debug("Fetching order ID: {} for customer ID: {}", orderId, customer.getId());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("Bu siparişi görüntüleme yetkiniz yok.");
        }
        return orderMapper.toOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelMyOrder(Long orderId) {
        User customer = getCurrentUser();
        logger.warn("Customer ID: {} attempting to cancel order ID: {}", customer.getId(), orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("Bu siparişi iptal etme yetkiniz yok.");
        }

        // İptal koşulları (processing altında olmalı?)
        if (!(order.getStatus() == OrderStatus.PENDING_PAYMENT || order.getStatus() == OrderStatus.PROCESSING)) {
             throw new BadRequestException("Bu sipariş artık iptal edilemez (Mevcut Durum: " + order.getStatus() + ").");
        }

        order.setStatus(OrderStatus.CANCELLED_BY_CUSTOMER);

         // Ödeme Durumu: Eğer ödenmişse refund gerekir, değilse direkt iptal.
        boolean paymentWasCompleted = order.getPaymentStatus() == PaymentStatus.COMPLETED || order.getPaymentStatus() == PaymentStatus.PARTIALLY_REFUNDED;
         if (paymentWasCompleted) {
             order.setPaymentStatus(PaymentStatus.REFUNDED); // TODO: Gerçek refund işlemini tetikle (PaymentService)!
              logger.info("Order ID {} cancelled by customer. Initiating refund process.", orderId);
             // paymentService.initiateRefund(orderId, "Müşteri İptali");
         } else {
              order.setPaymentStatus(PaymentStatus.PENDING); // Veya özel bir "CANCELLED" ödeme durumu? Şimdilik PENDING kalabilir.
             logger.info("Order ID {} cancelled by customer before payment completion.", orderId);
         }

        restoreStockForOrderItems(order.getOrderItems());

         if (order.getCoupon() != null) {
             // TODO: Kupon sayacını decrement et
             logger.info("Decrementing usage count for coupon code: {}", order.getCoupon().getCode());
            // Coupon coupon = order.getCoupon(); coupon.decrementTimesUsed(); couponRepository.save(coupon);
         }

        Order cancelledOrder = orderRepository.save(order);

         // Bildirim Gönder (Admin'e / Satıcıya?)
         // notificationService.createNotification(...);

        logger.info("Order ID: {} cancelled successfully by customer ID: {}.", orderId, customer.getId());
        return orderMapper.toOrderResponse(cancelledOrder);
    }

    // --- Admin/Seller Operations Implementation ---

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> findAllOrders(Pageable pageable, Long customerId, OrderStatus status) {
      //  checkAdminRole(); // Sadece admin tüm siparişleri görür
        logger.debug("ADMIN: Fetching all orders. Customer Filter: {}, Status Filter: {}, Pageable: {}", customerId, status, pageable);

         Specification<Order> spec = Specification.where(OrderSpecifications.hasCustomer(customerId))
                .and(OrderSpecifications.hasStatus(status));
         // İhtiyaca göre diğer Specification'lar eklenebilir (.withDateBetween(), .withSeller() vs.)

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        return orderPage.map(orderMapper::toOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> findSellerOrders(Pageable pageable, OrderStatus status) {
         // Hem Admin hem Seller bu endpoint'i kullanabilir ama farklı filtrelerle
         User currentUser = getCurrentUser();
         Specification<Order> spec;

         if(currentUser.getRole() == Role.ADMIN) {
              // Admin tüm seller'ları (veya ID ile filtrelenmiş) görebilir.
              // Şimdilik tüm siparişleri filtreli dönelim (yukarıdaki gibi) veya seller ID filtresi ekleyelim.
               spec = Specification.where(OrderSpecifications.hasStatus(status)); // Örnek
               logger.debug("ADMIN: Fetching seller-related orders. Status Filter: {}, Pageable: {}", status, pageable);
         } else if (currentUser.getRole() == Role.SELLER) {
             // Seller sadece kendi ürünlerini içeren siparişleri görmeli
               logger.debug("SELLER: Fetching orders containing products for seller ID: {}. Status Filter: {}, Pageable: {}",
                          currentUser.getId(), status, pageable);
                spec = Specification.where(OrderSpecifications.hasSellerProduct(currentUser.getId()))
                         .and(OrderSpecifications.hasStatus(status));
          } else {
              // Bu duruma düşmemeli (@PreAuthorize veya kontrol sonrası) ama güvenlik için
              throw new ForbiddenException("Siparişleri görme yetkiniz yok.");
          }

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        return orderPage.map(orderMapper::toOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse findOrderById(Long orderId) {
         // Admin veya ilgili Satıcı görebilmeli
        User currentUser = getCurrentUser();
         logger.debug("User/Admin finding order by ID: {}", orderId);
         Order order = orderRepository.findById(orderId)
                 .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

         // Yetki kontrolü: Ya Adminsin ya da sipariş senin ürününü içeriyor
       //   checkSellerPermissionForOrder(currentUser, order); // Bu metod seller olmayanlar için hata fırlatır (Admin hariç)

          return orderMapper.toOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
         // Admin veya ilgili Satıcı güncelleyebilmeli
        User currentUser = getCurrentUser();
        logger.info("User/Admin ID: {} updating status for Order ID: {} to {}", currentUser.getId(), orderId, newStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Yetki Kontrolü
     //   checkSellerPermissionForOrder(currentUser, order);

         // Durum Geçiş Kontrolleri (Örnek - Daha detaylı olabilir)
         OrderStatus currentStatus = order.getStatus();
         boolean isAdmin = currentUser.getRole() == Role.ADMIN;
         // Sadece Admin her durumu Cancelled yapabilir veya belirli durumlara geri alabilir mi?
         // Seller hangi durumları set edebilir? (Örn: PROCESSING -> SHIPPED)

         if (currentStatus == newStatus) {
            logger.warn("Order ID {} already in status {}. No update performed.", orderId, newStatus);
            return orderMapper.toOrderResponse(order); // Değişiklik yok
         }

         // Basit Örnek Geçişler:
          // Seller sadece işleniyor veya kargolandı yapabilir (eğer ödeme tamamsa)
          if (!isAdmin && !(newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.SHIPPED)) {
               throw new ForbiddenException("Satıcı olarak bu duruma güncelleme yetkiniz yok: " + newStatus);
          }
           if (currentStatus == OrderStatus.DELIVERED || currentStatus.name().startsWith("CANCELLED")) {
               throw new BadRequestException("Tamamlanmış veya iptal edilmiş sipariş durumu değiştirilemez.");
           }
           // Kargolanmış siparişi admin geri alabilir mi?
           if (currentStatus == OrderStatus.SHIPPED && newStatus == OrderStatus.PROCESSING && !isAdmin) {
                throw new ForbiddenException("Kargolanmış sipariş durumu sadece Admin tarafından değiştirilebilir.");
            }

         // Ana Logic
        order.setStatus(newStatus);

        // Yan Etkiler
        String notificationMsg = null;
         NotificationType notificationType = NotificationType.ORDER_UPDATE;

         if (newStatus == OrderStatus.SHIPPED && order.getTrackingNumber() == null) {
            logger.warn("Order ID {} marked as SHIPPED by User ID: {} but no tracking number provided.", orderId, currentUser.getId());
             notificationMsg = "#" + orderId + " numaralı siparişiniz kargoya verildi!";
        } else if (newStatus == OrderStatus.SHIPPED && order.getTrackingNumber() != null) {
             notificationMsg = "#" + orderId + " numaralı siparişiniz kargoya verildi! Takip Numarası: " + order.getTrackingNumber();
         }
         else if (newStatus == OrderStatus.DELIVERED) {
             order.setPaymentStatus(PaymentStatus.COMPLETED); // Otomatik tamamlama
              notificationMsg = "#" + orderId + " numaralı siparişiniz teslim edildi! Bizi değerlendirmeyi unutmayın.";

             // Ürünlerin satış sayılarını güncelle
             for (OrderItem item : order.getOrderItems()) {
                 Product product = item.getProduct();
                 if (product != null) {
                     product.setSalesCount(product.getSalesCount() + item.getQuantity());
                     productRepository.save(product);
                     logger.info("Updated sales count for product ID {}: new count {}", product.getId(), product.getSalesCount());
                 }
             }
         } else if (newStatus.name().startsWith("CANCELLED")) {
             logger.warn("Order ID: {} status set to {} by User/Admin ID: {}. Restoring stock and handling payment.", orderId, newStatus, currentUser.getId());
            if (order.getPaymentStatus() == PaymentStatus.COMPLETED || order.getPaymentStatus() == PaymentStatus.PARTIALLY_REFUNDED) {
                 order.setPaymentStatus(PaymentStatus.REFUNDED); // Refund gerekli
                 // TODO: PaymentService.initiateRefund(...) çağır
             } else {
                 order.setPaymentStatus(PaymentStatus.PENDING); // Veya FAILED?
             }
            restoreStockForOrderItems(order.getOrderItems());
            // TODO: Kuponu geri al?
            notificationMsg = "#" + orderId + " numaralı siparişiniz iptal edildi.";
         }

        Order updatedOrder = orderRepository.save(order);

         // Bildirim Gönder
         if(notificationMsg != null && updatedOrder.getCustomer() != null) {
            try{
                 notificationService.createNotification(updatedOrder.getCustomer(), notificationMsg, "/orders/my/"+orderId, notificationType);
             } catch (Exception e){
                 logger.error("Failed to send status update notification for Order ID {}", orderId, e);
             }
         }

        logger.info("Order ID: {} status updated to {} by User/Admin ID: {}.", orderId, newStatus, currentUser.getId());
        return orderMapper.toOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponse addTrackingNumber(Long orderId, String trackingNumber) {
         // Admin veya ilgili Satıcı ekleyebilir
        User currentUser = getCurrentUser();
         logger.info("User/Admin ID: {} adding tracking number '{}' to Order ID: {}", currentUser.getId(), trackingNumber, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

      //  checkSellerPermissionForOrder(currentUser, order); // Yetki kontrolü

        // Sipariş uygun durumda mı?
        if (!(order.getStatus() == OrderStatus.PROCESSING || order.getStatus() == OrderStatus.SHIPPED)) {
             throw new BadRequestException("Takip numarası eklemek için sipariş durumu uygun değil: " + order.getStatus());
        }

        order.setTrackingNumber(trackingNumber);

         // Eğer durum PROCESSING ise otomatik SHIPPED yapalım
        if(order.getStatus() == OrderStatus.PROCESSING) {
             order.setStatus(OrderStatus.SHIPPED);
             logger.info("Order ID: {} status automatically updated to SHIPPED after adding tracking number.", orderId);
            // TODO: SHIPPED bildirimi burada gönderilebilir
             try{
                  String msg = "#" + orderId + " numaralı siparişiniz kargoya verildi! Takip No: " + trackingNumber;
                  notificationService.createNotification(order.getCustomer(), msg, "/orders/my/"+orderId, NotificationType.ORDER_UPDATE);
              } catch (Exception e){
                   logger.error("Failed to send SHIPPED notification for Order ID {}", orderId, e);
              }
         }

        Order updatedOrder = orderRepository.save(order);
        logger.info("Tracking number '{}' added successfully to Order ID: {}.", trackingNumber, orderId);
        return orderMapper.toOrderResponse(updatedOrder);
    }
}
