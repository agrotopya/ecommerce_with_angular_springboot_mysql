package com.fibiyo.ecommerce.domain.enums;

public enum PaymentStatus {
    PENDING,           // Ödeme Bekleniyor
    COMPLETED,         // Tamamlandı
    FAILED,            // Başarısız
    REFUNDED,          // Tamamen İade Edildi
    PARTIALLY_REFUNDED // Kısmen İade Edildi
}