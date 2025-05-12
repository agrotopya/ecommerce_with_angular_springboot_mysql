import { Injectable, inject } from '@angular/core';
import { HttpParams } from '@angular/common/http'; // HttpClient kaldırıldı, ApiService kullanılacak
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { REVIEW_ENDPOINTS, ADMIN_ENDPOINTS } from '../../../core/constants/api-endpoints';
import { ReviewRequest, ReviewResponseDto } from '../../../shared/models/review.model';
import { Page } from '../../../shared/models/page.model';
import { ApiResponse } from '../../../shared/models/api-response.model'; // Tekrar doğru yol ile deniyoruz

@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  private apiService = inject(ApiService);

  constructor() { }

  getProductReviews(
    productId: number,
    page: number = 0,
    size: number = 5,
    approvedOnly: boolean = true
  ): Observable<Page<ReviewResponseDto>> {
    const url = REVIEW_ENDPOINTS.BY_PRODUCT(productId); // API_ENDPOINTS -> REVIEW_ENDPOINTS
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');

    if (approvedOnly) {
      params = params.set('approvedOnly', 'true');
    }
    return this.apiService.get<Page<ReviewResponseDto>>(url, params);
  }

  addReview(productId: number, request: ReviewRequest): Observable<ReviewResponseDto> {
    const url = REVIEW_ENDPOINTS.BY_PRODUCT(productId); // API_ENDPOINTS -> REVIEW_ENDPOINTS
    return this.apiService.post<ReviewResponseDto>(url, request);
  }

  deleteReview(reviewId: number): Observable<ApiResponse> {
    const url = REVIEW_ENDPOINTS.DETAIL(reviewId); // API_ENDPOINTS.REVIEWS_BY_ID -> REVIEW_ENDPOINTS.DETAIL
    return this.apiService.delete<ApiResponse>(url);
  }

  // Admin-specific methods
  getAllReviewsAdmin(params: HttpParams): Observable<Page<ReviewResponseDto>> {
    const url = ADMIN_ENDPOINTS.ADMIN_REVIEWS_ALL; // API_ENDPOINTS -> ADMIN_ENDPOINTS
    return this.apiService.get<Page<ReviewResponseDto>>(url, params);
  }

  approveReviewAdmin(reviewId: number): Observable<ReviewResponseDto> {
    const url = ADMIN_ENDPOINTS.ADMIN_REVIEW_APPROVE(reviewId); // API_ENDPOINTS -> ADMIN_ENDPOINTS
    return this.apiService.patch<ReviewResponseDto>(url, {});
  }

  rejectReviewAdmin(reviewId: number): Observable<ReviewResponseDto> {
    const url = ADMIN_ENDPOINTS.ADMIN_REVIEW_REJECT(reviewId); // API_ENDPOINTS -> ADMIN_ENDPOINTS
    return this.apiService.patch<ReviewResponseDto>(url, {});
  }

  deleteReviewAdmin(reviewId: number): Observable<ApiResponse> {
    const url = ADMIN_ENDPOINTS.ADMIN_REVIEW_DELETE(reviewId); // API_ENDPOINTS -> ADMIN_ENDPOINTS
    return this.apiService.delete<ApiResponse>(url);
  }
}
