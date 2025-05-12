// src/app/shared/models/cart.model.ts

/**
 * Represents a single item within the shopping cart.
 * Based on backend's CartItemResponse.java
 */
export interface CartItem {
  cartItemId: number;         // Corresponds to CartItem ID'si
  quantity: number;
  addedAt: string;            // Corresponds to LocalDateTime, typically string in ISO format
  productId: number;
  productName?: string;       // Optional, for display convenience
  productSlug?: string;       // Optional, for linking
  productPrice: number;       // Corresponds to BigDecimal
  productStock?: number;      // Optional, for displaying stock info
  productImageUrl?: string;   // Optional
  itemTotal: number;          // Corresponds to BigDecimal (quantity * productPrice)
}

/**
 * Represents the entire shopping cart.
 * Based on backend's CartResponse.java
 */
export interface Cart {
  cartId: number;
  userId: number;
  updatedAt: string;          // Corresponds to LocalDateTime
  items: CartItem[];
  cartSubtotal: number;       // Corresponds to BigDecimal, subtotal of all items
  totalItems: number;         // Total quantity of all items in the cart
}

/**
 * Represents the request payload for adding an item to the cart.
 * Based on backend's AddToCartRequest.java
 */
export interface AddToCartRequest {
  productId: number;
  quantity: number;
}

/**
 * Represents the request payload for updating the quantity of an item in the cart.
 * Based on backend's UpdateCartItemRequest.java
 */
export interface UpdateCartItemRequest {
  quantity: number;
}

/**
 * Generic API response structure from the backend (if needed for cart operations).
 * Based on backend's ApiResponse.java
 */
export interface ApiResponse<T = any> { // T can be used for specific data types
  success: boolean;
  message: string;
  data?: T; // Optional data carrier
}