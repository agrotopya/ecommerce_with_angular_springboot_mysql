import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from './api.service';
import { NotificationService } from './notification.service';
import { AI_ENDPOINTS } from '../constants/api-endpoints'; // AI_ENDPOINTS eklenmeli
import { ProductEnhancementRequestDto, ProductEnhancementResponseDto } from '@shared/models/ai.model'; // Yeni DTO'lar için model dosyası

@Injectable({
  providedIn: 'root'
})
export class AiService {
  private apiService = inject(ApiService);
  private notificationService = inject(NotificationService);

  constructor() { }

  enhanceProductDetails(requestDto: ProductEnhancementRequestDto): Observable<ProductEnhancementResponseDto> {
    return this.apiService.post<ProductEnhancementResponseDto>(AI_ENDPOINTS.ENHANCE_PRODUCT_DETAILS, requestDto).pipe(
      tap((response) => {
        if (response.success) {
          this.notificationService.showSuccess('AI enhancements received!');
        } else {
          this.notificationService.showError(response.errorMessage || 'Failed to get AI enhancements.');
        }
      }),
      catchError((error: HttpErrorResponse) => {
        this.notificationService.showError('Error communicating with AI service.');
        console.error('AI enhancement error:', error);
        return throwError(() => error);
      })
    );
  }

  // generateImage metodu da buraya taşınabilir veya SellerService/ProductService içinde kalabilir.
  // generateImage(requestDto: AiImageGenerationRequestDto): Observable<AiImageGenerationResponseDto> {
  //   return this.apiService.post<AiImageGenerationResponseDto>(AI_ENDPOINTS.GENERATE_IMAGE, requestDto).pipe(
  //     // ...
  //   );
  // }
}
