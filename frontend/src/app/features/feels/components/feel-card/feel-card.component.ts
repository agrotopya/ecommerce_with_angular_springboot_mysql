import { Component, Input, inject, signal, OnInit, ViewChild, ElementRef, effect, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FeelResponseDto } from '@shared/models/feel.model';
import { AuthService } from '@core/services/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { WishlistService } from '@features/wishlist/services/wishlist.service';
import { ReviewService } from '@features/reviews/services/review.service';
import { ReviewResponseDto } from '@shared/models/review.model';
import { Page } from '@shared/models/page.model';
import { ProductService } from '@features/products/product.service';
import { Product } from '@shared/models/product.model';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-feel-card',
  standalone: true,
  imports: [CommonModule, RouterLink, NgOptimizedImage],
  templateUrl: './feel-card.component.html',
  styleUrls: ['./feel-card.component.scss'],
})
export class FeelCardComponent implements OnInit, OnChanges {
  @Input({ required: true }) feel!: FeelResponseDto;
  @Input() isActive: boolean = false;

  @ViewChild('videoPlayer') videoPlayerRef!: ElementRef<HTMLVideoElement>;

  private router = inject(Router);
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);
  private wishlistService = inject(WishlistService);
  private reviewService = inject(ReviewService);
  private productService = inject(ProductService);

  public env = environment;

  isInWishlist = signal(false);
  isTogglingWishlist = signal(false);
  productReviews = signal<ReviewResponseDto[]>([]);
  isLoadingReviews = signal(false);
  showComments = signal(false);
  isPlaying = signal(false);
  showPlayButton = signal(false);

  constructor() {
    effect(() => {
      if (this.videoPlayerRef && this.videoPlayerRef.nativeElement) {
        if (this.isActive) {
          this.playVideo();
        } else {
          this.pauseVideo();
        }
      }
    });
  }

  ngOnInit(): void {
    if (this.feel.productId && this.authService.isAuthenticated()) {
      this.checkIfInWishlist();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isActive']) {
      console.log(`FeelCard ${this.feel.id} isActive changed to: ${this.isActive}`);
      if (this.videoPlayerRef && this.videoPlayerRef.nativeElement) {
        if (this.isActive) {
          this.playVideo();
        } else {
          this.pauseVideo();
        }
      }
    }
  }

  private playVideo(): void {
    if (this.videoPlayerRef && this.videoPlayerRef.nativeElement) {
      this.videoPlayerRef.nativeElement.play().then(() => {
        this.isPlaying.set(true);
        console.log(`FeelCard ${this.feel.id}: Video playing`);
      }).catch(error => {
        console.warn(`FeelCard ${this.feel.id}: Video play failed`, error);
        this.isPlaying.set(false);
      });
    }
  }

  private pauseVideo(): void {
    if (this.videoPlayerRef && this.videoPlayerRef.nativeElement) {
      this.videoPlayerRef.nativeElement.pause();
      this.isPlaying.set(false);
      console.log(`FeelCard ${this.feel.id}: Video paused`);
    }
  }

  togglePlayPause(): void {
    if (this.isPlaying()) {
      this.pauseVideo();
    } else {
      this.playVideo();
    }
  }

  checkIfInWishlist(): void {
    if (!this.feel.productId) return;
    this.wishlistService.checkAndStoreProductWishlistStatus(this.feel.productId).subscribe({
      next: (isInWishlist: boolean) => this.isInWishlist.set(isInWishlist),
      error: (err: HttpErrorResponse) => {
        console.error('Error checking wishlist status via subscription', err);
        this.isInWishlist.set(false);
      }
    });
  }

  toggleWishlist(): void {
    if (!this.authService.isAuthenticated()) {
      this.notificationService.showInfo('Lütfen favorilere eklemek için giriş yapın.');
      this.router.navigate(['/auth/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    if (!this.feel.productId) {
      this.notificationService.showError('Bu feel ile ilişkili bir ürün bulunamadı.');
      return;
    }

    this.isTogglingWishlist.set(true);
    const operation$: Observable<Product | void | null> = this.isInWishlist()
      ? this.wishlistService.removeProductFromWishlist(this.feel.productId)
      : this.wishlistService.addProductToWishlist(this.feel.productId);

    operation$.subscribe({
      next: () => {
        this.isInWishlist.update((current) => !current);
        this.notificationService.showSuccess(
          this.isInWishlist() ? 'Ürün favorilere eklendi!' : 'Ürün favorilerden çıkarıldı.'
        );
        this.isTogglingWishlist.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.notificationService.showError('Favori işlemi sırasında bir hata oluştu.');
        console.error('Error toggling wishlist', err);
        this.isTogglingWishlist.set(false);
      },
    });
  }

  loadProductReviews(): void {
    if (!this.feel.productId) return;
    this.isLoadingReviews.set(true);
    this.reviewService.getProductReviews(this.feel.productId, 0, 5, true).subscribe({
      next: (page: Page<ReviewResponseDto>) => {
        this.productReviews.set(page.content);
        this.isLoadingReviews.set(false);
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error loading product reviews', err);
        this.isLoadingReviews.set(false);
      }
    });
  }

  navigateToProduct(): void {
    console.log('navigateToProduct called. Feel data:', this.feel);
    if (this.feel.productSlug) {
      console.log('Navigating to product slug:', this.feel.productSlug);
      this.router.navigate(['/products', this.feel.productSlug]);
    } else if (this.feel.productId) {
      console.log('Product slug not found, fetching product by ID:', this.feel.productId);
      this.productService.getPublicProductById(this.feel.productId).subscribe({
        next: (product) => {
          if (product && product.slug) {
            console.log('Product found, navigating to slug:', product.slug);
            this.router.navigate(['/products', product.slug]);
          } else {
            console.error('Product or product slug not found after fetching by ID.');
            this.notificationService.showError('Ürün detayı bulunamadı.');
          }
        },
        error: (err: HttpErrorResponse) => {
          this.notificationService.showError('Ürün detayı yüklenirken hata oluştu.');
          console.error('Error fetching product details for feel navigation', err);
        }
      });
    } else {
      console.error('No productSlug or productId found on feel object.');
      this.notificationService.showError('Bu feel ile ilişkili bir ürün bulunamadı.');
    }
  }

  navigateToSellerProfile(): void {
    // Bu metodun doğru olduğundan emin olalım.
    if (this.feel.sellerId) {
      console.log('[Corrected Log] Navigating to seller profile for seller ID:', this.feel.sellerId);
      this.router.navigate(['/sellers', this.feel.sellerId]);
    } else {
      console.warn('[Corrected Log] Cannot navigate to seller profile: sellerId is missing from feel data for feel ID:', this.feel.id);
      this.notificationService.showError('Satıcı profili bilgisi eksik.');
    }
  }

  toggleComments(): void {
    this.showComments.update(current => !current);
    if (this.showComments() && this.productReviews().length === 0 && this.feel.productId) {
      this.loadProductReviews();
    }
  }

  shareFeel(): void {
    if (navigator.share) {
      navigator.share({
        title: this.feel.title,
        text: `Fibiyo'da bu harika feel'e göz at: ${this.feel.title}`,
        url: window.location.origin + this.router.createUrlTree(['/feels', this.feel.id]).toString(),
      })
      .then(() => console.log('Successful share'))
      .catch((error) => console.log('Error sharing', error));
    } else {
      const feelUrl = window.location.origin + this.router.createUrlTree(['/feels', this.feel.id]).toString();
      navigator.clipboard.writeText(feelUrl).then(() => {
        this.notificationService.showSuccess('Feel linki panoya kopyalandı!');
      }).catch(err => {
        this.notificationService.showError('Link kopyalanamadı.');
        console.error('Could not copy text: ', err);
      });
    }
  }
}
