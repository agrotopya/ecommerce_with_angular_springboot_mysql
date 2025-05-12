import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router'; // Router ve RouterLink eklendi
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { ReviewService } from '../../services/review.service';
import { ReviewResponseDto, ReviewRequest } from '../../../../shared/models/review.model';
import { Page } from '../../../../shared/models/page.model';
import { AuthService } from '../../../../core/services/auth.service';
import { NotificationService } from '../../../../core/services/notification.service';
// ReviewItemComponent ve ReviewFormComponent ileride import edilecek

@Component({
  selector: 'app-product-reviews',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink], // RouterLink eklendi // Alt bileşenler eklenecek
  templateUrl: './product-reviews.component.html',
  styleUrls: ['./product-reviews.component.scss']
})
export class ProductReviewsComponent implements OnInit {
  @Input({ required: true }) productId!: number;

  private reviewService = inject(ReviewService);
  public authService = inject(AuthService); // public yapıldı
  private notificationService = inject(NotificationService);
  private fb = inject(FormBuilder);
  public router = inject(Router); // Router inject edildi ve public yapıldı

  reviewsPage = signal<Page<ReviewResponseDto> | null>(null);
  isLoading = signal(false);
  errorLoadingReviews = signal<string | null>(null);
  userHasReviewed = signal(false); // Kullanıcının yorum yapıp yapmadığını tutacak sinyal
  isSubmittingReview = signal(false);
  errorSubmittingReview = signal<string | null>(null);

  // Yorum ekleme formu
  reviewForm: FormGroup;

  // Sayfalama
  currentPage = 0;
  pageSize = 5;

  constructor() {
    this.reviewForm = this.fb.group({
      rating: [null, [Validators.required, Validators.min(1), Validators.max(5)]],
      comment: ['', [Validators.maxLength(5000)]]
    });
  }

  ngOnInit(): void {
    if (this.productId) {
      this.loadReviews();
    }
  }

  loadReviews(page: number = 0): void {
    this.isLoading.set(true);
    this.errorLoadingReviews.set(null);
    this.userHasReviewed.set(false); // Her yüklemede sıfırla
    this.currentPage = page;

    this.reviewService.getProductReviews(this.productId, this.currentPage, this.pageSize).subscribe({
      next: (data) => {
        this.reviewsPage.set(data);
        this.isLoading.set(false);
        this.checkIfUserHasReviewed(data.content);
      },
      error: (err) => {
        console.error('Error loading reviews:', err);
        this.errorLoadingReviews.set('Failed to load reviews.');
        this.isLoading.set(false);
      }
    });
  }

  get canAddReview(): boolean {
    // Kullanıcı giriş yapmış mı VE daha önce yorum yapmamış mı?
    return this.authService.isAuthenticated() && !this.userHasReviewed();
  }

  private checkIfUserHasReviewed(reviews: ReviewResponseDto[]): void {
    const currentUser = this.authService.currentUser(); // currentUserSignal -> currentUser
    if (!currentUser || !reviews) {
      this.userHasReviewed.set(false);
      return;
    }
    const existingReview = reviews.find(review => review.customerId === currentUser.id);
    this.userHasReviewed.set(!!existingReview);
  }

  onSubmitReview(): void {
    if (!this.canAddReview) { // Bu kontrol zaten formu gizleyeceği için teorik olarak gereksiz ama güvenlik için kalabilir
      this.notificationService.showError('You cannot submit a review at this time.');
      return;
    }
    if (this.reviewForm.invalid) {
      this.notificationService.showError('Please provide a valid rating (1-5).');
      return;
    }

    this.isSubmittingReview.set(true);
    this.errorSubmittingReview.set(null);

    const reviewData: ReviewRequest = {
      rating: this.reviewForm.value.rating,
      comment: this.reviewForm.value.comment || undefined
    };

    this.reviewService.addReview(this.productId, reviewData).subscribe({
      next: (newReview) => {
        this.notificationService.showSuccess('Review submitted successfully! It will be visible after approval.');
        this.reviewForm.reset();
        this.isSubmittingReview.set(false);
        // Yorum listesini yenilemek yerine yeni yorumu başa ekleyebiliriz (onay beklediği için hemen görünmeyebilir)
        // Veya ilk sayfayı tekrar yükleyebiliriz. Şimdilik basit tutalım.
        // this.loadReviews(); // Veya yeni yorumu listeye ekle (onay durumuyla)
      },
      error: (err) => {
        console.error('Error submitting review:', err);
        const message = err.error?.message || 'Failed to submit review. You might need to purchase the product first or you have already reviewed it.';
        this.notificationService.showError(message);
        this.errorSubmittingReview.set(message);
        this.isSubmittingReview.set(false);
      }
    });
  }

  // Sayfalama için
  onPageChange(page: number): void {
    if (page >= 0 && page < (this.reviewsPage()?.totalPages ?? 0)) {
      this.loadReviews(page);
    }
  }

  get pageNumbers(): number[] {
    const totalPages = this.reviewsPage()?.totalPages ?? 0;
    return Array(totalPages).fill(0).map((x, i) => i);
  }
}
