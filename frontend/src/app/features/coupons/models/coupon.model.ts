// src/app/features/coupons/models/coupon.model.ts
import { DiscountType } from '../../../shared/enums/discount-type.enum'; // Enum import edildi

// export type DiscountType = 'PERCENTAGE' | 'FIXED_AMOUNT'; // Bu satır kaldırıldı, enum kullanılacak

export interface CouponRequest {
  code: string; // Zorunlu, 3-50 karakter
  description?: string; // Opsiyonel
  discountType: DiscountType; // Zorunlu: "PERCENTAGE" veya "FIXED_AMOUNT"
  discountValue: number; // Zorunlu, >0. Yüzde ise 0-100 arası olmalı.
  expiryDate: string; // Zorunlu, ISO Date string (örn: "2024-12-31T23:59:59")
  minPurchaseAmount?: number; // Opsiyonel, >=0 (default: 0)
  isActive?: boolean; // Opsiyonel (default: true)
  usageLimit?: number | null; // Opsiyonel, null ise sınırsız, >=0
}

export interface CouponResponse {
  id: number;
  code: string;
  description?: string;
  discountType: DiscountType;
  discountValue: number;
  expiryDate: string; // ISO Date string
  minPurchaseAmount: number;
  active: boolean; // isActive -> active olarak değiştirildi
  usageLimit: number | null;
  timesUsed: number;
  createdAt: string; // ISO Date string
  updatedAt: string; // ISO Date string
  // Frontend'de hesaplanacak veya backend'den gelecek ek alanlar
  valid?: boolean; // Kupon genel olarak geçerli mi (süresi dolmamış, aktif vb.)
  expired?: boolean; // Süresi dolmuş mu?
  usageLimitReached?: boolean; // Kullanım limitine ulaşılmış mı?
}

export interface CouponValidationResponse {
  valid: boolean;
  message: string;
  code?: string; // Geçerliyse kupon kodu
  discountAmountCalculated?: number; // Sepete göre hesaplanan indirim
  // Hata durumunda ek detaylar eklenebilir
  // errorType?: 'EXPIRED' | 'INVALID_CODE' | 'MIN_PURCHASE_NOT_MET' | 'USAGE_LIMIT_REACHED' | 'NOT_ACTIVE';
}
