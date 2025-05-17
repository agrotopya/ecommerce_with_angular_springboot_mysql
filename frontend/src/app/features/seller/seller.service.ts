// src/app/features/seller/seller.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
// Tipler Product ve ProductRequest olarak güncellendi. PaginatedProducts yerine Page<Product> kullanıldı.
import { Product, ProductRequest } from '../../shared/models/product.model';
import { Page } from '../../shared/models/page.model';
import { ApiResponse } from '../../shared/models/api-response.model';
import { OrderResponseDto } from '../../shared/models/order.model';
import { OrderStatus } from '../../shared/enums/order-status.enum';
import { SELLER_ENDPOINTS, ORDERS_ENDPOINTS, PRODUCT_ENDPOINTS } from '../../core/constants/api-endpoints';
import { AiImageGenerationRequest, AiImageGenerationResponse, SetAiImageAsProductRequest } from '../../shared/models/ai.model'; // AI DTO'ları eklendi

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

  // AI Özellikleri
  generateAiImage(request: AiImageGenerationRequest): Observable<AiImageGenerationResponse> {
    return this.apiService.post<AiImageGenerationResponse>(SELLER_ENDPOINTS.GENERATE_IMAGE, request);
  }

  setAiImageAsProduct(request: SetAiImageAsProductRequest): Observable<Product> {
    return this.apiService.post<Product>(SELLER_ENDPOINTS.SET_AI_IMAGE, request);
  }

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
    // Genel /api/orders/{orderId} endpoint'ini kullan, backend @orderSecurity ile yetkilendirme yapacak.
    return this.apiService.get<OrderResponseDto>(ORDERS_ENDPOINTS.ORDER_DETAIL(orderId));
  }

  updateMySellerOrderStatus(orderId: number, status: OrderStatus): Observable<OrderResponseDto> {
    // Backend /api/orders/{orderId}/status endpoint'ini kullanıyor ve status'u query param olarak alıyor.
    // SELLER_ENDPOINTS.UPDATE_ORDER_STATUS(orderId) doğru endpoint'i işaret etmeli.
    // Eğer SELLER_ENDPOINTS.UPDATE_ORDER_STATUS farklı bir yapıdaysa (örn: body bekliyorsa), ona göre düzenlenmeli.
    // OrderController'daki @PatchMapping("/{orderId}/status") @RequestParam OrderStatus status alıyor.
    // Bu endpoint genel ve @orderSecurity ile korunuyor.
    const params = new HttpParams().set('status', status.toString());
    return this.apiService.patch<OrderResponseDto>(ORDERS_ENDPOINTS.ORDER_STATUS_UPDATE(orderId), {}, { params });
  }

  addMySellerTrackingNumber(orderId: number, trackingNumber: string): Observable<OrderResponseDto> {
    // OrderController'daki @PatchMapping("/{orderId}/tracking") @RequestParam String trackingNumber alıyor.
    // Bu endpoint genel ve @orderSecurity ile korunuyor.
    const params = new HttpParams().set('trackingNumber', trackingNumber);
    return this.apiService.patch<OrderResponseDto>(ORDERS_ENDPOINTS.ORDER_TRACKING_UPDATE(orderId), {}, { params });
  }

  // Feel Yönetimi (FeelService'e taşındı veya burada da kalabilir, proje yapısına göre)
  // getMyFeels(...)
  // createFeel(...)

  uploadProductImages(productId: number, files: File[]): Observable<string[]> { // Dönen tip ProductResponse yerine string[] (URL listesi) olmalı
    const formData = new FormData();
    files.forEach((file, index) => {
      formData.append('files', file, file.name); // Backend @RequestParam("files") List<MultipartFile> files bekliyor
    });

    // PRODUCT_ENDPOINTS.DETAIL(productId) -> /products/{productId}
    // Buna /images ekleyerek /products/{productId}/images yolunu oluşturuyoruz.
    // Bu endpoint ProductController altında tanımlanmıştı.
    const endpoint = `${PRODUCT_ENDPOINTS.DETAIL(productId)}/images`;
    return this.apiService.post<string[]>(endpoint, formData); // Backend List<String> (imageUrls) dönecek
  }
}
