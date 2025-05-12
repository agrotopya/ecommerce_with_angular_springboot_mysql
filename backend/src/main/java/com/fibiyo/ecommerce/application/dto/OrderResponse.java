package com.fibiyo.ecommerce.application.dto;

import com.fibiyo.ecommerce.domain.enums.OrderStatus;
import com.fibiyo.ecommerce.domain.enums.PaymentStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private LocalDateTime orderDate;
    private OrderStatus status;
    private BigDecimal totalAmount; // İndirimsiz ürün toplamı
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal finalAmount; // Ödenecek/Ödenen son tutar
    private AddressDto shippingAddress; // JSON'dan parse edilmiş DTO
    private AddressDto billingAddress; // Opsiyonel
    private String paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String couponCode; // Kullanılan kupon kodu
    private String trackingNumber;

    // İlişkiler
    private Long customerId;
    private String customerUsername;
    private List<OrderItemResponse> orderItems;
    // private List<PaymentResponse> payments; // Ödemeleri ayrı endpoint ile almak daha iyi olabilir
}