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
    const crumbs: Array<{ name: string; slug: string | null; isLink: boolean }> = [];
    if (!product || !product.categoryId) {
      this.breadcrumbs.set([]);
      return;
    }

    // Add current product name (not a link)
    crumbs.push({ name: product.name, slug: null, isLink: false });

    // Add current product's category (as a link)
    crumbs.push({ name: product.categoryName, slug: `/category/${product.categorySlug}`, isLink: true });

    let currentCategoryId: number | null | undefined = product.categoryId;
    let categoryDetails: CategoryResponseDto | null = null;

    // Fetch parent categories iteratively
    // To avoid too many sequential calls, we can limit the depth or fetch all parents if API supports it
    // For now, let's assume a reasonable depth and fetch one by one.
    // We need to get the parentId from the product's category first.
    try {
      const currentCategory = await this.categoryService.getCategoryById(product.categoryId).toPromise() ?? null;

      if (currentCategory && currentCategory.parentId) {
        // Eğer bir üst kategorisi varsa, onu da çekelim
        const parentCategory = await this.categoryService.getCategoryById(currentCategory.parentId).toPromise() ?? null;
        if (parentCategory) {
          crumbs.push({ name: parentCategory.name, slug: `/category/${parentCategory.slug}`, isLink: true });
        }
      }
      // Ürünün kendi kategorisi zaten product.categoryName ve product.categorySlug ile eklenmişti.
      // Bu yüzden burada tekrar eklemeye gerek yok, yukarıdaki push yeterli.
      // Eğer product nesnesinde parentId ve parentSlug gibi bilgiler olsaydı, daha az API çağrısı yapabilirdik.
      // Mevcut yapıda, ürünün kategorisinin parentId'sini almak için ilk bir çağrı, sonra parent'ı almak için ikinci bir çağrı yapılıyor.

    } catch (error) {
      console.error('Error building breadcrumbs:', error);
    }

    // Add "Products" as the root link
    crumbs.push({ name: 'Products', slug: '/products', isLink: true });
    this.breadcrumbs.set(crumbs.reverse()); // Products > Parent (varsa) > Current Category > Product Name
  }
}
