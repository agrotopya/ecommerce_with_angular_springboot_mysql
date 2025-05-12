import { inject, Injectable, signal, effect } from '@angular/core'; // effect eklendi
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap, catchError, of, map, BehaviorSubject } from 'rxjs';
import { WISHLIST_ENDPOINTS } from '@core/constants/api-endpoints'; // API_ENDPOINTS -> WISHLIST_ENDPOINTS
import { ApiService } from '@core/services/api.service';
import { NotificationService } from '@core/services/notification.service';
import { Product } from '@shared/models/product.model';
import { Page } from '@shared/models/page.model'; // PaginatedResponse -> Page
import { WishlistRequest, WishlistCheckResponse } from '@shared/models/wishlist.model';
import { AuthService } from '@core/services/auth.service';

@Injectable({
  providedIn: 'root',
})
export class WishlistService {
  private apiService = inject(ApiService);
  private notificationService = inject(NotificationService);
  private authService = inject(AuthService);

  // Ürünlerin istek listesindeki durumunu tutmak için bir BehaviorSubject
  // Key: productId, Value: boolean (isInWishlist)
  private productWishlistStatus = new BehaviorSubject<Map<number, boolean>>(new Map());
  public productWishlistStatus$ = this.productWishlistStatus.asObservable();

  constructor() {
    // Kullanıcı giriş yaptığında veya çıkış yaptığında durumu sıfırla/güncelle
    effect(() => {
      const isAuthenticated = this.authService.isAuthenticated();
      if (!isAuthenticated) {
        this.productWishlistStatus.next(new Map());
      } else {
        // Giriş yapıldığında, önemli ürünlerin durumunu (örneğin görüntülenenler)
        // kontrol etmek için bir mekanizma eklenebilir, ancak bu genellikle
        // bileşen bazında yapılır.
      }
    });
  }

  /**
   * Kullanıcının istek listesindeki ürünleri getirir.
   */
  getMyWishlist(
    page: number = 0,
    size: number = 10,
    sort: string = 'addedAt,desc'
  ): Observable<Page<Product> | null> { // PaginatedResponse -> Page
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    return this.apiService.get<Page<Product>>(WISHLIST_ENDPOINTS.BASE, params).pipe( // PaginatedResponse -> Page
      catchError(error => {
        this.notificationService.showError('Failed to load wishlist.');
        console.error('Error loading wishlist:', error);
        return of(null);
      })
    );
  }

  /**
   * Bir ürünü istek listesine ekler.
   */
  addProductToWishlist(productId: number): Observable<Product | null> {
    const payload: WishlistRequest = { productId };
    return this.apiService.post<Product>(WISHLIST_ENDPOINTS.BASE, payload).pipe(
      tap((product) => {
        if (product) {
          this.notificationService.showSuccess(`${product.name} added to wishlist!`);
          this.updateProductWishlistStatus(productId, true);
        }
      }),
      catchError(error => {
        this.notificationService.showError('Failed to add product to wishlist.');
        console.error('Error adding to wishlist:', error);
        return of(null);
      })
    );
  }

  /**
   * Bir ürünü istek listesinden kaldırır.
   */
  removeProductFromWishlist(productId: number): Observable<void | null> {
    return this.apiService.delete<void>(WISHLIST_ENDPOINTS.BY_PRODUCT_ID(productId)).pipe(
      tap(() => {
        this.notificationService.showSuccess('Product removed from wishlist.');
        this.updateProductWishlistStatus(productId, false);
      }),
      catchError(error => {
        this.notificationService.showError('Failed to remove product from wishlist.');
        console.error('Error removing from wishlist:', error);
        return of(null);
      })
    );
  }

  /**
   * Bir ürünün istek listesinde olup olmadığını kontrol eder.
   * Bu metod, tek bir ürünün durumunu kontrol etmek ve BehaviorSubject'i güncellemek için kullanılır.
   */
  checkAndStoreProductWishlistStatus(productId: number): Observable<boolean> {
    if (!this.authService.isAuthenticated()) { // isAuthenticatedSignal -> isAuthenticated
      this.updateProductWishlistStatus(productId, false);
      return of(false);
    }
    return this.apiService.get<WishlistCheckResponse>(WISHLIST_ENDPOINTS.CHECK_PRODUCT(productId)).pipe(
      map(response => {
        this.updateProductWishlistStatus(productId, response.isInWishlist);
        return response.isInWishlist;
      }),
      catchError(error => {
        console.error('Error checking wishlist status for product:', productId, error);
        // Hata durumunda false döndür ve durumu güncelleme
        this.updateProductWishlistStatus(productId, false); // veya mevcut durumu koru
        return of(false);
      })
    );
  }

  /**
   * Belirli bir ürünün istek listesi durumunu BehaviorSubject üzerinden günceller.
   */
  private updateProductWishlistStatus(productId: number, isInWishlist: boolean): void {
    const currentMap = this.productWishlistStatus.getValue();
    currentMap.set(productId, isInWishlist);
    this.productWishlistStatus.next(new Map(currentMap)); // Yeni bir Map referansı ile BehaviorSubject'i tetikle
  }

  /**
   * Belirli bir ürünün istek listesi durumunu BehaviorSubject'ten senkron olarak alır.
   * Bu, anlık kontrol için kullanılabilir ancak reaktif olmayan bir yaklaşımdır.
   * Mümkün olduğunca productWishlistStatus$ observable'ını kullanın.
   */
  getProductWishlistStatusSignal(productId: number): boolean {
    return this.productWishlistStatus.getValue().get(productId) || false;
  }

  /**
   * Birden fazla ürünün istek listesi durumunu kontrol eder ve saklar.
   * Özellikle ürün listeleri yüklenirken kullanılabilir.
   */
  checkAndStoreMultipleProductWishlistStatus(productIds: number[]): void {
    if (!this.authService.isAuthenticated() || productIds.length === 0) { // isAuthenticatedSignal -> isAuthenticated
      const currentMap = this.productWishlistStatus.getValue();
      productIds.forEach(id => currentMap.set(id, false));
      this.productWishlistStatus.next(new Map(currentMap));
      return;
    }

    // Backend'den toplu kontrol için bir endpoint yoksa, tek tek çağrı yapılabilir.
    // Bu, çok sayıda ürün için performans sorunu yaratabilir.
    // İdealde backend'de `/api/wishlist/check-multiple` gibi bir endpoint olmalı.
    // Şimdilik tek tek çağrı yapıyoruz.
    productIds.forEach(id => {
      // Eğer durum zaten biliniyorsa tekrar sorgulama (opsiyonel optimizasyon)
      // if (this.productWishlistStatus.getValue().has(id)) return;

      this.checkAndStoreProductWishlistStatus(id).subscribe();
    });
  }
}
