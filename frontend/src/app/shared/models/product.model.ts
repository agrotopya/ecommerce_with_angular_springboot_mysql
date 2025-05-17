// src/app/shared/models/product.model.ts
import { CategoryResponseDto } from "./category.model"; // category.model olarak güncellendi

// Product arayüzü, backend'den gelen yanıta ve apidocs.md'ye göre güncellendi
export interface Product {
  brand: any; // TODO: Tipini belirle (string | null olabilir)
  stockQuantity: any; // TODO: Tipini belirle (number | null olabilir)
  id: number;
  name: string;
  slug: string;
  description: string;
  price: number;
  stock: number; // stockQuantity -> stock (backend yanıtına göre)
  sku?: string | null;
  mainImageUrl?: string | null; // Backend'deki ProductResponseDto ile uyumlu hale getirildi
  productImages?: ProductImage[]; // ProductImageDto -> ProductImage olarak güncellendi
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
  originalPrice?: number;
  category?: CategoryResponseDto;
  tags?: string[];
  attributes?: ProductAttributeDto[];
  isFeatured?: boolean;
  rejectionReason?: string;
  reviewSummaryAi?: string | null;
  aiGeneratedImageUrl?: string | null;
}

// Dropdown için sadece id ve name içeren arayüz
export interface ProductSelectItem {
  id: number;
  name: string;
}

// ProductRequest arayüzü, backend'in beklediği alanlara göre güncellendi
export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  stock: number; // stockQuantity -> stock
  categoryId: number;
  brand?: string;
  sku?: string;
  active?: boolean;
}

export interface ProductImage {
  id?: number;
  imageUrl: string;
  isPrimary: boolean;
  altText?: string;
  displayOrder?: number;
}

export interface ProductAttributeDto {
  name: string;
  value: string;
}

// For admin approval
export interface ProductApprovalRequestDto {
  isApproved: boolean;
  rejectionReason?: string;
}

// Yardımcı fonksiyonlar güncellenmiş Product arayüzüne göre güncellendi.
export function isProductApproved(product: Product): boolean {
  return product.approved === true;
}

export function isProductActive(product: Product): boolean {
  return product.active === true;
}
