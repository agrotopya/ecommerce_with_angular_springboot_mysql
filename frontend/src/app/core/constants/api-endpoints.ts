import { environment } from "../../../environments/environment";

// src/app/core/constants/api-endpoints.ts

export const API_URL = environment.apiUrl;

// Tüm endpoint'leri logla
console.log('API_ENDPOINTS: API_URL =', API_URL);

export const AUTH_ENDPOINTS = {
  LOGIN: `/auth/login`,
  REGISTER: `/auth/register`,
  FORGOT_PASSWORD: `/auth/forgot-password`, // Eklendi
  RESET_PASSWORD: `/auth/reset-password`,   // Reset password için de ekleyelim, muhtemelen sonraki adımda lazım olacak
  // ... diğer auth endpointleri
};
console.log('API_ENDPOINTS: AUTH_ENDPOINTS =', AUTH_ENDPOINTS);

export const USER_ENDPOINTS = {
  ME: `/users/me`,
  CHANGE_PASSWORD: `/users/me/password`, // Şifre değiştirme endpoint'i eklendi
  // ... diğer user endpointleri
};
console.log('API_ENDPOINTS: USER_ENDPOINTS =', USER_ENDPOINTS);

export const CART_ENDPOINTS = {
  BASE: `/cart`, // Backend: /api/cart
  ITEMS: `/cart/items`, // Backend: /api/cart/items
  ITEM_DETAIL: (productId: number) => `/cart/items/${productId}` // Backend: /api/cart/items/{productId}
};
console.log('API_ENDPOINTS: CART_ENDPOINTS =', CART_ENDPOINTS);

export const PRODUCT_ENDPOINTS = {
  BASE: `/products`, // /api/products
  DETAIL: (id: number) => `/products/${id}`, // /api/products/{id}
  BY_SLUG: (slug: string) => `/products/slug/${slug}` // YENİ VEYA MEVCUT KONTROL EDİN
  // SELLER_PRODUCTS: '/seller/products', // Örnek, backend endpointlerine göre
  // ADMIN_PRODUCTS: '/admin/products',   // Örnek
};
console.log('API_ENDPOINTS: PRODUCT_ENDPOINTS =', PRODUCT_ENDPOINTS);

export const CATEGORY_ENDPOINTS = {
  BASE: `/categories`,
  ACTIVE: `/categories/active`,
  ROOTS: `/categories/roots`,
  BY_SLUG: (slug: string) => `/categories/slug/${slug}`,
  DETAIL: (id: number) => `/categories/${id}`,
  SUB_CATEGORIES: (parentId: number) => `/categories/${parentId}/subcategories`,
  UPLOAD_IMAGE: (categoryId: number) => `/categories/${categoryId}/image` // Kategori resmi yükleme endpoint'i
};
console.log('API_ENDPOINTS: CATEGORY_ENDPOINTS =', CATEGORY_ENDPOINTS);

export const ORDERS_ENDPOINTS = {
  BASE: `/orders`, // /api/orders
  MY_ORDERS: `/orders/my`, // /api/orders/my
  ORDER_DETAIL: (orderId: number) => `/orders/${orderId}`, // /api/orders/{orderId}
  MY_ORDER_DETAIL: (orderId: number) => `/orders/my/${orderId}`, // /api/orders/my/{orderId}
  CANCEL_ORDER: (orderId: number) => `/orders/my/${orderId}/cancel`, // /api/orders/my/{orderId}/cancel
  ORDER_STATUS_UPDATE: (orderId: number) => `/orders/${orderId}/status`, // PATCH /api/orders/{orderId}/status
  ORDER_TRACKING_UPDATE: (orderId: number) => `/orders/${orderId}/tracking`, // PATCH /api/orders/{orderId}/tracking
  SELLER_MY_SALES_SUMMARY: `/orders/seller/my-sales-summary`, // GET /api/orders/seller/my-sales-summary
};
console.log('API_ENDPOINTS: ORDERS_ENDPOINTS =', ORDERS_ENDPOINTS);

export const PAYMENTS_ENDPOINTS = {
  CREATE_CHECKOUT_SESSION: `/payments/create-checkout-session`, // /api/payments/create-checkout-session
};
console.log('API_ENDPOINTS: PAYMENTS_ENDPOINTS =', PAYMENTS_ENDPOINTS);

