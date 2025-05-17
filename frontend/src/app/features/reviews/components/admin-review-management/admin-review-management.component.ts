import { Component, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ReviewService } from '../../services/review.service';
import { ReviewResponseDto } from '../../../../shared/models/review.model';
import { Page } from '../../../../shared/models/page.model';
import { NotificationService } from '../../../../core/services/notification.service';
import { RouterModule } from '@angular/router'; // Ürün linkleri için
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago.pipe'; // TimeAgoPipe import edildi

@Component({
  selector: 'app-admin-review-management',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, DatePipe, TimeAgoPipe], // TimeAgoPipe eklendi
  templateUrl: './admin-review-management.component.html',
  styleUrls: ['./admin-review-management.component.scss'] // styleUrl -> styleUrls
})
export class AdminReviewManagementComponent implements OnInit {
  private reviewService = inject(ReviewService);
  private notificationService = inject(NotificationService);

  reviewsResponse: WritableSignal<Page<ReviewResponseDto> | null> = signal(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  // Filtreleme ve Sayfalama
  currentPage = signal(0);
  pageSize = signal(10);
  currentFilterStatus = signal<boolean | undefined>(undefined); // true: approved, false: not approved, undefined: all

  filterOptions = [
    { label: 'All Reviews', value: undefined },
    { label: 'Approved Reviews', value: true },
    { label: 'Pending Approval', value: false }
  ];
  selectedFilter: string = ''; // ngModel için, string olarak alıp boolean'a çevireceğiz

  ngOnInit(): void {
    this.loadReviews();
  }

  loadReviews(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    let params = new HttpParams()
      .set('page', this.currentPage().toString())
      .set('size', this.pageSize().toString())
      .set('sort', 'createdAt,desc'); // En yeni yorumlar üstte

    if (this.currentFilterStatus() !== undefined) {
      params = params.set('isApproved', this.currentFilterStatus()!.toString());
    }

    this.reviewService.getAllReviewsAdmin(params).subscribe({
      next: (response) => {
        console.log('AdminReviewManagement: Raw reviews response from service:', response);
        if (response && response.content) {
          response.content.forEach(review => console.log(`AdminReviewManagement: Review ID ${review.id}, approved: ${review.approved}`)); // isApproved -> approved
        }
        this.reviewsResponse.set(response);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Failed to load reviews.');
        this.notificationService.showError(this.errorMessage()!);
        this.isLoading.set(false);
        console.error('Error loading reviews for admin:', err);
      }
    });
  }

  onPageChange(page: number): void {
    if (page >= 0 && page < (this.reviewsResponse()?.totalPages ?? 0)) {
      this.currentPage.set(page);
      this.loadReviews();
    }
  }

  onFilterChange(): void {
    this.currentPage.set(0);
    if (this.selectedFilter === 'true') {
      this.currentFilterStatus.set(true);
    } else if (this.selectedFilter === 'false') {
      this.currentFilterStatus.set(false);
    } else {
      this.currentFilterStatus.set(undefined);
    }
    this.loadReviews();
  }

  approveReview(reviewId: number): void {
    if (confirm('Are you sure you want to approve this review?')) {
      this.reviewService.approveReviewAdmin(reviewId).subscribe({
        next: () => {
          this.notificationService.showSuccess('Review approved successfully.');
          this.loadReviews(); // Listeyi yenile
        },
        error: (err) => {
          this.notificationService.showError('Failed to approve review.');
          console.error('Error approving review:', err);
        }
      });
    }
  }

  rejectReview(reviewId: number): void {
    if (confirm('Are you sure you want to reject this review? (This will mark it as not approved)')) {
      this.reviewService.rejectReviewAdmin(reviewId).subscribe({
        next: () => {
          this.notificationService.showSuccess('Review rejected successfully.');
          this.loadReviews(); // Listeyi yenile
        },
        error: (err) => {
          this.notificationService.showError('Failed to reject review.');
          console.error('Error rejecting review:', err);
        }
      });
    }
  }

  deleteReview(reviewId: number): void {
    if (confirm('Are you sure you want to permanently delete this review? This action cannot be undone.')) {
      this.reviewService.deleteReviewAdmin(reviewId).subscribe({
        next: () => {
          this.notificationService.showSuccess('Review deleted successfully.');
          this.loadReviews(); // Listeyi yenile
        },
        error: (err) => {
          this.notificationService.showError('Failed to delete review.');
          console.error('Error deleting review:', err);
        }
      });
    }
  }

  get pageNumbers(): number[] {
    const totalPages = this.reviewsResponse()?.totalPages ?? 0;
    return Array(totalPages).fill(0).map((x, i) => i);
  }
}
