// src/app/features/wishlist/models/wishlist.model.ts
import { Product } from "../../../shared/models/product.model"; // ProductResponse -> Product

export interface WishlistRequest {
  productId: number;
}

// WishlistResponse genellikle ProductResponse[] veya Page<ProductResponse> şeklinde olur.
// apidocs.md 7.1'e göre Page<ProductResponse> dönüyor.
// 7.2'ye göre ProductResponse dönüyor.
// Bu, ekleme sonrası tek bir ürün bilgisi döndüğü anlamına gelebilir.
// Ayrı bir WishlistResponseDto tanımlamaya gerek olmayabilir.

export interface IsInWishlistResponse {
  isInWishlist: boolean;
}
