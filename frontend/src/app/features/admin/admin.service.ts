// src/app/features/admin/admin.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { User } from '../../shared/models/user.model'; // UserResponse -> User
import { Role } from '../../shared/enums/role.enum';
import { ADMIN_ENDPOINTS, ORDERS_ENDPOINTS } from '../../core/constants/api-endpoints'; // ADMIN_ENDPOINTS ve ORDERS_ENDPOINTS import et
import { Product } from '../../shared/models/product.model';
import { Page } from '../../shared/models/page.model';
import { ApiResponse } from '../../shared/models/user.model';
import { OrderResponseDto } from '../../shared/models/order.model'; // OrderResponse -> OrderResponseDto
import { OrderStatus } from '../../shared/enums/order-status.enum'; // OrderStatus enum import et

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiService = inject(ApiService);

  // Kullanıcıları listeleme (sayfalama ve filtreleme ile)
  getUsers(
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc',
    searchTerm?: string,
    role?: Role,
    isActive?: boolean
  ): Observable<Page<User>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchTerm) {
      params = params.set('search', searchTerm);
    }
    if (role) {
      params = params.set('role', role);
    }
    if (isActive !== undefined) {
      params = params.set('active', isActive.toString());
    }
    return this.apiService.get<Page<User>>(ADMIN_ENDPOINTS.USERS_BASE, params);
  }

  // Kullanıcı detayını getirme
  getUserById(userId: number | string): Observable<User> {
    return this.apiService.get<User>(ADMIN_ENDPOINTS.USER_DETAIL(userId));
  }

  // Kullanıcı durumunu güncelleme (aktif/pasif)
  updateUserStatus(userId: number | string, isActive: boolean): Observable<User> {
    // API dokümantasyonuna göre (13.3): PATCH /api/admin/users/{userId}/status?isActive=true
    return this.apiService.patch<User>(`${ADMIN_ENDPOINTS.USER_UPDATE_STATUS(userId)}?isActive=${isActive}`, {});
  }

  // Kullanıcı rolünü güncelleme
  updateUserRole(userId: number | string, role: Role): Observable<User> {
    // API dokümantasyonuna göre (13.4): PATCH /api/admin/users/{userId}/role?role=ADMIN (enum: CUSTOMER, SELLER, ADMIN)
    // Role enum'ı zaten 'CUSTOMER', 'SELLER', 'ADMIN' değerlerini içeriyor olmalı.
    console.log(`AdminService: Updating role for user ${userId} to ${role}`);
    return this.apiService.patch<User>(`${ADMIN_ENDPOINTS.USER_UPDATE_ROLE(userId)}?role=${role}`, {});
  }

  // Ürün Yönetimi (Admin)
  getAllProductsForAdmin(
    page: number = 0,
    size: number = 10,
    sort: string = 'createdAt,desc',
    approvalStatus?: boolean // true for approved, false for not_approved/pending, undefined for all
    // Diğer filtreler eklenebilir: sellerId, categoryId, searchTerm vb.
  ): Observable<Page<Product>> { // PaginatedProducts -> Page<Product>
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (approvalStatus !== undefined) {
      // Backend'in beklediği parametre adı ve değeri (örn: 'isApproved=true' veya 'status=APPROVED')
      // API dokümantasyonunuza göre ayarlayın.
      // Şimdilik 'isApproved' kullandığımızı varsayalım.
      params = params.set('isApproved', approvalStatus.toString());
    }
    // Diğer filtreler için:
    // if (sellerId) params = params.set('sellerId', sellerId.toString());
    // if (searchTerm) params = params.set('search', searchTerm);

    return this.apiService.get<Page<Product>>(ADMIN_ENDPOINTS.ADMIN_PRODUCTS_ALL, params);
  }

  approveProduct(productId: number): Observable<Product> { // Genellikle güncellenmiş ürünü döner
    return this.apiService.patch<Product>(ADMIN_ENDPOINTS.ADMIN_PRODUCT_APPROVE(productId), {});
  }

  rejectProduct(productId: number): Observable<Product> { // Genellikle güncellenmiş ürünü döner
    // Backend bu endpoint için bir body bekliyorsa (örn: red nedeni), onu ekleyin.
    return this.apiService.patch<Product>(ADMIN_ENDPOINTS.ADMIN_PRODUCT_REJECT(productId), {});
  }

  deleteProductByAdmin(productId: number): Observable<ApiResponse> { // Genellikle sadece başarı/hata mesajı döner
    return this.apiService.delete<ApiResponse>(ADMIN_ENDPOINTS.ADMIN_PRODUCT_DELETE(productId));
  }

  setAdminProductActiveStatus(productId: number, isActive: boolean): Observable<Product> {
    const params = new HttpParams().set('isActive', isActive.toString());
    return this.apiService.patch<Product>(ADMIN_ENDPOINTS.ADMIN_PRODUCT_ACTIVATE(productId), {}, { params });
  }

  // Sipariş Yönetimi (Admin)
  getAllOrdersForAdmin(
    page: number = 0,
    size: number = 10,
    sort: string = 'orderDate,desc',
    customerKeyword?: string, // customerId -> customerKeyword olarak değiştirildi
    status?: OrderStatus,
    startDate?: string, // startDate eklendi
    endDate?: string    // endDate eklendi
  ): Observable<Page<OrderResponseDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (customerKeyword) { // customerId -> customerKeyword
      params = params.set('customerKeyword', customerKeyword); // Parametre adı backend'e göre ayarlanmalı
    }
    if (status) {
      params = params.set('status', status);
    }
    if (startDate) {
      params = params.set('startDate', startDate); // Parametre adı backend'e göre ayarlanmalı
    }
    if (endDate) {
      params = params.set('endDate', endDate);     // Parametre adı backend'e göre ayarlanmalı
    }
    return this.apiService.get<Page<OrderResponseDto>>(ORDERS_ENDPOINTS.BASE, params);
  }

  getOrderDetailsForAdmin(orderId: number | string): Observable<OrderResponseDto> {
    // apidocs.md 8.6 GET /api/orders/{orderId} (Admin)
    return this.apiService.get<OrderResponseDto>(ORDERS_ENDPOINTS.ORDER_DETAIL(Number(orderId))); // orderId Number'a çevrildi
  }

  updateOrderStatusForAdmin(orderId: number | string, status: OrderStatus): Observable<OrderResponseDto> { // OrderResponse -> OrderResponseDto
    // apidocs.md 8.7 PATCH /api/orders/{orderId}/status?status=SHIPPED
    const params = new HttpParams().set('status', status);
    return this.apiService.patch<OrderResponseDto>(ORDERS_ENDPOINTS.ORDER_STATUS_UPDATE(Number(orderId)), {}, { params }); // orderId Number'a çevrildi, OrderResponse -> OrderResponseDto
  }

  addTrackingNumberForAdmin(orderId: number | string, trackingNumber: string): Observable<OrderResponseDto> { // OrderResponse -> OrderResponseDto
    // apidocs.md 8.8 PATCH /api/orders/{orderId}/tracking?trackingNumber=XYZ
    const params = new HttpParams().set('trackingNumber', trackingNumber);
    return this.apiService.patch<OrderResponseDto>(ORDERS_ENDPOINTS.ORDER_TRACKING_UPDATE(Number(orderId)), {}, { params }); // orderId Number'a çevrildi, OrderResponse -> OrderResponseDto
  }
}
