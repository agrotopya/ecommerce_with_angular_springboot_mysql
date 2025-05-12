// src/app/shared/enums/notification-type.enum.ts

export enum NotificationType {
  ORDER_UPDATE = 'ORDER_UPDATE', // Sipariş durumu değiştiğinde
  PRODUCT_APPROVED = 'PRODUCT_APPROVED', // Satıcının ürünü onaylandığında
  PRODUCT_REJECTED = 'PRODUCT_REJECTED', // Satıcının ürünü reddedildiğinde
  NEW_REVIEW = 'NEW_REVIEW', // Bir ürüne yeni yorum yapıldığında (satıcıya veya admine)
  REVIEW_APPROVED = 'REVIEW_APPROVED', // Yorum onaylandığında (kullanıcıya)
  NEW_PROMOTION = 'NEW_PROMOTION', // Yeni bir kampanya veya indirim olduğunda
  FEEL_APPROVED = 'FEEL_APPROVED', // Satıcının feel'i onaylandığında
  FEEL_REJECTED = 'FEEL_REJECTED', // Satıcının feel'i reddedildiğinde
  SUBSCRIPTION_REMINDER = 'SUBSCRIPTION_REMINDER', // Abonelik bitiş tarihi yaklaştığında
  SUBSCRIPTION_RENEWED = 'SUBSCRIPTION_RENEWED', // Abonelik yenilendiğinde
  SUBSCRIPTION_CANCELLED = 'SUBSCRIPTION_CANCELLED', // Abonelik iptal edildiğinde
  WELCOME_MESSAGE = 'WELCOME_MESSAGE', // Yeni kullanıcı kaydolduğunda
  PASSWORD_RESET = 'PASSWORD_RESET', // Şifre sıfırlandığında
  // Diğer bildirim türleri eklenebilir
}
