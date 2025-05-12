// src/app/features/seller/seller.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
// Tipler Product ve ProductRequest olarak güncellendi. PaginatedProducts yerine Page<Product> kullanıldı.
import { Product, ProductRequest } from '../../shared/models/product.model';
import { Page } from '../../shared/models/page.model';
import { ApiResponse } from '../../shared/models/api-response.model'; // user.model -> api-response.model olarak düzeltildi
import { OrderResponseDto } from '../../shared/models/order.model'; // OrderResponseDto import edildi
import { OrderStatus } from '../../shared/enums/order-status.enum'; // OrderStatus enum import edildi
import { SELLER_ENDPOINTS } from '../../core/constants/api-endpoints'; // SELLER_ENDPOINTS import edildi

// const SELLER_API_PATH = '/seller'; // SELLER_ENDPOINTS kullanıldığı için bu satır kaldırıldı

@Injectable({
  providedIn: 'root'
})
export class SellerService {
  private apiService = inject(ApiService);

  // Ürün Yönetimi
  getMyProducts(page: number = 0, size: number = 10, sort: string = 'name,asc'): Observable<Page<Product>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    return this.apiService.get<Page<Product>>(SELLER_ENDPOINTS.MY_PRODUCTS, params);
  }

  getProductByIdForSeller(productId: number): Observable<Product> {
    return this.apiService.get<Product>(SELLER_ENDPOINTS.PRODUCT_DETAIL(productId));
  }

  createProduct(productData: ProductRequest): Observable<Product> { // Genellikle oluşturulan ürünü döner
    return this.apiService.post<Product>(SELLER_ENDPOINTS.PRODUCTS_BASE, productData);
  }

  updateProduct(productId: number, productData: ProductRequest): Observable<Product> { // Genellikle güncellenen ürünü döner
    return this.apiService.put<Product>(SELLER_ENDPOINTS.PRODUCT_DETAIL(productId), productData);
  }

  deleteProduct(productId: number): Observable<ApiResponse> { // Genellikle sadece başarı/hata mesajı döner
    return this.apiService.delete<ApiResponse>(SELLER_ENDPOINTS.PRODUCT_DETAIL(productId));
  }

  activateProduct(productId: number, isActive: boolean): Observable<Product> {
    const params = new HttpParams().set('isActive', isActive.toString());
    return this.apiService.patch<Product>(SELLER_ENDPOINTS.ACTIVATE_PRODUCT(productId), {}, { params });
  }

  uploadProductImage(productId: number, formData: FormData): Observable<Product> {
    return this.apiService.post<Product>(SELLER_ENDPOINTS.UPLOAD_IMAGE(productId), formData);
  }

  // AI Özellikleri (Eğer bu serviste olacaksa)
  // generateAiImage(request: AiImageGenerationRequest): Observable<AiImageGenerationResponse> {
  //   return this.apiService.post<AiImageGenerationResponse>(`${SELLER_API_PATH}/products/generate-image`, request);
  // }

  // setAiImageAsProduct(request: SetAiImageAsProductRequest): Observable<Product> {
  //   return this.apiService.post<Product>(`${SELLER_API_PATH}/products/set-ai-image`, request);
  // }

  // Sipariş Yönetimi
  getMySellerOrders(page: number = 0, size: number = 10, sort: string = 'orderDate,desc', status?: OrderStatus): Observable<Page<OrderResponseDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    if (status) {
      params = params.set('status', status);
    }
    return this.apiService.get<Page<OrderResponseDto>>(SELLER_ENDPOINTS.MY_ORDERS, params);
  }

  getMySellerOrderDetail(orderId: number): Observable<OrderResponseDto> {
    return this.apiService.get<OrderResponseDto>(SELLER_ENDPOINTS.ORDER_DETAIL(orderId));
  }

  updateMySellerOrderStatus(orderId: number, status: OrderStatus): Observable<OrderResponseDto> {
    // Backend bu endpoint için request body bekliyorsa, { status } şeklinde gönderilebilir.
    // Şimdilik query param veya path variable ile çözülmediği varsayılarak boş body gönderiliyor.
    // Backend'in /api/seller/orders/{orderId}/status endpoint'i PATCH ve RequestBody olarak OrderStatusUpdateRequestDto bekliyor olabilir.
    // Bu durumda { status: status } veya backend'in beklediği DTO formatında gönderilmeli.
    // Şimdilik basit bir PATCH varsayımı yapıyoruz, backend'e göre düzenlenmeli.
    // Eğer backend sadece status'u query param olarak alıyorsa:
    // const params = new HttpParams().set('status', status);
    // return this.apiService.patch<OrderResponseDto>(SELLER_ENDPOINTS.UPDATE_ORDER_STATUS(orderId), {}, { params });
    // Eğer backend request body'de { "status": "SHIPPED" } gibi bir şey bekliyorsa:
    return this.apiService.patch<OrderResponseDto>(SELLER_ENDPOINTS.UPDATE_ORDER_STATUS(orderId), { status });
  }

  // Feel Yönetimi (FeelService'e taşındı veya burada da kalabilir, proje yapısına göre)
  // getMyFeels(...)
  // createFeel(...)
}
