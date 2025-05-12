// src/app/shared/models/api-response.model.ts

/**
 * Generic API response structure.
 * @template T The type of the data payload.
 */
export interface ApiResponse<T = any> { // data için generic tip T kullanıldı
  success: boolean;
  message: string;
  data?: T; // Optional data payload
}
