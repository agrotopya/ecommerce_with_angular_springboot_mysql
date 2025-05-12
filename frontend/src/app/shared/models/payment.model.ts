// src/app/shared/models/payment.model.ts

export interface CreateCheckoutSessionRequest {
  orderId: number;
  successUrl?: string;
  cancelUrl?: string;
}

export interface StripeCheckoutSessionResponse {
  sessionId: string;
  checkoutUrl?: string; // Opsiyonel, backend her zaman dönmeyebilir
}
