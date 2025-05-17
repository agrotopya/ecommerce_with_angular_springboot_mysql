import { Component, Input, inject, signal, OnInit } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs'; // Observable importu buraya taşındı
import { FeelResponseDto } from '@shared/models/feel.model';
import { AuthService } from '@core/services/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { WishlistService } from '@features/wishlist/services/wishlist.service'; // Doğru import yolu
import { ReviewService } from '@features/reviews/services/review.service';
import { ReviewResponseDto } from '@shared/models/review.model';
import { Page } from '@shared/models/page.model';
import { ProductService } from '@features/products/product.service';
import { Product } from '@shared/models/product.model'; // Product modeli import edildi

@Component({
  selector: 'app-feel-card',
  standalone: true,
  imports: [CommonModule, RouterLink, NgOptimizedImage],
  templateUrl: './feel-card.component.html',
  styleUrls: ['./feel-card.component.scss'],
})
export class FeelCardComponent implements OnInit {
  @Input({ required: true }) feel!: FeelResponseDto;

  private router = inject(Router);
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);
  private wishlistService = inject(WishlistService);
  private reviewService = inject(ReviewService);
  private productService = inject(ProductService);


  isInWishlist = signal(false);
  isTogglingWishlist = signal(false);
  productReviews = signal<ReviewResponseDto[]>([]);
  isLoadingReviews = signal(false);
  showComments = signal(false); // Yorumları gösterme/gizleme durumu için sinyal

  // Feel'in kendi like/unlike mekanizması backend'de olduğu için onu koruyoruz.
  // Kalp ikonu artık ürünün favorilere eklenmesiyle ilgili olacak.
  // isFeelLiked = signal(this.feel?.isLikedByCurrentUser || false);
  // isLikingFeel = signal(false);

  ngOnInit(): void {
    if (this.feel.productId && this.authService.isAuthenticated()) {
      this.checkIfInWishlist();
    }
    if (this.feel.productId) {
      this.loadProductReviews();
    }
    // this.isFeelLiked.set(this.feel?.isLikedByCurrentUser || false);
  }

  checkIfInWishlist(): void {
    if (!this.feel.productId) return;
    // isProductInWishlist yerine checkAndStoreProductWishlistStatus kullanılıyor.
    // Bu metod zaten BehaviorSubject'i güncelliyor, bu yüzden subscribe içindeki set anlamsız olabilir
    // ya da direkt BehaviorSubject'ten gelen değeri kullanabiliriz.
    // Şimdilik component içindeki signal'i güncellemek için subscribe oluyoruz.
    this.wishlistService.checkAndStoreProductWishlistStatus(this.feel.productId).subscribe({
      next: (isInWishlist: boolean) => this.isInWishlist.set(isInWishlist),
      error: (err: HttpErrorResponse) => {
        console.error('Error checking wishlist status via subscription', err);
        this.isInWishlist.set(false); // Hata durumunda false ayarla
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
      next: (productOrNull: Product | void | null) => {
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
    // ReviewService'de getProductReviews metodu olduğunu varsayıyoruz.
    // Bu metod Page<ReviewResponseDto> döndürüyorsa, .content kısmını almalıyız.
    // Şimdilik sadece onaylanmış yorumları alacak şekilde (approvedOnly: true) varsayalım.
    // getProductReviews metodu 4 argüman alıyor: productId, page, size, approvedOnly. Sort içeride handle ediliyor.
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
    if (this.feel.productSlug) {
      this.router.navigate(['/products/slug', this.feel.productSlug]);
    } else if (this.feel.productId) {
      // Fallback eğer slug yoksa ama ID varsa (idealde slug her zaman olmalı)
      this.productService.getPublicProductById(this.feel.productId).subscribe({
        next: (product) => { // product: Product tipinde olmalı
          if (product && product.slug) {
            this.router.navigate(['/products/slug', product.slug]);
          } else {
            this.notificationService.showError('Ürün detayı bulunamadı.');
          }
        },
        error: (err: HttpErrorResponse) => { // err: HttpErrorResponse tipinde olmalı
          this.notificationService.showError('Ürün detayı yüklenirken hata oluştu.');
          console.error('Error fetching product details for feel navigation', err);
        }
      });
    } else {
      this.notificationService.showError('Bu feel ile ilişkili bir ürün bulunamadı.');
    }
  }

  // Feel'in kendi like/unlike mekanizması için (eğer backend'de varsa ve kullanılacaksa)
  // onFeelLikeToggle(): void {
  //   if (!this.authService.isAuthenticated()) {
  //     this.notificationService.showInfo('Beğenmek için lütfen giriş yapın.');
  //     this.router.navigate(['/auth/login'], { queryParams: { returnUrl: this.router.url } });
  //     return;
  //   }
  //   this.isLikingFeel.set(true);
  //   const operation$ = this.isFeelLiked()
  //     ? this.feelService.unlikeFeel(this.feel.id) // feelService inject edilmeli
  //     : this.feelService.likeFeel(this.feel.id); // feelService inject edilmeli

  //   operation$.subscribe({
  //     next: () => {
  //       this.isFeelLiked.update(liked => !liked);
  //       this.feel.likeCount = this.isFeelLiked() ? (this.feel.likeCount + 1) : (this.feel.likeCount - 1);
  //       this.isLikingFeel.set(false);
  //     },
  //     error: (err) => {
  //       this.notificationService.showError('Beğeni işlemi sırasında bir hata oluştu.');
  //       console.error('Error toggling feel like', err);
  //       this.isLikingFeel.set(false);
  //     }
  //   });
  // }

  // Kullanıcı adının üzerine tıklandığında satıcı profiline gitmek için (eğer varsa)
  navigateToSellerProfile(): void {
    if (this.feel.sellerUsername) {
      // Satıcı profil rotası '/sellers/:username' veya benzeri bir yapıda olmalı
      // Örnek: this.router.navigate(['/sellers', this.feel.sellerUsername]);
      // Şimdilik sadece log basıyoruz, çünkü seller profil sayfası henüz tanımlı değil.
      console.log('Navigate to seller profile:', this.feel.sellerUsername);
      this.notificationService.showInfo(`Satıcı profiline git: ${this.feel.sellerUsername} (henüz implemente edilmedi)`);
    }
  }

  toggleComments(): void {
    this.showComments.update(current => !current);
    if (this.showComments() && this.productReviews().length === 0 && this.feel.productId) {
      this.loadProductReviews(); // Yorumlar daha önce yüklenmediyse ve bölüm açılıyorsa yükle
    }
  }

  shareFeel(): void {
    // Web Share API kullanılabilir veya link kopyalama işlevi eklenebilir
    if (navigator.share) {
      navigator.share({
        title: this.feel.title,
        text: `Fibiyo'da bu harika feel'e göz at: ${this.feel.title}`,
        url: window.location.href, // Veya feel'in kendi direkt linki varsa o
      })
      .then(() => console.log('Successful share'))
      .catch((error) => console.log('Error sharing', error));
    } else {
      // Fallback: linki panoya kopyala
      const feelUrl = window.location.origin + this.router.createUrlTree(['/feels', this.feel.id]).toString(); // Feel'in direkt linki
      navigator.clipboard.writeText(feelUrl).then(() => {
        this.notificationService.showSuccess('Feel linki panoya kopyalandı!');
      }).catch(err => {
        this.notificationService.showError('Link kopyalanamadı.');
        console.error('Could not copy text: ', err);
      });
    }
  }
}
