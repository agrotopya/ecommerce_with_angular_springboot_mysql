// src/app/shared/models/review.model.ts

export interface ReviewRequest {
  rating: number; // 1-5
  comment?: string; // Optional
}

export interface ReviewResponseDto {
  id: number;
  rating: number;
  comment?: string;
  createdAt: string; // ISO Date string
  isApproved: boolean;
  productId: number;
  productName?: string; // Optional, as it might not always be returned or needed
  customerId: number;
  customerUsername: string;
}
