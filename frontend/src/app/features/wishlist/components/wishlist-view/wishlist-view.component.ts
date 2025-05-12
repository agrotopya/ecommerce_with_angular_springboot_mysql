import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, NgFor, NgIf } from '@angular/common';
import { Router, RouterLink } from '@angular/router'; // Router eklendi
import { WishlistService } from '../../services/wishlist.service';
import { Product } from '@shared/models/product.model';
import { CartService } from '@features/cart/cart.service'; // CartService import eklendi
import { AuthService } from '@core/services/auth.service'; // AuthService import eklendi
import { Page } from '@shared/models/page.model';
import { NotificationService } from '@core/services/notification.service';
import { ProductCardComponent } from '../../../products/product-card/product-card.component'; // ProductCardComponent import edildi
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component'; // Aktif edildi
import { PaginatorComponent } from '@shared/components/paginator/paginator.component'; // Aktif edildi

@Component({
  selector: 'app-wishlist-view',
  standalone: true,
  imports: [
    CommonModule,
    NgFor,
    NgIf,
    RouterLink,
    ProductCardComponent, // ProductCardComponent imports'a eklendi
    LoadingSpinnerComponent, // Aktif edildi
    PaginatorComponent // Aktif edildi
  ],
  templateUrl: './wishlist-view.component.html',
  styleUrls: ['./wishlist-view.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WishlistViewComponent implements OnInit {
  private wishlistService = inject(WishlistService);
  private notificationService = inject(NotificationService);
  private cartService = inject(CartService); // CartService eklendi
  private router = inject(Router); // Router eklendi
  private authService = inject(AuthService); // AuthService eklendi


  products = signal<Product[]>([]);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  pageSize = signal(12); // Sayfa başına ürün sayısı

  ngOnInit(): void {
    this.loadWishlist();
  }

  loadWishlist(page: number = 0): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.currentPage.set(page);

    this.wishlistService.getMyWishlist(page, this.pageSize()).subscribe({
      next: (response: Page<Product> | null) => {
        if (response) {
          this.products.set(response.content);
          this.totalPages.set(response.totalPages);
          this.totalElements.set(response.totalElements);
        } else {
          this.products.set([]);
          this.totalPages.set(0);
          this.totalElements.set(0);
        }
        this.isLoading.set(false);
      },
      error: (err: any) => { // err tip eklendi
        console.error('Error loading wishlist items:', err);
        this.errorMessage.set('Failed to load wishlist. Please try again.');
        this.isLoading.set(false);
      },
    });
  }

  onRemoveFromWishlist(productId: number): void {
    this.wishlistService.removeProductFromWishlist(productId).subscribe({
      next: () => {
        // Ürün başarıyla kaldırıldıktan sonra listeyi yenile
        // Mevcut sayfada kalmak için, eğer son eleman silindiyse ve sayfa boşaldıysa bir önceki sayfaya git
        if (this.products().length === 1 && this.currentPage() > 0) {
          this.loadWishlist(this.currentPage() - 1);
        } else {
          this.loadWishlist(this.currentPage());
        }
      },
      error: (err: any) => { // err tip eklendi
        // Hata zaten serviste notificationService ile gösteriliyor.
        console.error('Error removing product from wishlist in component:', err);
      }
    });
  }

  onPageChange(page: number): void {
    this.loadWishlist(page);
  }

  // Sepete ekleme işlemi için metod
  onAddToCart(product: Product): void {
    if (!this.authService.isAuthenticated()) {
      this.notificationService.showInfo('Please login to add items to your cart.');
      this.router.navigate(['/auth/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    if (product && product.id !== undefined) {
      this.cartService.addItem({ productId: product.id, quantity: 1 }).subscribe({
        next: () => {
          this.notificationService.showSuccess(`${product.name} added to cart!`);
        },
        error: (err: any) => { // err tip eklendi
          this.notificationService.showError('Failed to add product to cart.');
          console.error('Error adding product to cart from wishlist:', err);
        }
      });
    }
  }
}
