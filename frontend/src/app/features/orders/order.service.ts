// src/app/features/orders/order.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { HttpParams } from '@angular/common/http'; // HttpParams import edildi
import { ApiService } from '../../core/services/api.service';
import { ORDERS_ENDPOINTS, PAYMENTS_ENDPOINTS } from '../../core/constants/api-endpoints'; // PAYMENTS_ENDPOINTS eklendi
import { OrderResponseDto, CreateOrderRequestDto } from '../../shared/models/order.model'; // Order -> OrderResponseDto, OrderRequest -> CreateOrderRequestDto
import { Page } from '../../shared/models/page.model'; // Page modeli import edildi
import { MonthlySalesDto } from '../../shared/models/monthly-sales.model'; // MonthlySalesDto import edildi
// Payment modelleri eklendi - Bu satır zaten vardı, tekrar eklemeye gerek yok.
// Eğer '../../shared/models/payment.model' bulunamıyorsa, dosya yolu veya adı yanlış olabilir.
// Ya da payment.model.ts dosyası henüz oluşturulmamış olabilir.
// Önceki adımda oluşturmuştuk, import yolunu kontrol edelim.
// import { StripeCheckoutSessionResponse, CreateCheckoutSessionRequest } from '../../shared/models/payment.model';
// Yukarıdaki satır doğru görünüyor. Belki de bir önceki adımda dosya kaydedilirken bir sorun oldu.
// Tekrar kontrol edelim.
import { StripeCheckoutSessionResponse, CreateCheckoutSessionRequest } from '../../shared/models/payment.model';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private apiService = inject(ApiService);

  createOrder(orderRequest: CreateOrderRequestDto): Observable<OrderResponseDto> { // OrderRequest -> CreateOrderRequestDto, Order -> OrderResponseDto
    return this.apiService.post<OrderResponseDto>(ORDERS_ENDPOINTS.BASE, orderRequest);
  }

  createCheckoutSession(request: CreateCheckoutSessionRequest): Observable<StripeCheckoutSessionResponse> {
    return this.apiService.post<StripeCheckoutSessionResponse>(PAYMENTS_ENDPOINTS.CREATE_CHECKOUT_SESSION, request);
    // tap operatörü ve içindeki yönlendirme kaldırıldı.
  }

  getMyOrders(page: number = 0, size: number = 10, sort: string = 'orderDate,desc'): Observable<any> { // Page<OrderResponseDto> dönecek şekilde ayarlanabilir
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    return this.apiService.get<any>(ORDERS_ENDPOINTS.MY_ORDERS, params); // any -> Page<OrderResponseDto>
  }

  getMyOrderDetail(orderId: number): Observable<OrderResponseDto> { // Order -> OrderResponseDto
    return this.apiService.get<OrderResponseDto>(ORDERS_ENDPOINTS.MY_ORDER_DETAIL(orderId));
  }

  // Admin ve Seller için sipariş metodları da buraya eklenebilir
  // getOrderByIdForAdmin(orderId: number): Observable<OrderResponseDto> { ... }
  // updateOrderStatus(orderId: number, status: string): Observable<OrderResponseDto> { ... }

  getAllOrdersForAdmin(
    page: number = 0,
    size: number = 10,
    sort: string = 'orderDate,desc',
    customerId?: number,
    status?: string // OrderStatus enum string olarak
  ): Observable<Page<OrderResponseDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    if (customerId) {
      params = params.set('customerId', customerId.toString());
    }
    if (status) {
      params = params.set('status', status);
    }
    // API dokümanına göre admin için tüm siparişler ORDERS_ENDPOINTS.BASE üzerinden çekiliyor.
    // Eğer admin'e özel bir endpoint varsa (örn: ORDERS_ENDPOINTS.ADMIN_ALL_ORDERS), o kullanılmalı.
    // Şimdilik ORDERS_ENDPOINTS.BASE kullanıyoruz, bu endpoint'in admin yetkisiyle çağrıldığında
    // tüm siparişleri döndürdüğünü varsayıyoruz.
    return this.apiService.get<Page<OrderResponseDto>>(ORDERS_ENDPOINTS.BASE, params);
  }

  getSellerMonthlySalesSummary(): Observable<MonthlySalesDto[]> {
    return this.apiService.get<MonthlySalesDto[]>(ORDERS_ENDPOINTS.SELLER_MY_SALES_SUMMARY);
  }
}
