// src/app/features/products/product.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators'; // map operatörü import edildi
import { HttpParams } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { Product } from '../../shared/models/product.model';
import { Page } from '../../shared/models/page.model';
// PRODUCT_ENDPOINTS yerine SELLER_ENDPOINTS kullanılacak veya PRODUCT_ENDPOINTS'e eklenecek
import { PRODUCT_ENDPOINTS, SELLER_ENDPOINTS } from '../../core/constants/api-endpoints';

const PRODUCTS_API_PATH = '/products'; // Bu artık kullanılmıyor gibi, kaldırılabilir

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  private apiService = inject(ApiService);

  getPublicProducts(params?: HttpParams): Observable<Page<Product>> {
    // PRODUCTS_API_PATH yerine PRODUCT_ENDPOINTS.BASE kullanalım
    console.log('ProductService: Fetching public products with params:', params?.toString());
    return this.apiService.get<Page<Product>>(PRODUCT_ENDPOINTS.BASE, params);
  }

  getPublicProductById(id: number): Observable<Product> {
    // PRODUCTS_API_PATH yerine PRODUCT_ENDPOINTS.DETAIL kullanalım
    return this.apiService.get<Product>(PRODUCT_ENDPOINTS.DETAIL(id));
  }

  getPublicProductBySlug(slug: string): Observable<Product> {
    // PRODUCTS_API_PATH yerine PRODUCT_ENDPOINTS.BY_SLUG kullanalım
    // Zaten alttaki getProductBySlug ile aynı, biri kaldırılabilir veya bu public kalabilir.
    // Şimdilik bu kalsın, diğeriyle aynı işi yapıyor.
    return this.apiService.get<Product>(PRODUCT_ENDPOINTS.BY_SLUG(slug));
  }

  // getProductBySlug metodu getPublicProductBySlug ile aynı, biri tercih edilebilir.
  // Eğer farklı bir mantığı olacaksa kalabilir. Şimdilik yorum satırı yapalım veya kaldıralım.
  // getProductBySlug(slug: string): Observable<Product> {
  //   console.log(`ProductService: Fetching product by slug: ${slug}`);
  //   return this.apiService.get<Product>(PRODUCT_ENDPOINTS.BY_SLUG(slug));
  // }

  // Satıcının ürünlerini dropdown için getirir (sadece id ve name)
  // Bu metod SellerService içinde de olabilirdi.
  getProductsBySellerForDropdown(): Observable<Product[]> {
    // SellerService'teki getMyProducts tüm sayfalamayı ve detayları getiriyor.
    // Backend'de sadece id ve name dönen bir endpoint daha performanslı olabilir.
    // Şimdilik tüm ürünleri çekip frontend'de map'leyeceğiz.
    // TODO: Backend'e sadece id ve name dönen bir endpoint eklenirse burası optimize edilebilir.
    const params = new HttpParams().set('size', '1000'); // Çok sayıda ürün olabileceği varsayımıyla
    return this.apiService.get<Page<Product>>(SELLER_ENDPOINTS.MY_PRODUCTS, params).pipe( // PRODUCT_ENDPOINTS.SELLER_MY_PRODUCTS -> SELLER_ENDPOINTS.MY_PRODUCTS
      map((page: Page<Product>) => page.content.map(p => ({ id: p.id, name: p.name } as Product)))
      // Yukarıdaki map işlemi Product arayüzünün tüm alanlarını bekleyeceği için hata verebilir.
      // Sadece id ve name içeren yeni bir arayüz tanımlamak daha doğru olur.
      // Veya Product arayüzündeki diğer alanları opsiyonel yapmak.
      // Şimdilik Product[] döndürüyoruz, CreateFeelComponent'te sadece id ve name kullanılacak.
    );
  }
}
// src/app/features/products/product.service.ts
