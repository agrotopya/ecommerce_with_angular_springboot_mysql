import { Product } from './product.model'; // Product modelini import ediyoruz

/**
 * İstek listesine ürün eklemek için kullanılan istek modeli.
 */
export interface WishlistRequest {
  productId: number;
}

/**
 * Bir ürünün istek listesinde olup olmadığını kontrol etme yanıtı için model.
 */
export interface WishlistCheckResponse {
  isInWishlist: boolean;
  productId: number; // Hangi ürün olduğu bilgisi de faydalı olabilir
}

// İstek listesi yanıtı genellikle ürün listesi (PaginatedResponse<Product>)
// veya tek bir ürün (Product) şeklinde olacağından ayrı bir WishlistResponse
// modeline bu aşamada ihtiyaç olmayabilir. Servis metodları doğrudan Product
// veya PaginatedResponse<Product> döndürebilir.
