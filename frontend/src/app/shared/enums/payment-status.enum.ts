// src/app/shared/enums/payment-status.enum.ts

export enum PaymentStatus {
  PENDING = 'PENDING', // Ödeme bekleniyor veya işleniyor
  PAID = 'PAID', // Ödeme tamamlandı
  FAILED = 'FAILED', // Ödeme başarısız oldu
  REFUNDED = 'REFUNDED', // İade edildi
  PARTIALLY_REFUNDED = 'PARTIALLY_REFUNDED' // Kısmen iade edildi
}
