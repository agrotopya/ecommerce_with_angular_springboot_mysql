import { Product } from "./product.model";
import { AddressDto } from "./address.model"; // Doğru AddressDto import edildi
import { OrderStatus } from "../enums/order-status.enum"; // OrderStatus import edildi
import { PaymentStatus } from "../enums/payment-status.enum"; // PaymentStatus import edildi

// order.model.ts içindeki yerel AddressDto tanımı kaldırıldı.

export interface OrderItemRequestDto {
  productId: number;
  quantity: number;
  unitPrice: number; // Price at the time of order
}

export interface PaymentDetailsDto {
  method: string; // e.g., "creditCard", "paypal"
  transactionId?: string; // From payment gateway
  status?: string; // e.g., "PENDING", "COMPLETED", "FAILED"
  // Add more payment-specific fields if needed
}

export interface CreateOrderRequestDto {
  // userId?: number; // Backend should get this from authenticated token
  cartItems: OrderItemRequestDto[];
  shippingAddress: AddressDto;
  billingAddress: AddressDto; // Could be same as shipping
  paymentMethod: string; // paymentDetails nesnesi kaldırıldı, doğrudan paymentMethod eklendi (apidocs.md 8.1 ile uyumlu)
  totalAmount: number;
  currency?: string; // e.g., "USD"
  notes?: string;
  couponCode?: string; // Kupon kodu için eklendi
}

export interface OrderItemResponseDto {
  id: number;
  product: Product; // ProductResponseDto -> Product
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface OrderResponseDto {
  id: number;
  trackingNumber: string; // orderTrackingNumber -> trackingNumber (backend yanıtına göre)
  userId: number; // apidocs: customerId
  customerFirstName?: string; // Eklendi
  customerLastName?: string; // Eklendi
  customerEmail?: string; // Eklendi (shippingAddress.email yerine bunu kullanabiliriz)
  items: OrderItemResponseDto[]; // apidocs: orderItems
  shippingAddress: AddressDto;
  billingAddress: AddressDto;
  // paymentDetails: PaymentDetailsDto; // Bu alan kaldırılıyor
  paymentMethod: string; // Backend yanıtına göre eklendi (zaten apidocs'ta vardı)
  paymentStatus: PaymentStatus; // string -> PaymentStatus enum
  status: OrderStatus; // string -> OrderStatus enum
  totalAmount: number; // apidocs: İndirimsiz ürün toplamı
  discountAmount?: number; // apidocs'tan eklendi
  shippingFee?: number; // apidocs'tan eklendi
  finalAmount?: number; // apidocs'tan eklendi (ödenecek tutar)
  currency: string; // currencyCode -> currency olarak düzeltildi (HTML'de de düzeltilecek)
  createdAt: string; // apidocs: orderDate (veya createdAt)
  updatedAt: string;
  notes?: string;
  couponCode?: string; // apidocs'tan eklendi
}

export interface OrderStatusUpdateRequestDto {
    status: OrderStatus; // string -> OrderStatus enum
    notes?: string; // Optional notes for status update, e.g., tracking number for SHIPPED
}
