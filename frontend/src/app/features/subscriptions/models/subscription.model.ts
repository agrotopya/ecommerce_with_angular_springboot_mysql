// src/app/features/subscriptions/models/subscription.model.ts

// SubscriptionType enum'ı shared/enums altında oluşturulacak.
// Şimdilik string literal type kullanalım.
export type SubscriptionType = 'FREE' | 'CUSTOMER_PLUS' | 'SELLER_PLUS';

export interface SubscriptionStatusResponse {
  currentSubscription: SubscriptionType;
  expiryDate?: string | null; // ISO Date string, FREE ise null olabilir
  isActive: boolean; // Mevcut abonelik geçerli ve süresi dolmamış mı?
}

// StripeCheckoutSessionResponse zaten payment.model.ts içinde tanımlı.
// SubscriptionUpgradeRequest (apidocs.md'de yok ama frontend.md'de var)
// Bu, /api/subscriptions/checkout-session endpoint'ine query param olarak gönderiliyor.
// Ayrı bir DTO'ya gerek yok.
