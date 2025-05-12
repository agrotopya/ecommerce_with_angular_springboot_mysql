import { Component, inject, signal, OnInit, OnDestroy, ChangeDetectionStrategy, computed, effect } from '@angular/core';
import { CommonModule, Location, NgOptimizedImage } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Observable, Subscription, switchMap, catchError, EMPTY, tap, finalize, BehaviorSubject } from 'rxjs';

import { FeelService } from '../../services/feel.service';
import { FeelResponseDto } from '../../../../shared/models/feel.model';
import { Product } from '../../../../shared/models/product.model'; // Ürün modeli
import { ReviewResponseDto } from '../../../../shared/models/review.model'; // Yorum modeli
import { ProductService } from '../../../products/product.service'; // Ürün servisi
import { ReviewService } from '../../../reviews/services/review.service'; // Yorum servisi
import { WishlistService } from '../../../wishlist/services/wishlist.service'; // Ürün beğenme için kalabilir
// import { WishlistRequest, WishlistCheckResponse } from '@shared/models/wishlist.model'; // Feel beğenme için gerekmeyebilir
import { AuthService } from '../../../../core/services/auth.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { ApiResponse } from '../../../../shared/models/api-response.model'; // ApiResponse import edildi
import { ProductReviewsComponent } from '../../../reviews/components/product-reviews/product-reviews.component'; // Yorumlar için doğru yol import edildi

