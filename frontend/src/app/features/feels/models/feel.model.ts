// src/app/features/feels/models/feel.model.ts

export interface CreateFeelRequestDto {
  title: string; // Zorunlu
  description?: string; // Opsiyonel
  productId: number; // Zorunlu, Feel'in ilişkilendirileceği ürün ID'si
  // videoFile ve thumbnailFile FormData ile gönderilecek, DTO'da değil.
}

export interface FeelResponseDto {
  id: number;
  title: string;
  description?: string;
  videoUrl: string;
  thumbnailUrl?: string;
  productId: number;
  productName?: string; // Denormalized
  productSlug?: string; // Denormalized
  sellerId: number;
  sellerUsername?: string; // Denormalized
  createdAt: string; // ISO Date string
  updatedAt: string; // ISO Date string
  views: number;
  likes: number;
  isActive: boolean;
}
