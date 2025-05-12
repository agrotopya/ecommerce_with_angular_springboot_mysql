// src/app/features/orders/order-list/order-list.component.ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { OrderService } from '../order.service';
import { OrderResponseDto } from '../../../shared/models/order.model'; // Order -> OrderResponseDto
import { Page } from '../../../shared/models/page.model'; // Page import edildi
// import { NotificationService } from '../../../core/services/notification.service';
import { PaginatorComponent } from '../../../shared/components/paginator/paginator.component'; // Aktif edildi

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, RouterModule, PaginatorComponent], // PaginatorComponent eklendi
  templateUrl: './order-list.component.html',
  styleUrls: ['./order-list.component.scss']
})
export class OrderListComponent implements OnInit {
  private orderService = inject(OrderService);
  // private notificationService = inject(NotificationService);

  ordersResponse = signal<Page<OrderResponseDto> | null>(null); // orders -> ordersResponse, Order[] -> Page<OrderResponseDto> | null
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  // Sayfalama için
  currentPage = signal(0);
  pageSize = signal(10); // Varsayılan sayfa boyutu
  // totalElements signal'i ordersResponse.totalElements'ten okunabilir, ayrıca tutmaya gerek yok.
  // totalPages signal'i ordersResponse.totalPages'ten okunabilir.

  ngOnInit(): void {
    this.loadOrders(this.currentPage());
  }

  loadOrders(page: number = 0): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.currentPage.set(page);

    this.orderService.getMyOrders(this.currentPage(), this.pageSize()).subscribe({
      next: (response: Page<OrderResponseDto>) => {
        this.ordersResponse.set(response);
        this.isLoading.set(false);
      },
      error: (err: any) => { // err tip eklendi
        this.isLoading.set(false);
        this.errorMessage.set('Failed to load orders. Please try again.');
        console.error('Error loading orders:', err);
        // this.notificationService.showError('Failed to load orders.');
      }
    });
  }

  onPageChange(page: number): void {
    this.loadOrders(page);
  }
}
