import { Component, inject, OnInit, signal, WritableSignal, computed, PLATFORM_ID } from '@angular/core'; // PLATFORM_ID import edildi
import { CommonModule, DatePipe, CurrencyPipe, isPlatformBrowser } from '@angular/common'; // isPlatformBrowser import edildi
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { SellerService } from '../../seller.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { OrderResponseDto, OrderItemResponseDto } from '../../../../shared/models/order.model';
import { OrderStatus } from '../../../../shared/enums/order-status.enum';
import { PaymentStatus } from '../../../../shared/enums/payment-status.enum'; // PaymentStatus import edildi
import { FormsModule } from '@angular/forms';
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago.pipe';

@Component({
  selector: 'app-my-seller-order-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, DatePipe, CurrencyPipe, TimeAgoPipe],
  templateUrl: './my-seller-order-detail.component.html',
  styleUrls: ['./my-seller-order-detail.component.scss'],
  providers: [DatePipe, CurrencyPipe]
})
export class MySellerOrderDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private sellerService = inject(SellerService);
  private notificationService = inject(NotificationService);
  private platformId = inject(PLATFORM_ID); // PLATFORM_ID inject edildi

  order: WritableSignal<OrderResponseDto | null> = signal(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);
  orderId = signal<number | null>(null);

  selectedStatus: WritableSignal<OrderStatus | null> = signal(null);
  orderStatusOptions = computed(() => {
    return Object.values(OrderStatus).map(status => ({ label: this.formatOrderStatus(status), value: status }));
  });

  // Satıcının güncelleyebileceği durumlar (backend'deki iş mantığına göre ayarlanmalı)
  updatableOrderStatuses: OrderStatus[] = [
    OrderStatus.PROCESSING,
    OrderStatus.SHIPPED,
    OrderStatus.DELIVERED, // Satıcının teslim edildi olarak işaretleyebilmesi için eklendi
    // OrderStatus.CANCELLED_BY_SELLER // İptal için ayrı bir buton/mantık daha uygun olabilir
  ];


  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('orderId');
    if (idParam) {
      this.orderId.set(+idParam);
      this.loadOrderDetail();
    } else {
      this.errorMessage.set('Order ID not found in route.');
      this.isLoading.set(false);
      this.notificationService.showError('Order ID not found.');
    }
  }

  loadOrderDetail(): void {
    const id = this.orderId();
    if (!id) return;

    if (!isPlatformBrowser(this.platformId)) {
      console.log('MySellerOrderDetailComponent: Running on server, skipping API call for order details.');
      this.isLoading.set(false); // Yüklenmeyi durdur, SSR'da veri çekilmeyecek
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.sellerService.getMySellerOrderDetail(id).subscribe({
      next: (response) => {
        this.order.set(response);
        this.selectedStatus.set(response.status); // Mevcut durumu seçili yap
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || `Failed to load order details for ID: ${id}.`);
        this.notificationService.showError(this.errorMessage()!);
        console.error('Error loading seller order detail:', err);
        if (err.status === 404 || err.status === 403) {
          this.router.navigate(['/seller/orders']); // Sipariş bulunamazsa veya yetki yoksa listeye dön
        }
      }
    });
  }

  updateOrderStatus(): void {
    const currentOrder = this.order();
    const newStatus = this.selectedStatus();

    if (!currentOrder || !newStatus || newStatus === currentOrder.status) {
      this.notificationService.showInfo('No status change detected or new status is invalid.');
      return;
    }

    if (confirm(`Are you sure you want to update the status of order #${currentOrder.id} to "${this.formatOrderStatus(newStatus)}"?`)) {
      this.isLoading.set(true);
      this.sellerService.updateMySellerOrderStatus(currentOrder.id, newStatus).subscribe({
        next: (updatedOrder) => {
          this.order.set(updatedOrder);
          this.selectedStatus.set(updatedOrder.status);
          this.isLoading.set(false);
          this.notificationService.showSuccess('Order status updated successfully.');
        },
        error: (err: HttpErrorResponse) => {
          this.isLoading.set(false);
          this.notificationService.showError(err.error?.message || 'Failed to update order status.');
          console.error('Error updating order status:', err);
        }
      });
    } else {
      // Kullanıcı iptal ederse, dropdown'ı eski değere geri al
      this.selectedStatus.set(currentOrder.status);
    }
  }

  formatOrderStatus(status: OrderStatus | null): string {
    if (!status) return 'N/A';
    return status.replace(/_/g, ' ').replace(/\w\S*/g, (txt: string) => txt.charAt(0).toUpperCase() + txt.substring(1).toLowerCase());
  }

  formatPaymentStatus(status: PaymentStatus | null): string { // PaymentStatus için formatlama metodu
    if (!status) return 'N/A';
    return status.replace(/_/g, ' ').replace(/\w\S*/g, (txt: string) => txt.charAt(0).toUpperCase() + txt.substring(1).toLowerCase());
  }

  // Helper to check if a status is updatable by the seller
  isStatusUpdatable(status: OrderStatus): boolean {
    return this.updatableOrderStatuses.includes(status);
  }

  calculateItemTotal(item: OrderItemResponseDto): number {
    return item.quantity * item.unitPrice;
  }
}
