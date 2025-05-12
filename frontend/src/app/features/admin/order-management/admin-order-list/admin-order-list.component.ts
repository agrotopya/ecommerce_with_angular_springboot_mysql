import { Component, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http'; // HttpErrorResponse eklendi
import { AdminService } from '../../admin.service'; // Yol düzeltildi
import { OrderResponseDto } from '../../../../shared/models/order.model';
import { Page } from '../../../../shared/models/page.model';
import { OrderStatus } from '../../../../shared/enums/order-status.enum';
import { NotificationService } from '../../../../core/services/notification.service';

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
  pageSize = 15; // Sayfa başına sipariş sayısı
  currentSort = 'orderDate,desc'; // Varsayılan sıralama

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
}
