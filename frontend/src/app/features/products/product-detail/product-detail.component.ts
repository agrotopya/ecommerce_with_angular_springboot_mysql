// src/app/features/products/product-detail/product-detail.component.ts
import { Component, inject, OnInit, signal, WritableSignal } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common'; // CurrencyPipe eklendi
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProductService } from '../product.service';
import { Product, isProductActive, isProductApproved } from '../../../shared/models/product.model';
import { NgOptimizedImage } from '@angular/common';
import { EMPTY, Observable, switchMap, tap } from 'rxjs'; // tap eklendi
import { Nl2brPipe } from '../../../shared/pipes/nl2br.pipe';
import { CartService } from '../../cart/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { WishlistService } from '../../wishlist/services/wishlist.service'; // WishlistService eklendi
import { NotificationService } from '../../../core/services/notification.service'; // NotificationService eklendi
import { ProductReviewsComponent } from '../../reviews/components/product-reviews/product-reviews.component'; // Eklendi

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, NgOptimizedImage, CurrencyPipe, Nl2brPipe, ProductReviewsComponent], // ProductReviewsComponent eklendi
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss'],
})
export class ProductDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  public authService = inject(AuthService);
  private wishlistService = inject(WishlistService); // WishlistService inject edildi
  private notificationService = inject(NotificationService); // NotificationService inject edildi
  private router = inject(Router);

  product: WritableSignal<Product | null> = signal(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);
  isWishlisted = signal(false);
  isTogglingWishlist = signal(false);


  // Alternatif: Observable ve async pipe ile
  // product$: Observable<Product | null> | undefined;

  ngOnInit(): void {
    this.route.paramMap.pipe(
      switchMap(params => {
        const slug = params.get('slug');
        if (slug) {
          this.isLoading.set(true);
          this.isWishlisted.set(false); // Reset on new product load
          console.log(`ProductDetail: Loading product with slug: ${slug}`); // LOG
          return this.productService.getPublicProductBySlug(slug);
        }
        return EMPTY;
      }),
      tap(productDetails => {
        if (productDetails && productDetails.id && this.authService.isAuthenticated()) {
          this.wishlistService.checkAndStoreProductWishlistStatus(productDetails.id).subscribe(status => {
            this.isWishlisted.set(status);
          });
        }
      })
    ).subscribe({
      next: (productDetails) => {
        this.product.set(productDetails);
        this.isLoading.set(false);
        console.log('ProductDetail: Product Loaded:', JSON.stringify(productDetails)); // DETAYLI LOG
        console.log('ProductDetail: Product active:', productDetails.active);
        console.log('ProductDetail: Product approved:', productDetails.approved);
        console.log('ProductDetail: Product stock:', productDetails.stock);
      },
      error: (err: any) => { // err tip eklendi
        console.error('ProductDetail: Error loading product details:', err);
        this.isLoading.set(false);
        this.product.set(null);
      }
    });
  }

  loadProductDetails(slug: string): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.productService.getPublicProductBySlug(slug).subscribe({
      next: (data) => {
        this.product.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Failed to load product details. The product may not exist or there was a network issue.');
        this.isLoading.set(false);
        console.error(`Error loading product with slug ${slug}:`, err);
        // Opsiyonel: Kullanıcıyı ürünler listesine veya 404 sayfasına yönlendir
        // this.router.navigate(['/products']);
      }
    });
  }

  // Yardımcı fonksiyonları component içinde kullanılabilir hale getir
  isProductActive(product: Product): boolean {
    return isProductActive(product);
  }

  isProductApproved(product: Product): boolean {
    return isProductApproved(product);
  }


  // Sepete ekleme fonksiyonu (ileride eklenecek)

  addToCartHandler(): void {
    const currentProduct = this.product();
    if (!currentProduct) return;

    if (!this.authService.isAuthenticated()) {
      this.notificationService.showInfo('Please login to add items to your cart.');
      this.router.navigate(['/auth/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }

    console.log('Adding to cart from detail:', currentProduct.id);
    this.cartService.addItem({ productId: currentProduct.id, quantity: 1 }).subscribe({
      next: () => {
        this.notificationService.showSuccess(`${currentProduct.name} added to cart!`);
        console.log(`Product ${currentProduct.id} added to cart from detail.`);
      },
      error: (err: any) => { // err tip eklendi
        this.notificationService.showError('Failed to add product to cart.');
        console.error('Failed to add product from detail:', err);
      }
    });
  }

  toggleWishlist(): void {
    const currentProduct = this.product();
    if (!currentProduct || currentProduct.id === undefined) return;

    if (!this.authService.isAuthenticated()) {
      this.notificationService.showInfo('Please login to manage your wishlist.');
      this.router.navigate(['/auth/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }

    this.isTogglingWishlist.set(true);
    const currentStatus = this.isWishlisted();

    if (currentStatus) {
      this.wishlistService.removeProductFromWishlist(currentProduct.id).subscribe({
        next: () => {
          this.isWishlisted.set(false);
          this.isTogglingWishlist.set(false);
          // NotificationService zaten WishlistService içinde çağrılıyor.
        },
        error: (err: any) => {
          this.isTogglingWishlist.set(false);
          console.error('Error removing from wishlist in ProductDetailComponent:', err);
        }
      });
    } else {
      this.wishlistService.addProductToWishlist(currentProduct.id).subscribe({
        next: (product: Product | null) => {
          this.isWishlisted.set(true);
          this.isTogglingWishlist.set(false);
          // NotificationService zaten WishlistService içinde çağrılıyor.
        },
        error: (err: any) => {
          this.isTogglingWishlist.set(false);
          console.error('Error adding to wishlist in ProductDetailComponent:', err);
        }
      });
    }
  }
}
