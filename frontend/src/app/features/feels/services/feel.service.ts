import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, tap, map, catchError, of, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { FEEL_ENDPOINTS } from '../../../../app/core/constants/api-endpoints';
import { FeelResponseDto, CreateFeelRequestDto } from '../../../../app/shared/models/feel.model';
import { ApiResponse } from '../../../../app/shared/models/api-response.model';
import { NotificationService } from '../../../../app/core/services/notification.service';
import { Page } from '../../../../app/shared/models/page.model';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class FeelService {
  private apiService = inject(ApiService);
  private notificationService = inject(NotificationService);

  constructor() {}

  getPublicFeels(page: number = 0, size: number = 10): Observable<Page<FeelResponseDto>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');
    return this.apiService.get<Page<FeelResponseDto>>(FEEL_ENDPOINTS.BASE, params).pipe(
      catchError((error: HttpErrorResponse) => {
        this.notificationService.showError('Failed to load feels.');
        return throwError(() => error);
      })
    );
  }

  getFeelById(feelId: number): Observable<FeelResponseDto> {
    return this.apiService.get<FeelResponseDto>(FEEL_ENDPOINTS.DETAIL(feelId)).pipe(
      catchError((error: HttpErrorResponse) => {
        this.notificationService.showError('Failed to load feel details.');
        return throwError(() => error);
      })
    );
  }

  getFeelsByProductId(productId: number, page: number = 0, size: number = 5): Observable<Page<FeelResponseDto>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');
    return this.apiService.get<Page<FeelResponseDto>>(FEEL_ENDPOINTS.BY_PRODUCT(productId), params).pipe(
      catchError((error: HttpErrorResponse) => {
        this.notificationService.showError(`Failed to load feels for product ${productId}.`);
        return throwError(() => error);
      })
    );
  }

  getFeelsBySeller(sellerId: number, page: number = 0, size: number = 10): Observable<Page<FeelResponseDto>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');
    // FEEL_ENDPOINTS.BY_SELLER(sellerId) endpoint'inin /api/feels/seller/{sellerId} olduğunu varsayıyoruz.
    return this.apiService.get<Page<FeelResponseDto>>(FEEL_ENDPOINTS.BY_SELLER(sellerId), params).pipe(
      catchError((error: HttpErrorResponse) => {
        this.notificationService.showError(`Failed to load feels for seller ${sellerId}.`);
        return throwError(() => error);
      })
    );
  }

  likeFeel(feelId: number): Observable<ApiResponse> {
    return this.apiService.post<ApiResponse>(FEEL_ENDPOINTS.LIKE(feelId), {}).pipe(
      tap(() => this.notificationService.showSuccess('Feel liked!')),
      catchError((error: HttpErrorResponse) => {
        this.notificationService.showError('Failed to like feel.');
        return throwError(() => error);
      })
    );
  }

  unlikeFeel(feelId: number): Observable<ApiResponse> {
    return this.apiService.delete<ApiResponse>(FEEL_ENDPOINTS.LIKE(feelId)).pipe(
      tap(() => this.notificationService.showSuccess('Feel unliked.')),
      catchError((error: HttpErrorResponse) => {
        this.notificationService.showError('Failed to unlike feel.');
        return throwError(() => error);
      })
    );
  }

  createFeel(dto: CreateFeelRequestDto, videoFile: File, thumbnailFile?: File): Observable<FeelResponseDto> {
    const formData = new FormData();
    formData.append('dto', new Blob([JSON.stringify(dto)], { type: 'application/json' }));
    formData.append('videoFile', videoFile);
    if (thumbnailFile) {
      formData.append('thumbnailFile', thumbnailFile);
    }
    return this.apiService.post<FeelResponseDto>(FEEL_ENDPOINTS.BASE, formData).pipe(
      tap(() => this.notificationService.showSuccess('Feel created successfully!')),
      catchError((error: HttpErrorResponse) => {
        this.notificationService.showError('Failed to create feel.');
        console.error('Feel creation error:', error);
        return throwError(() => error);
      })
    );
  }

  updateFeel(feelId: number, dto: CreateFeelRequestDto, videoFile?: File, thumbnailFile?: File): Observable<FeelResponseDto> {
    const formData = new FormData();
    formData.append('dto', new Blob([JSON.stringify(dto)], { type: 'application/json' }));
    if (videoFile) {
      formData.append('videoFile', videoFile);
    }
    if (thumbnailFile) {
      formData.append('thumbnailFile', thumbnailFile);
    }
    console.warn('updateFeel called, but backend might not support PUT /feels/{id} for seller. Consider backend changes.');
    return this.apiService.post<FeelResponseDto>(`${FEEL_ENDPOINTS.BASE}`, formData).pipe(
      tap(() => this.notificationService.showSuccess('Feel update attempted (used POST).')),
      catchError((error: HttpErrorResponse) => {
        this.notificationService.showError('Error updating feel.');
        console.error('Feel update error:', error);
        return throwError(() => error);
      })
    );
  }

  getMyFeels(page: number = 0, size: number = 10): Observable<Page<FeelResponseDto>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');
    return this.apiService.get<Page<FeelResponseDto>>(FEEL_ENDPOINTS.MY_FEELS, params).pipe(
      catchError((error: HttpErrorResponse) => {
        this.notificationService.showError('Failed to load your feels.');
        return throwError(() => error);
      })
    );
  }

  deleteFeel(feelId: number): Observable<ApiResponse> {
    return this.apiService.delete<ApiResponse>(FEEL_ENDPOINTS.DETAIL(feelId)).pipe(
      tap(() => this.notificationService.showSuccess('Feel deleted successfully.')),
      catchError((error: HttpErrorResponse) => {
        this.notificationService.showError('Failed to delete feel.');
        return throwError(() => error);
      })
    );
  }

  // Admin Methods
  getAllFeelsForAdmin(page: number = 0, size: number = 10, isActive?: boolean): Observable<Page<FeelResponseDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');
    if (isActive !== undefined) {
      params = params.set('isActive', isActive.toString());
    }
    return this.apiService.get<Page<FeelResponseDto>>(FEEL_ENDPOINTS.ADMIN_GET_ALL, params).pipe(
      catchError((error: HttpErrorResponse) => {
        this.notificationService.showError('Failed to load feels for admin.');
        return throwError(() => error);
      })
    );
  }

  updateFeelStatusByAdmin(feelId: number, isActive: boolean): Observable<FeelResponseDto> {
    const url = `${FEEL_ENDPOINTS.ADMIN_UPDATE_STATUS(feelId)}?isActive=${isActive}`;
    return this.apiService.patch<FeelResponseDto>(url, {}).pipe(
      tap(() => this.notificationService.showSuccess(`Feel status updated to ${isActive ? 'Active' : 'Inactive'}.`)),
      catchError((error: HttpErrorResponse) => {
        this.notificationService.showError('Failed to update feel status.');
        return throwError(() => error);
      })
    );
  }

  deleteFeelByAdmin(feelId: number): Observable<ApiResponse> {
    return this.apiService.delete<ApiResponse>(FEEL_ENDPOINTS.DETAIL(feelId)).pipe(
      tap(() => this.notificationService.showSuccess('Feel deleted by admin successfully.')),
      catchError((error: HttpErrorResponse) => {
        this.notificationService.showError('Failed to delete feel by admin.');
        return throwError(() => error);
      })
    );
  }
}
