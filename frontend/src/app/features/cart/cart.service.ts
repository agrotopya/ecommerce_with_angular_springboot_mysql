// src/app/features/cart/cart.service.ts
import { Injectable, inject, signal, WritableSignal, effect } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { Cart, AddToCartRequest, UpdateCartItemRequest, CartItem } from '../../shared/models/cart.model';
import { CART_ENDPOINTS } from '../../core/constants/api-endpoints';
// import { NotificationService } from '../../core/services/notification.service'; // İleride eklenecek

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private apiService = inject(ApiService);
  private authService = inject(AuthService); // Kullanıcı login mi diye kontrol için
  // private notificationService = inject(NotificationService); // İleride

  // Sepet durumunu reaktif olarak tutmak için signal
  public cart: WritableSignal<Cart | null> = signal(null);
  public totalCartItems = signal(0); // Header'da göstermek için

  constructor() {
    // Kullanıcı login/logout olduğunda sepet durumunu yönet
    effect(() => {
      const isAuth = this.authService.isAuthenticated(); // Sinyalin güncel değerini al
      console.log('CartService: isAuthenticated effect triggered. Is authenticated:', isAuth);
      if (isAuth) {
        this.loadCart().subscribe(); // Yükleme işlemini başlat
      } else {
        this.cart.set(null);
        this.updateTotalItemsSignal();
      }
    });
  }

  private updateTotalItemsSignal(): void {
    const currentCart = this.cart();
    if (currentCart && currentCart.items) {
      this.totalCartItems.set(currentCart.items.reduce((sum, item) => sum + item.quantity, 0));
    } else {
      this.totalCartItems.set(0);
    }
  }

  loadCart(): Observable<Cart> {
    console.log('CartService: Loading cart from API.');
    return this.apiService.get<Cart>(CART_ENDPOINTS.BASE).pipe(
      tap(loadedCart => {
        this.cart.set(loadedCart);
        this.updateTotalItemsSignal();
        console.log('CartService: Cart loaded:', loadedCart);
      })
    );
  }

  addItem(itemRequest: AddToCartRequest): Observable<Cart> {
    console.log('CartService: Adding item to cart:', itemRequest);
    return this.apiService.post<Cart>(CART_ENDPOINTS.ITEMS, itemRequest).pipe(
      tap(updatedCart => {
        this.cart.set(updatedCart);
        this.updateTotalItemsSignal();
        // this.notificationService.showSuccess('Product added to cart!');
        console.log('CartService: Item added, cart updated:', updatedCart);
      })
    );
  }

  updateItemQuantity(productId: number, quantityRequest: UpdateCartItemRequest): Observable<Cart> {
    console.log(`CartService: Updating item quantity for product ${productId}:`, quantityRequest);
    return this.apiService.put<Cart>(CART_ENDPOINTS.ITEM_DETAIL(productId), quantityRequest).pipe(
      tap(updatedCart => {
        this.cart.set(updatedCart);
        this.updateTotalItemsSignal();
        console.log('CartService: Item quantity updated, cart updated:', updatedCart);
      })
    );
  }

  removeItem(productId: number): Observable<Cart> {
    console.log(`CartService: Removing item with product ID ${productId} from cart.`);
    return this.apiService.delete<Cart>(CART_ENDPOINTS.ITEM_DETAIL(productId)).pipe(
      tap(updatedCart => {
        this.cart.set(updatedCart);
        this.updateTotalItemsSignal();
        // this.notificationService.showSuccess('Product removed from cart.');
        console.log('CartService: Item removed, cart updated:', updatedCart);
      })
    );
  }

  clearCart(): Observable<any> {
    console.log('CartService: Clearing cart.');
    return this.apiService.delete<any>(CART_ENDPOINTS.BASE).pipe(
      tap(() => {
        this.cart.set(null); // API isteği başarılı olursa sepeti null yap
        this.updateTotalItemsSignal();
        console.log('CartService: Cart cleared.');
      })
    );
  }

  // Helper: Sepette belirli bir ürün var mı? Varsa CartItem döner.
  findItemInCart(productId: number): CartItem | undefined {
    return this.cart()?.items.find(item => item.productId === productId);
  }
}