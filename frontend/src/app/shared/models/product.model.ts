// src/app/shared/models/product.model.ts
import { CategoryResponseDto } from "./category.model"; // category.model olarak güncellendi

// Product arayüzü, backend'den gelen yanıta ve apidocs.md'ye göre güncellendi
export interface Product {
  brand: any;
  stockQuantity: any;
  id: number;
  name: string;
  slug: string;
  description: string;
  price: number;
  stock: number; // stockQuantity -> stock (backend yanıtına göre)
  sku?: string | null;
  mainImageUrl?: string | null; // Backend'deki ProductResponseDto ile uyumlu hale getirildi
  productImages?: ProductImage[]; // ProductImageDto -> ProductImage olarak güncellendi
  // imageUrl ve imageUrls alanları kaldırıldı, mainImageUrl ve productImages kullanılacak
  createdAt: string;
  updatedAt: string;
  averageRating?: number;
  reviewCount?: number;
  categoryId: number;
  categoryName: string;
  categorySlug: string; // apidocs'ta var, eklendi
  sellerId: number;
  sellerUsername: string; // sellerName -> sellerUsername (backend yanıtına göre)
  active: boolean; // isActive -> active (backend yanıtına göre)
  approved: boolean; // isApproved -> approved (backend yanıtına göre)
  salesCount?: number; // Eklendi
  // Opsiyonel veya frontend'de olmayan alanlar:
  originalPrice?: number;
  category?: CategoryResponseDto; // Backend'den gelmiyor, categoryId ve categoryName var
  tags?: string[];
  // images?: ProductImageDto[]; // productImages ile değiştirildi veya birleştirildi
  attributes?: ProductAttributeDto[];
  isFeatured?: boolean;
  rejectionReason?: string;
  reviewSummaryAi?: string | null;
  aiGeneratedImageUrl?: string | null;
}

// ProductRequest arayüzü, backend'in beklediği alanlara göre güncellendi
export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  stock: number; // stockQuantity -> stock
  categoryId: number;
  brand?: string; // apidocs.md ProductRequest'te yok, frontend.md'de var. Opsiyonel bırakalım.
  sku?: string; // apidocs.md ProductRequest'te yok, frontend.md'de var. Opsiyonel bırakalım.
  active?: boolean; // Eklendi
  // tags, images, attributes backend ProductRequest DTO'sunda yoksa kaldırılmalı.
  // Şimdilik apidocs.md'deki ProductRequest'e göre (4.2.1) bu alanlar yok.
  // imageUrl alanı da ProductRequest'te yok, görsel ayrı yükleniyor.
  // isFeatured da ProductRequest'te yok, backend default atıyor.
}

export interface ProductImage { // ProductImageDto -> ProductImage olarak güncellendi
  id?: number;
  imageUrl: string;
  isPrimary: boolean;
  altText?: string;
  displayOrder?: number;
}

export interface ProductAttributeDto { // Bu DTO da kullanılabilir
  name: string; // e.g., "Color", "Size"
  value: string; // e.g., "Red", "XL"
}

// For admin approval
export interface ProductApprovalRequestDto {
  isApproved: boolean;
  rejectionReason?: string;
}

// PaginatedProducts, Pageable ve Sort arayüzleri kaldırıldı.
// Page<T> arayüzü page.model.ts dosyasında tanımlı.

// Yardımcı fonksiyonlar güncellenmiş Product arayüzüne göre güncellendi.
export function isProductApproved(product: Product): boolean {
  return product.approved === true; // isApproved -> approved
}

export function isProductActive(product: Product): boolean {
  return product.active === true; // isActive -> active
}
