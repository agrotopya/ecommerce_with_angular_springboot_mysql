// src/app/features/products/product-detail/product-detail.component.ts
import { Component, inject, OnInit, signal, WritableSignal, effect } from '@angular/core';
import { CommonModule, CurrencyPipe, NgOptimizedImage } from '@angular/common'; // NgOptimizedImage eklendi
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProductService } from '../product.service';
import { CategoryService } from '../../categories/category.service'; // CategoryService eklendi
import { CategoryResponseDto } from '../../../shared/models/category.model'; // CategoryResponseDto eklendi
import { Product, isProductActive, isProductApproved, ProductImage } from '../../../shared/models/product.model';
import { EMPTY, switchMap, tap, forkJoin, of } from 'rxjs'; // forkJoin ve of eklendi
import { Nl2brPipe } from '../../../shared/pipes/nl2br.pipe';
import { CartService } from '../../cart/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { WishlistService } from '../../wishlist/services/wishlist.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ProductReviewsComponent } from '../../reviews/components/product-reviews/product-reviews.component';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, NgOptimizedImage, CurrencyPipe, Nl2brPipe, ProductReviewsComponent],
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss'],
})
export class ProductDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  public authService = inject(AuthService);
  private wishlistService = inject(WishlistService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);

  product: WritableSignal<Product | null> = signal(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);
  isWishlisted = signal(false);
  isTogglingWishlist = signal(false);
  currentDisplayImageUrl = signal<string | null>(null);
  breadcrumbs: WritableSignal<Array<{ name: string; slug: string | null; isLink: boolean }>> = signal([]); // Breadcrumb sinyali

  private categoryService = inject(CategoryService); // CategoryService inject edildi

  constructor() {
    // product sinyali değiştiğinde ana görseli ayarla
    effect(() => {
      const p = this.product();
      if (p) {
        if (p.mainImageUrl) {
          this.currentDisplayImageUrl.set(p.mainImageUrl);
        } else if (p.productImages && p.productImages.length > 0) {
          const primaryImage = p.productImages.find(img => img.isPrimary);
          this.currentDisplayImageUrl.set(primaryImage ? primaryImage.imageUrl : p.productImages[0].imageUrl);
        } else {
          this.currentDisplayImageUrl.set(null); // Veya placeholder
        }
      } else {
        this.currentDisplayImageUrl.set(null); // Ürün null ise gösterilecek görsel de null
      }
    });
  }

  ngOnInit(): void {
    this.route.paramMap.pipe(
      switchMap(params => {
        const slug = params.get('slug');
        if (slug) {
          this.isLoading.set(true);
          this.isWishlisted.set(false);
          this.product.set(null);
          this.currentDisplayImageUrl.set(null);
          console.log(`ProductDetail: Loading product with slug: ${slug}`);
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
        console.log('ProductDetail: Product Loaded:', productDetails ? productDetails.name : 'null');
        if (productDetails) {
          this.buildBreadcrumbs(productDetails);
          if (productDetails.reviewSummaryAi) {
            console.log('ProductDetail: reviewSummaryAi RAW:', productDetails.reviewSummaryAi);
          } else {
            console.log('ProductDetail: reviewSummaryAi is missing or null/undefined in productDetails.');
          }
        }
      },
      error: (err: any) => {
        console.error('ProductDetail: Error loading product details:', err);
        this.isLoading.set(false);
        this.product.set(null);
        this.errorMessage.set(err.error?.message || 'Failed to load product details.');
      }
    });
  }

  // loadProductDetails metodu ngOnInit içindeki akışla birleşti.
  // Eğer ayrı bir 'Try Again' butonu için gerekirse tekrar eklenebilir.
  // loadProductDetails(slug: string): void { ... }


  isProductActive(product: Product): boolean {
    return isProductActive(product);
  }

  isProductApproved(product: Product): boolean {
    return isProductApproved(product);
  }

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
      error: (err: any) => {
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
        },
        error: (err: any) => {
          this.isTogglingWishlist.set(false);
          console.error('Error removing from wishlist in ProductDetailComponent:', err);
        }
      });
    } else {
      this.wishlistService.addProductToWishlist(currentProduct.id).subscribe({
        next: () => { // Product | null parametresi kaldırıldı, backend void dönebilir veya sadece başarı mesajı
          this.isWishlisted.set(true);
          this.isTogglingWishlist.set(false);
        },
        error: (err: any) => {
          this.isTogglingWishlist.set(false);
          console.error('Error adding to wishlist in ProductDetailComponent:', err);
        }
      });
    }
  }

  selectImage(imageUrl: string): void {
    this.currentDisplayImageUrl.set(imageUrl);
  }

  async buildBreadcrumbs(product: Product): Promise<void> {
    const productName = product?.name || 'Ürün Detayı'; // Ürün adı için fallback
    const finalCrumbs: Array<{ name: string; slug: string | null; isLink: boolean }> = [];
    const categoryHierarchy: Array<{ name: string; slug: string | null; isLink: boolean }> = [];

    if (product && product.categoryId) {
      let categoryIdToFetch: number | undefined = product.categoryId;
      while (categoryIdToFetch) {
        try {
          // Ensure categoryService and getCategoryById are correctly implemented and return expected CategoryResponseDto
          const category: CategoryResponseDto | null = await this.categoryService.getCategoryById(categoryIdToFetch).toPromise() || null;
          if (category && category.name && category.slug) {
            // Add to the beginning of the array to maintain parent -> child order
            categoryHierarchy.unshift({ name: category.name, slug: `/category/${category.slug}`, isLink: true });
            categoryIdToFetch = category.parentId ?? undefined; // Move to the parent category, ensuring undefined if parentId is null
          } else {
            // If category data is incomplete or not found, stop ascending this path
            categoryIdToFetch = undefined;
          }
        } catch (error) {
          console.error('Error fetching parent category for breadcrumbs:', error);
          categoryIdToFetch = undefined; // Stop on error
        }
      }
    }

    if (categoryHierarchy.length > 0) {
      finalCrumbs.push(...categoryHierarchy);
    } else {
      // If no category hierarchy could be built (e.g., product has no category, or fetching failed),
      // provide a default starting point. The screenshot implies "Products" is the start.
      // Assuming '/products' is the slug for a general products listing page.
      finalCrumbs.push({ name: 'Tüm Ürünler', slug: '/products', isLink: true });
    }

    // Add current product name (not a link) at the very end
    finalCrumbs.push({ name: productName, slug: null, isLink: false });

    this.breadcrumbs.set(finalCrumbs);
  }
}
