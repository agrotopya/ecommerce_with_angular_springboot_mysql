package com.fibiyo.ecommerce.domain.enums;

public enum NotificationType {
    ORDER_UPDATE,        // Sipariş durumu değişikliği
    NEW_PROMOTION,       // Yeni indirim/kampanya
    WISHLIST_PRICE_DROP, // İstek listesindeki ürünün fiyatı düştü
    NEW_PRODUCT_SELLER,  // Takip edilen satıcıdan yeni ürün (İleri seviye özellik)
    REVIEW_APPROVED,     // Yorum onaylandı
    REVIEW_REJECTED,     // Yorum reddedildi (Opsiyonel)
    PRODUCT_APPROVED,    // Ürün onaylandı (Satıcı için)
    PRODUCT_REJECTED,    // Ürün reddedildi (Satıcı için)
    SYSTEM_MESSAGE,      // Genel sistem duyurusu
    SUBSCRIPTION_UPDATE, // Abonelik durumu (örn: süresi doluyor)
    IMAGE_GEN_UPDATE,    // AI Görüntü hakkı güncellemesi (Satıcı için)
    GENERIC,              // Genel bildirim
    PRODUCT_UPDATE,
    SYSTEM
}