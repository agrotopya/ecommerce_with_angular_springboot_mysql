// src/app/shared/enums/order-status.enum.ts

export enum OrderStatus {
  PENDING_PAYMENT = 'PENDING_PAYMENT', // Ödeme bekleniyor
  PROCESSING = 'PROCESSING', // Sipariş alındı, hazırlanıyor
  SHIPPED = 'SHIPPED', // Kargoya verildi
  DELIVERED = 'DELIVERED', // Teslim edildi
  CANCELLED_BY_CUSTOMER = 'CANCELLED_BY_CUSTOMER', // Müşteri tarafından iptal edildi
  CANCELLED_BY_SELLER = 'CANCELLED_BY_SELLER', // Satıcı tarafından iptal edildi
  CANCELLED_BY_ADMIN = 'CANCELLED_BY_ADMIN', // Admin tarafından iptal edildi
  REFUNDED = 'REFUNDED', // İade edildi
  PARTIALLY_REFUNDED = 'PARTIALLY_REFUNDED', // Kısmen iade edildi
  ON_HOLD = 'ON_HOLD', // Beklemede (örn: stok sorunu)
  FAILED = 'FAILED' // Sipariş oluşturma/ödeme başarısız
}