export const ADMIN_ENDPOINTS = {
  USERS_BASE: `/admin/users`,
  USER_DETAIL: (userId: number | string) => `/admin/users/${userId}`,
  USER_UPDATE_STATUS: (userId: number | string) => `/admin/users/${userId}/status`,
  USER_UPDATE_ROLE: (userId: number | string) => `/admin/users/${userId}/role`,
  // Admin Product Management Endpoints (ProductController altında ama admin yetkisiyle)
  ADMIN_PRODUCTS_ALL: `/products/admin/all`, // GET
  ADMIN_PRODUCT_APPROVE: (productId: number) => `/products/${productId}/approve`, // PATCH
  ADMIN_PRODUCT_REJECT: (productId: number) => `/products/${productId}/reject`, // PATCH
  ADMIN_PRODUCT_DELETE: (productId: number) => `/products/${productId}`, // DELETE (Admin yetkisiyle)
  ADMIN_PRODUCT_ACTIVATE: (productId: number) => `/products/${productId}/activate-admin`, // PATCH
  // Admin Review Management Endpoints
  ADMIN_REVIEWS_ALL: `/reviews/admin/all`, // GET
  ADMIN_REVIEW_APPROVE: (reviewId: number) => `/reviews/admin/${reviewId}/approve`, // PATCH
  ADMIN_REVIEW_REJECT: (reviewId: number) => `/reviews/admin/${reviewId}/reject`, // PATCH
  ADMIN_REVIEW_DELETE: (reviewId: number) => `/reviews/admin/${reviewId}`, // DELETE
  // Admin Category Management Endpoints
  ADMIN_CATEGORIES_ALL: `/categories/admin/all`, // GET (veya sadece /categories ve query param ile)
  ADMIN_CATEGORY_CREATE: `/categories`, // POST (Admin yetkisiyle)
  ADMIN_CATEGORY_DETAIL: (categoryId: number) => `/categories/${categoryId}` // PUT, DELETE (Admin yetkisiyle)
};
console.log('API_ENDPOINTS: ADMIN_ENDPOINTS =', ADMIN_ENDPOINTS);

export const REVIEW_ENDPOINTS = {
  BASE: `/reviews`, // Genel /api/reviews yolu
  BY_PRODUCT: (productId: number) => `/reviews/product/${productId}`, // GET, POST
  DETAIL: (reviewId: number) => `/reviews/${reviewId}` // DELETE (kullanıcı kendi yorumunu siler)
};
console.log('API_ENDPOINTS: REVIEW_ENDPOINTS =', REVIEW_ENDPOINTS);

export const WISHLIST_ENDPOINTS = {
  BASE: `/wishlist`, // GET, POST
  BY_PRODUCT_ID: (productId: number) => `/wishlist/${productId}`, // DELETE
  CHECK_PRODUCT: (productId: number) => `/wishlist/check/${productId}` // GET
};
console.log('API_ENDPOINTS: WISHLIST_ENDPOINTS =', WISHLIST_ENDPOINTS);

export const COUPON_ENDPOINTS = {
  VALIDATE: (code: string) => `/coupons/validate/${code}`, // GET
  // Admin Coupon Endpoints
  ADMIN_BASE: `/coupons`, // GET (all), POST (create)
  ADMIN_DETAIL_BY_ID: (couponId: number) => `/coupons/${couponId}`, // GET, PUT, DELETE
  ADMIN_DETAIL_BY_CODE: (code: string) => `/coupons/code/${code}` // GET
};
console.log('API_ENDPOINTS: COUPON_ENDPOINTS =', COUPON_ENDPOINTS);

export const NOTIFICATION_ENDPOINTS = {
  MY_NOTIFICATIONS: `/notifications/my`, // GET
  MY_UNREAD_COUNT: `/notifications/my/unread-count`, // GET
  MARK_AS_READ: (notificationId: number) => `/notifications/${notificationId}/read`, // PATCH
  MARK_ALL_AS_READ: `/notifications/my/read-all`, // PATCH
  DELETE_NOTIFICATION: (notificationId: number) => `/notifications/${notificationId}` // DELETE
};
console.log('API_ENDPOINTS: NOTIFICATION_ENDPOINTS =', NOTIFICATION_ENDPOINTS);

