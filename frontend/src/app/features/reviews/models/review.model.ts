// src/app/features/reviews/models/review.model.ts

export interface ReviewRequest {
  rating: number; // 1-5 arası zorunlu
  comment?: string; // Opsiyonel, max 5000 karakter
}

export interface ReviewResponseDto {
  id: number;
  rating: number;
  comment?: string;
  createdAt: string; // ISO Date string
  isApproved: boolean;
  productId: number;
  productName?: string; // Denormalized
  customerId: number; // veya UserResponseDto gibi bir tip olabilir
  customerUsername?: string; // Denormalized
}
