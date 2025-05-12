// src/app/shared/models/feel.model.ts

import { Product } from './product.model';
import { User } from './user.model'; // Satıcı bilgisi için

export interface FeelResponseDto {
  id: number;
  title: string;
  description?: string;
  videoUrl: string;
  thumbnailUrl?: string;
  viewCount: number;
  likeCount: number;
  createdAt: string; // veya Date
  updatedAt: string; // veya Date
  sellerId: number;
  sellerUsername: string;
  sellerProfileImageUrl?: string; // Opsiyonel, satıcının profil resmi
  productId?: number; // İlişkili ürünün ID'si
  productSlug?: string; // İlişkili ürünün slug'ı
  productName?: string; // İlişkili ürünün adı
  productMainImageUrl?: string; // İlişkili ürünün ana resmi
  active: boolean; // Feel'in aktif olup olmadığı (isActive -> active olarak değiştirildi)
  // Backend'den gelen UserDto'ya göre seller bilgileri eklenebilir
  // seller?: Partial<User>; // Veya daha spesifik bir SellerInfo arayüzü
  isLikedByCurrentUser?: boolean; // Mevcut kullanıcının beğenip beğenmediği
}

export interface CreateFeelRequestDto {
  title: string;
  description?: string;
  productId?: number;
  // videoFile ve thumbnailFile FormData ile gönderilecek, bu DTO'da olmayacak
}

// Admin tarafından feel durumunu güncellemek için
export interface UpdateFeelStatusRequestDto {
  isActive: boolean;
}