@Component({
  selector: 'app-feel-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, NgOptimizedImage, ProductReviewsComponent], // ProductReviewsComponent imports'a eklendi
  templateUrl: './feel-detail.component.html',
  styleUrls: ['./feel-detail.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeelDetailComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private location = inject(Location);
  private sanitizer = inject(DomSanitizer);
  private feelService = inject(FeelService);
  private productService = inject(ProductService);
  private reviewService = inject(ReviewService);
  private wishlistService = inject(WishlistService);
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);

  private subscriptions = new Subscription();

  feel = signal<FeelResponseDto | null>(null);
  product = signal<Product | null>(null);
  productReviews = signal<ReviewResponseDto[]>([]); // Yorumlar için aktif edildi
  isLoadingFeel = signal(true);
  isLoadingProduct = signal(false);
  isLoadingReviews = signal(false); // Yorumlar için aktif edildi
  isFeelLiked = signal(false); // Bu feel'in kullanıcı tarafından beğenilip beğenilmediği
  isLikingFeel = signal(false); // Feel beğenme işlemi sırasında loading state
  // Ürün için istek listesi durumu ayrı yönetilebilir, şimdilik sadece feel beğenmeye odaklanalım.
  // isProductWishlisted = signal(false);
  // isTogglingProductWishlist = signal(false);


  // Güvenli video URL'i için
  safeVideoUrl = computed(() => {
    const currentFeel = this.feel();
    if (currentFeel?.videoUrl) {
      // YouTube, Vimeo vb. embed URL'lerini veya direkt video URL'lerini güvenli hale getir
      // Örnek: YouTube URL'i ise embed formatına çevir
      if (currentFeel.videoUrl.includes('youtube.com/watch?v=')) {
        const videoId = currentFeel.videoUrl.split('v=')[1].split('&')[0];
        return this.sanitizer.bypassSecurityTrustResourceUrl(`https://www.youtube.com/embed/${videoId}?autoplay=1&mute=1`);
      }
      // Direkt video dosyası ise
      return this.sanitizer.bypassSecurityTrustResourceUrl(currentFeel.videoUrl);
    }
    return null;
  });

  constructor() {
    // Route parametresi değiştiğinde feel'i yeniden yükle
    // Bu, aynı component üzerinde farklı feel'lere geçişte (örneğin kaydırma ile) önemlidir.
    effect(() => {
      const feelId = this.route.snapshot.paramMap.get('feelId');
      if (feelId) {
        this.loadFeelDetails(feelId);
      }
    });
  }

  ngOnInit(): void {
    // İlk yükleme için effect zaten çalışacak, ancak isterseniz burada da çağırabilirsiniz.
    // const feelId = this.route.snapshot.paramMap.get('feelId');
    // if (feelId) {
    //   this.loadFeelDetails(feelId);
    // }
  }

  loadFeelDetails(feelId: string): void {
    this.isLoadingFeel.set(true);
    this.subscriptions.add(
      this.feelService.getFeelById(+feelId).pipe( // Convert feelId to number
        tap(currentFeel => {
          this.feel.set(currentFeel);
          this.feel.set(currentFeel);
          this.isFeelLiked.set(currentFeel.isLikedByCurrentUser || false); // Feel'in beğenilme durumunu ayarla

          if (currentFeel.productId !== undefined) { // undefined kontrolü
            this.loadProductDetails(currentFeel.productId);
            this.loadProductReviews(currentFeel.productId);
            // Ürünün istek listesi durumu ayrı kontrol edilebilir, şimdilik yoruma alıyoruz.
            // this.checkIfProductWishlisted(currentFeel.productId);
          } else {
            // productId tanımsızsa, ürünle ilgili işlemleri atla
            this.product.set(null);
            this.productReviews.set([]);
            // this.isProductWishlisted.set(false);
            console.warn('Feel does not have an associated product.');
          }
        }),
        catchError(err => {
          this.notificationService.showError('Feel detayı yüklenirken bir hata oluştu.');
          this.router.navigate(['/feels']);
          return EMPTY;
        }),
        finalize(() => this.isLoadingFeel.set(false))
      ).subscribe()
    );
  }

  loadProductDetails(productId: number): void {
    this.isLoadingProduct.set(true);
    this.subscriptions.add(
      this.productService.getPublicProductById(productId).pipe(
        tap(productData => this.product.set(productData)),
        catchError(err => {
          this.notificationService.showError('İlişkili ürün bilgileri yüklenirken bir hata oluştu.');
          return EMPTY;
        }),
        finalize(() => this.isLoadingProduct.set(false))
      ).subscribe()
    );
  }

  loadProductReviews(productId: number): void {
    this.isLoadingReviews.set(true);
    this.subscriptions.add(
      this.reviewService.getProductReviews(productId, 0, 3).pipe(
        tap(reviewsPage => this.productReviews.set(reviewsPage.content)),
        catchError(err => {
          this.notificationService.showError('Ürün yorumları yüklenirken bir hata oluştu.');
          return EMPTY;
        }),
        finalize(() => this.isLoadingReviews.set(false))
      ).subscribe()
    );
  }

  // checkIfProductWishlisted(productId: number): void {
  //   if (!this.authService.isAuthenticated()) {
  //     this.isProductWishlisted.set(false);
  //     return;
  //   }
  //   this.subscriptions.add(
  //     this.wishlistService.checkAndStoreProductWishlistStatus(productId).subscribe(
  //       (isInWishlist: boolean) => this.isProductWishlisted.set(isInWishlist)
  //     )
  //   );
  // }

  toggleFeelLike(): void {
    if (!this.authService.isAuthenticated()) {
      this.notificationService.showInfo('Beğenmek için giriş yapmalısınız.');
      this.router.navigate(['/auth/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }

    const currentFeel = this.feel();
    if (!currentFeel) {
      this.notificationService.showError('Feel bulunamadı.');
      return;
    }

    this.isLikingFeel.set(true);
    let action$: Observable<ApiResponse<void>>;

    if (this.isFeelLiked()) {
      action$ = this.feelService.unlikeFeel(currentFeel.id);
    } else {
      action$ = this.feelService.likeFeel(currentFeel.id);
    }

    this.subscriptions.add(
      action$.pipe(
        finalize(() => this.isLikingFeel.set(false))
      ).subscribe({
        next: () => {
          this.isFeelLiked.update(liked => !liked);
          this.feel.update(f => {
            if (!f) return null;
            // Backend'den güncel likeCount gelmiyorsa, burada manuel artır/azalt.
            // Backend zaten likeCount'u güncelleyip FeelResponseDto'da dönüyorsa bu satırlara gerek yok,
            // loadFeelDetails tekrar çağrılabilir veya gelen yanıttan alınabilir.
            // Şimdilik client-side güncelleme yapıyoruz.
            const newLikeCount = this.isFeelLiked() ? f.likeCount + 1 : (f.likeCount > 0 ? f.likeCount - 1 : 0);
            return { ...f, likeCount: newLikeCount, isLikedByCurrentUser: this.isFeelLiked() };
          });
          // this.notificationService.showSuccess(this.isFeelLiked() ? 'Feel beğenildi!' : 'Feel beğenisi geri alındı.'); // Servis içinde zaten var
        },
        error: (err) => {
          // Hata mesajı zaten feelService içinde gösteriliyor.
          console.error('Toggle feel like error:', err);
        }
      })
    );
  }

  // Ürünü istek listesine ekleme/çıkarma (ayrı bir butonla yönetilecekse)
  // toggleProductWishlist(): void { ... }


  goBack(): void {
    this.location.back();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }
}
