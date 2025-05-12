import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { ApiService } from '../../../core/services/api.service';
import { COUPON_ENDPOINTS } from '../../../core/constants/api-endpoints';
import { CouponValidationResponse, CouponResponse, CouponRequest } from '../models/coupon.model'; // Modelleri import et
import { Page } from '../../../shared/models/page.model';

@Injectable({
  providedIn: 'root'
})
export class CouponService {
  private apiService = inject(ApiService);

  constructor() { }

  validateCoupon(code: string, cartTotal: number): Observable<CouponValidationResponse> {
    const params = new HttpParams().set('cartTotal', cartTotal.toString());
    return this.apiService.get<CouponValidationResponse>(COUPON_ENDPOINTS.VALIDATE(code), params);
  }

  // Admin - Kupon Oluşturma
  createCoupon(couponRequest: CouponRequest): Observable<CouponResponse> {
    return this.apiService.post<CouponResponse>(COUPON_ENDPOINTS.ADMIN_BASE, couponRequest);
  }

  // Admin - Tüm Kuponları Getirme
  getCoupons(page: number, size: number, sort: string, isActive?: boolean): Observable<Page<CouponResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    if (isActive !== undefined) {
      params = params.set('isActive', isActive.toString());
    }
    return this.apiService.get<Page<CouponResponse>>(COUPON_ENDPOINTS.ADMIN_BASE, params);
  }

  // Admin - Kupon Detayı ID ile
  getCouponById(couponId: number): Observable<CouponResponse> {
    return this.apiService.get<CouponResponse>(COUPON_ENDPOINTS.ADMIN_DETAIL_BY_ID(couponId));
  }

  // Admin - Kupon Detayı Kod ile
  getCouponByCode(code: string): Observable<CouponResponse> {
    return this.apiService.get<CouponResponse>(COUPON_ENDPOINTS.ADMIN_DETAIL_BY_CODE(code));
  }

  // Admin - Kupon Güncelleme
  updateCoupon(couponId: number, couponRequest: CouponRequest): Observable<CouponResponse> {
    return this.apiService.put<CouponResponse>(COUPON_ENDPOINTS.ADMIN_DETAIL_BY_ID(couponId), couponRequest);
  }

  // Admin - Kupon Silme
  deleteCoupon(couponId: number): Observable<void> { // Genellikle bir şey dönmez
    return this.apiService.delete<void>(COUPON_ENDPOINTS.ADMIN_DETAIL_BY_ID(couponId));
  }
}