export const SUBSCRIPTION_ENDPOINTS = {
  MY_STATUS: `/subscriptions/my-status`, // GET
  CHECKOUT_SESSION: `/subscriptions/checkout-session` // POST
};
console.log('API_ENDPOINTS: SUBSCRIPTION_ENDPOINTS =', SUBSCRIPTION_ENDPOINTS);

// FeelService tarafından kullanılan temel endpoint'ler
export const API_FEELS = '/feels';
export const API_FEELS_LIKE = (feelId: string | number) => `/feels/${feelId}/like`;

export const FEEL_ENDPOINTS = {
  BASE: `/feels`, // POST (create), GET (all public)
  DETAIL: (feelId: number) => `/feels/${feelId}`, // GET, DELETE (Seller/Admin)
  BY_PRODUCT: (productId: number) => `/feels/product/${productId}`, // GET
  BY_SELLER: (sellerId: number) => `/feels/seller/${sellerId}`, // GET
  MY_FEELS: `/feels/seller/my`, // GET (seller's own)
  LIKE: (feelId: number) => `/feels/${feelId}/like`, // POST, DELETE
  // Admin specific feel endpoints
  ADMIN_GET_ALL: `/feels/admin/all`, // GET - Admin için tüm feel'ler (aktif/pasif)
  ADMIN_UPDATE_STATUS: (feelId: number) => `/feels/admin/${feelId}/status`, // PATCH
  ADMIN_DELETE: (feelId: number) => `/feels/admin/${feelId}` // DELETE - Admin için silme (apidocs'ta /feels/{feelId} idi ama admin için ayrı olabilir)
  // Eğer /feels/{feelId} hem satıcı hem admin içinse ve yetkiyle ayrılıyorsa ADMIN_DELETE gereksiz.
  // Şimdilik apidocs'a göre DETAIL endpoint'i admin silmesi için de kullanılacak.
};
console.log('API_ENDPOINTS: FEEL_ENDPOINTS =', FEEL_ENDPOINTS);

export const SELLER_ENDPOINTS = {
  // Seller Product Management (SellerService içinde zaten /seller path'i kullanılıyor,
  // bu yüzden burada sadece alt yolları belirtmek yeterli olabilir veya tam yolları yazabiliriz)
  PRODUCTS_BASE: `/seller/products`,
  PRODUCT_DETAIL: (productId: number) => `/seller/products/${productId}`,
  MY_PRODUCTS: `/seller/products/my`,
  ACTIVATE_PRODUCT: (productId: number) => `/seller/products/${productId}/activate`,
  UPLOAD_IMAGE: (productId: number) => `/seller/products/${productId}/image`,
  // AI Features for Seller
  GENERATE_IMAGE: `/seller/products/generate-image`, // POST
  SET_AI_IMAGE: `/seller/products/set-ai-image`, // POST

  // Seller Order Management
  MY_ORDERS: `/orders/seller/my`, // GET - Satıcının kendi siparişleri (OrderController altındaki yeni path)
  ORDER_DETAIL: (orderId: number) => `/orders/${orderId}`, // GET - Genel sipariş detayı, @orderSecurity ile korunacak
  UPDATE_ORDER_STATUS: (orderId: number) => `/orders/${orderId}/status` // PATCH - Genel sipariş durumu güncelleme, @orderSecurity ile korunacak
  // Kargo takip no için de genel endpoint kullanılacak: ORDERS_ENDPOINTS.ORDER_TRACKING_UPDATE(orderId)
};
console.log('API_ENDPOINTS: SELLER_ENDPOINTS =', SELLER_ENDPOINTS);

export const AI_ENDPOINTS = {
  ENHANCE_PRODUCT_DETAILS: `/ai/enhance-product-details`, // POST
  GENERATE_IMAGE: `/ai/generate-image` // Bu endpoint AiFeaturesController'da da vardı, SellerProductController'dan taşınabilir.
                                        // Eğer SellerProductController'daki /seller/products/generate-image kullanılacaksa,
                                        // bu satır SELLER_ENDPOINTS altında kalmalı ve buradaki kaldırılmalı.
                                        // Şimdilik genel bir /ai/generate-image olduğunu varsayıyoruz.
};
console.log('API_ENDPOINTS: AI_ENDPOINTS =', AI_ENDPOINTS);
