import { Component, inject, OnInit, signal, WritableSignal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { SellerService } from '../../seller.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { Page } from '../../../../shared/models/page.model';
import { OrderResponseDto } from '../../../../shared/models/order.model';
import { OrderStatus } from '../../../../shared/enums/order-status.enum';
import { PaymentStatus } from '../../../../shared/enums/payment-status.enum'; // PaymentStatus import edildi
import { FormsModule } from '@angular/forms'; // FormsModule eklendi
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago.pipe'; // TimeAgoPipe import edildi

@Component({
  selector: 'app-my-seller-orders-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TimeAgoPipe], // FormsModule ve TimeAgoPipe eklendi
  templateUrl: './my-seller-orders-list.component.html',
  styleUrls: ['./my-seller-orders-list.component.scss'],
  providers: [DatePipe]
})
export class MySellerOrdersListComponent implements OnInit {
  private sellerService = inject(SellerService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);

  orders: WritableSignal<Page<OrderResponseDto> | null> = signal(null);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  currentPage = signal(0);
  pageSize = signal(10);
  sortColumn = signal('orderDate');
  sortDirection = signal('desc');

  // Filtreleme için
  selectedStatus: WritableSignal<OrderStatus | ''> = signal(''); // OrderStatus veya boş string
  orderStatusOptions = computed(() => {
    // OrderStatus enum'unu bir array'e dönüştür, kullanıcı dostu isimlerle
    return Object.values(OrderStatus).map(status => ({ label: this.formatOrderStatus(status), value: status }));
  });


  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(page: number = this.currentPage()): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.currentPage.set(page);

    const sort = `${this.sortColumn()},${this.sortDirection()}`;
    const statusValue = this.selectedStatus();
    const statusParam = statusValue === '' ? undefined : statusValue;

    this.sellerService.getMySellerOrders(this.currentPage(), this.pageSize(), sort, statusParam).subscribe({
      next: (response) => {
        this.orders.set(response);
        this.isLoading.set(false);
        if (response.content.length === 0 && page > 0) {
          this.loadOrders(page - 1);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load your orders.');
        this.notificationService.showError(this.errorMessage()!);
        console.error('Error loading seller orders:', err);
      }
    });
  }

  onPageChange(page: number): void {
    this.loadOrders(page);
  }

  onSort(column: string): void {
    if (this.sortColumn() === column) {
      this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortColumn.set(column);
      this.sortDirection.set('asc');
    }
    this.loadOrders(0); // Sıralama değiştiğinde ilk sayfaya git
  }

  onFilterChange(): void {
    this.loadOrders(0); // Filtre değiştiğinde ilk sayfaya git
  }

  viewOrderDetail(orderId: number): void {
    this.router.navigate(['/seller/orders', orderId]);
  }

  formatOrderStatus(status: OrderStatus): string {
    if (!status) return '';
    return status.replace(/_/g, ' ').replace(/\w\S*/g, (txt) => txt.charAt(0).toUpperCase() + txt.substring(1).toLowerCase());
  }

  formatPaymentStatus(status: PaymentStatus): string {
    if (!status) return '';
    return status.replace(/_/g, ' ').replace(/\w\S*/g, (txt) => txt.charAt(0).toUpperCase() + txt.substring(1).toLowerCase());
  }

  // Sipariş durumunu güncellemek için (ileride MySellerOrderDetailComponent'te daha detaylı olacak)
  // updateStatus(orderId: number, newStatus: OrderStatus): void {
  //   this.sellerService.updateMySellerOrderStatus(orderId, newStatus).subscribe({
  //     next: () => {
  //       this.notificationService.showSuccess('Order status updated successfully.');
  //       this.loadOrders(this.currentPage()); // Listeyi yenile
  //     },
  //     error: (err) => {
  //       this.notificationService.showError(err.error?.message || 'Failed to update order status.');
  //     }
  //   });
  // }
}
