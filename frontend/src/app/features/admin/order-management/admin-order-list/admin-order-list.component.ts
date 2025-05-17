import { Component, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http'; // HttpErrorResponse eklendi
import { AdminService } from '../../admin.service';
import { OrderResponseDto } from '../../../../shared/models/order.model';
import { Page } from '../../../../shared/models/page.model';
import { OrderStatus } from '../../../../shared/enums/order-status.enum';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthService } from '../../../../core/services/auth.service'; // AuthService eklendi
import { Role } from '../../../../shared/enums/role.enum'; // Role eklendi

@Component({
  selector: 'app-admin-order-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './admin-order-list.component.html',
  styleUrls: ['./admin-order-list.component.scss']
})
export class AdminOrderListComponent implements OnInit {
  private adminService = inject(AdminService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);
  private authService = inject(AuthService); // AuthService inject edildi

  ordersPage = signal<Page<OrderResponseDto> | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  // Filtreleme
  filterCustomerId: string = ''; // Bunu müşteri arama (keyword) ile değiştirebiliriz veya ekleyebiliriz
  filterCustomerKeyword: string = ''; // Yeni: Müşteri adı, e-postası vb. için
  filterStatus: string = '';
  filterStartDate: string = ''; // Yeni: Başlangıç tarihi
  filterEndDate: string = '';   // Yeni: Bitiş tarihi
  orderStatusOptions = Object.values(OrderStatus);

  // Sayfalama
  currentPage = 0;
  pageSize = 15;
  currentSort = 'orderDate,desc';

  // Her sipariş için düzenleme verilerini tutacak Map
  editingOrdersData = new Map<number, { selectedStatus?: OrderStatus, trackingNumberInput?: string }>();
  public OrderStatus = OrderStatus; // HTML'de enum'a erişim için

  constructor() {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(page: number = this.currentPage): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.currentPage = page;

    // const customerId = this.filterCustomerId ? Number(this.filterCustomerId) : undefined; // Eski ID filtresi
    const customerKeyword = this.filterCustomerKeyword.trim() || undefined;
    const status = this.filterStatus ? (this.filterStatus as OrderStatus) : undefined;
    const startDate = this.filterStartDate || undefined;
    const endDate = this.filterEndDate || undefined;

    this.adminService.getAllOrdersForAdmin(
      this.currentPage,
      this.pageSize,
      this.currentSort,
      // customerId, // Eski ID filtresi
      customerKeyword, // Yeni keyword filtresi
      status,
      startDate,
      endDate
    ).subscribe({
      next: (data: Page<OrderResponseDto>) => {
        this.ordersPage.set(data);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => { // Tip eklendi
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load orders. Please try again.');
        this.notificationService.showError(err.error?.message || 'Failed to load orders.');
        console.error('Error loading orders for admin:', err);
      }
    });
  }

  applyFilters(): void {
    this.loadOrders(0); // Filtre uygulandığında ilk sayfaya dön
  }

  clearFilters(): void {
    this.filterCustomerId = ''; // Eğer hala kullanılıyorsa
    this.filterCustomerKeyword = '';
    this.filterStatus = '';
    this.filterStartDate = '';
    this.filterEndDate = '';
    this.loadOrders(0);
  }

  onPageChange(page: number): void {
    if (page >= 0 && page < (this.ordersPage()?.totalPages ?? 0)) {
      this.loadOrders(page);
    }
  }

  get pageNumbers(): number[] {
    const totalPages = this.ordersPage()?.totalPages ?? 0;
    return Array(totalPages).fill(0).map((x, i) => i);
  }

  navigateToOrderDetail(orderId: number): void {
    this.router.navigate(['/admin/orders', orderId]); // Admin sipariş detay sayfasına yönlendir
  }

  // Sipariş durumunu hızlı güncelleme (ileride eklenebilir)
  // quickUpdateStatus(orderId: number, newStatus: OrderStatus): void { ... }

  isAdmin(): boolean {
    return this.authService.hasRole(Role.ADMIN);
  }

  getOrderEditData(orderId: number): { selectedStatus?: OrderStatus, trackingNumberInput?: string } {
    if (!this.editingOrdersData.has(orderId)) {
      // İlgili siparişin mevcut durumunu ve takip numarasını başlangıç değeri olarak ata
      const order = this.ordersPage()?.content?.find(o => o.id === orderId);
      this.editingOrdersData.set(orderId, {
        selectedStatus: order?.status,
        trackingNumberInput: order?.trackingNumber || ''
      });
    }
    return this.editingOrdersData.get(orderId)!;
  }

  updateOrderStatusHandler(order: OrderResponseDto, newStatus?: OrderStatus): void {
    if (!newStatus || !order.id) {
      this.notificationService.showWarning('Please select a new status.');
      return;
    }
    if (newStatus === order.status) {
      this.notificationService.showInfo('Order is already in the selected status.');
      return;
    }

    this.isLoading.set(true); // Yükleme göstergesini başlat
    this.adminService.updateOrderStatusForAdmin(order.id, newStatus).subscribe({
      next: (updatedOrder) => {
        this.notificationService.showSuccess(`Order #${order.id} status updated to ${newStatus}.`);
        // Listeyi yenilemek yerine sadece güncellenen siparişi değiştirebiliriz
        const currentOrders = this.ordersPage()?.content || [];
        const index = currentOrders.findIndex(o => o.id === order.id);
        if (index > -1) {
          currentOrders[index] = updatedOrder;
          this.ordersPage.set({ ...this.ordersPage()!, content: [...currentOrders] });
        } else {
          this.loadOrders(this.currentPage); // Veya tüm listeyi yenile
        }
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.notificationService.showError(err.error?.message || `Failed to update status for order #${order.id}`);
        console.error('Error updating order status:', err);
        // Başarısız olursa, seçili durumu eski haline getirebiliriz
        const editData = this.getOrderEditData(order.id);
        editData.selectedStatus = order.status;
      }
    });
  }

  addTrackingNumberHandler(order: OrderResponseDto, trackingNumber?: string): void {
    const trackNum = trackingNumber?.trim();
    if (!trackNum || !order.id) {
      this.notificationService.showWarning('Please enter a tracking number.');
      return;
    }

    this.isLoading.set(true); // Yükleme göstergesini başlat
    this.adminService.addTrackingNumberForAdmin(order.id, trackNum).subscribe({
      next: (updatedOrder) => {
        this.notificationService.showSuccess(`Tracking number updated for order #${order.id}.`);
        const currentOrders = this.ordersPage()?.content || [];
        const index = currentOrders.findIndex(o => o.id === order.id);
        if (index > -1) {
          currentOrders[index] = updatedOrder; // Hem trackingNumber hem de status güncellenmiş olabilir
          this.ordersPage.set({ ...this.ordersPage()!, content: [...currentOrders] });
           // Input alanını da güncelle (eğer DTO'dan farklıysa)
          const editData = this.getOrderEditData(order.id);
          editData.trackingNumberInput = updatedOrder.trackingNumber || '';
          editData.selectedStatus = updatedOrder.status; // Durum da değişmiş olabilir
        } else {
          this.loadOrders(this.currentPage);
        }
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.notificationService.showError(err.error?.message || `Failed to add tracking number for order #${order.id}`);
        console.error('Error adding tracking number:', err);
      }
    });
  }
}
