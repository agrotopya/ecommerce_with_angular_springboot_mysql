// src/app/features/products/product-card/product-card.component.ts
import { Component, Input, Output, EventEmitter, inject, signal, OnInit, OnDestroy } from '@angular/core'; // OnInit, OnDestroy eklendi
import { CommonModule, NgOptimizedImage } from '@angular/common'; // NgOptimizedImage eklendi
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs'; // Subscription eklendi
import { Product, isProductActive, isProductApproved } from '../../../shared/models/product.model';
import { CartService } from '../../cart/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { WishlistService } from '../../wishlist/services/wishlist.service'; // WishlistService eklendi
import { NotificationService } from '../../../core/services/notification.service'; // NotificationService eklendi

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, RouterLink, NgOptimizedImage], // NgOptimizedImage eklendi
  templateUrl: './product-card.component.html',
  styleUrls: ['./product-card.component.scss'],
})
export class ProductCardComponent implements OnInit, OnDestroy { // OnInit, OnDestroy implement edildi
  @Input({ required: true }) product!: Product;
  @Input() isWishlistedInput: boolean = false; // Dışarıdan gelen istek listesi durumu (signal kaldırıldı)
  @Output() toggleWishlist = new EventEmitter<number>();

  private cartService = inject(CartService);
  public authService = inject(AuthService);
  private wishlistService = inject(WishlistService); // WishlistService inject edildi
  private notificationService = inject(NotificationService); // NotificationService inject edildi
  private router = inject(Router);

  isTogglingWishlist = signal(false);
  // isWishlistedInput'u doğrudan kullanmak yerine, bileşen içinde bir signal tutup
  // OnInit'te WishlistService'ten gelen güncel durumu kontrol edebiliriz.
  // Ancak şimdilik parent component'ten gelen değere ve WishlistService'teki BehaviorSubject'e güveniyoruz.
  // Bu component'in kendi state'ini yönetmesi için isWishlisted adında bir signal oluşturabiliriz.
  // isWishlisted = signal(this.isWishlistedInput); // Başlangıç değeri input'tan
  private wishlistStatusSubscription: Subscription | undefined;

  ngOnInit(): void {
    // Eğer parent component isWishlistedInput'u sağlamıyorsa (varsayılan false ise)
    // ve kullanıcı giriş yapmışsa, ürünün istek listesi durumunu kontrol et.
    if (this.product && this.product.id && this.authService.isAuthenticated()) {
      // WishlistService'teki BehaviorSubject'e abone olarak anlık durumu al
      this.wishlistStatusSubscription = this.wishlistService.productWishlistStatus$.subscribe(statusMap => {
        if (statusMap.has(this.product.id)) {
          this.isWishlistedInput = statusMap.get(this.product.id)!;
        } else {
          // Eğer map'te yoksa, servisten kontrol et (bu genellikle ilk yüklemede olur)
          // Ancak bu, her kart için ayrı bir subscribe oluşturur, dikkatli olunmalı.
          // Daha iyi bir yaklaşım, parent component'in (örn. ProductListComponent)
          // tüm ürünler için durumu toplu olarak çekip kartlara iletmesidir.
          // Şimdilik, eğer parent'tan bir değer gelmediyse ve map'te yoksa,
          // checkAndStoreProductWishlistStatus'u çağırabiliriz.
          if (this.isWishlistedInput === false) { // Sadece başlangıçta false ise sorgula
             // Bu subscribe'ı da yönetmek gerekebilir, ancak genellikle kısa ömürlüdür.
             // Daha güvenli olması için bunu da bir Subscription nesnesine ekleyip OnDestroy'da unsubscribe edebiliriz.
             // Şimdilik basit tutuyoruz.
             this.wishlistService.checkAndStoreProductWishlistStatus(this.product.id).subscribe(status => {
               this.isWishlistedInput = status;
             });
          }
        }
      });
    }
  }

  ngOnDestroy(): void {
    if (this.wishlistStatusSubscription) {
      this.wishlistStatusSubscription.unsubscribe();
    }
  }

  // Yardımcı fonksiyonları component içinde kullanılabilir hale getir
  isProductActive(product: Product): boolean {
    return isProductActive(product);
  }

  isProductApproved(product: Product): boolean {
    return isProductApproved(product);
  }

  onAddToCart(event: MouseEvent): void {
    event.preventDefault(); // Linkin tetiklenmesini engelle
    event.stopPropagation(); // Olayın yukarıya yayılmasını engelle

    if (!this.authService.isAuthenticated()) {
      // Kullanıcıyı login sayfasına yönlendir
      this.router.navigate(['/auth/login']);
      return;
    }

    console.log('Adding to cart from card:', this.product.id);
    this.cartService.addItem({ productId: this.product.id, quantity: 1 }).subscribe({
      next: () => {
        // NotificationService üzerinden bildirim gösterilebilir.
        // this.notificationService.showSuccess(`${this.product.name} added to cart!`);
        console.log(`Product ${this.product.id} added to cart from card.`);
      },
      error: (err) => {
        // this.notificationService.showError('Failed to add product to cart.');
        console.error('Failed to add product from card:', err);
      }
    });
  }

  onToggleWishlist(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();

    if (!this.authService.isAuthenticated()) {
      this.notificationService.showInfo('Please login to manage your wishlist.');
      this.router.navigate(['/auth/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }

    if (!this.product || this.product.id === undefined) return;

    this.isTogglingWishlist.set(true);

    const currentStatus = this.isWishlistedInput;

    if (currentStatus) {
      this.wishlistService.removeProductFromWishlist(this.product.id).subscribe({
        next: () => {
          // WishlistService içindeki BehaviorSubject durumu güncelleyeceği için
          // ve OnInit'te buna abone olduğumuz için isWishlistedInput otomatik güncellenecektir.
          this.isTogglingWishlist.set(false);
          this.toggleWishlist.emit(this.product.id);
        },
        error: (err: any) => {
          this.isTogglingWishlist.set(false);
          console.error('Error removing from wishlist in ProductCardComponent:', err);
        }
      });
    } else {
      this.wishlistService.addProductToWishlist(this.product.id).subscribe({
        next: (product: Product | null) => {
          // WishlistService içindeki BehaviorSubject durumu güncelleyeceği için
          // ve OnInit'te buna abone olduğumuz için isWishlistedInput otomatik güncellenecektir.
          this.isTogglingWishlist.set(false);
          this.toggleWishlist.emit(this.product.id);
        },
        error: (err: any) => {
          this.isTogglingWishlist.set(false);
          console.error('Error adding to wishlist in ProductCardComponent:', err);
        }
      });
    }
  }
}
