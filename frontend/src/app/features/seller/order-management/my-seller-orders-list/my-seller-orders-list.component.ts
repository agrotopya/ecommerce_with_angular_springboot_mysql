import { Component, inject, OnInit, signal, WritableSignal, computed, PLATFORM_ID, effect } from '@angular/core'; // PLATFORM_ID ve effect eklendi
import { CommonModule, DatePipe, isPlatformBrowser } from '@angular/common'; // isPlatformBrowser eklendi
import { Router, RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { SellerService } from '../../seller.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { Page } from '../../../../shared/models/page.model';
import { OrderResponseDto } from '../../../../shared/models/order.model';
import { OrderStatus } from '../../../../shared/enums/order-status.enum';
import { PaymentStatus } from '../../../../shared/enums/payment-status.enum';
import { FormsModule } from '@angular/forms';
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago.pipe';
import { AuthService } from '../../../../core/services/auth.service'; // AuthService eklendi
import { Role } from '../../../../shared/enums/role.enum'; // Role eklendi (isAdmin kontrolü için, gerekirse)

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
  private authService = inject(AuthService);
  private platformId = inject(PLATFORM_ID); // PLATFORM_ID inject edildi

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
    const options = Object.values(OrderStatus).map(status => ({ label: this.formatOrderStatus(status), value: status }));
    console.log('MySellerOrdersListComponent: orderStatusOptions computed:', options); // YENİ LOG
    return options;
  });

  // Her sipariş için düzenleme verilerini tutacak Map
  editingOrdersData = new Map<number, { selectedStatus?: OrderStatus, trackingNumberInput?: string }>();
  public OrderStatus = OrderStatus;


  constructor() { // constructor eklendi
    if (isPlatformBrowser(this.platformId)) {
      effect(() => {
        const isAuthenticated = this.authService.isAuthenticated();
        const currentUser = this.authService.currentUser();
        console.log('MySellerOrdersListComponent: Auth state changed. isAuthenticated:', isAuthenticated, 'User:', currentUser?.username, 'Roles:', currentUser?.roles);
        if (isAuthenticated && currentUser && this.authService.hasRole(Role.SELLER)) {
          console.log('MySellerOrdersListComponent: User is authenticated as SELLER, loading orders.');
          this.loadOrders();
        } else if (isAuthenticated && currentUser && !this.authService.hasRole(Role.SELLER)) {
          console.warn('MySellerOrdersListComponent: User is authenticated but not a SELLER. Clearing orders and showing error.');
          this.orders.set(null);
          this.errorMessage.set('You do not have permission to view seller orders.');
          this.isLoading.set(false);
        } else if (!isAuthenticated) {
          console.log('MySellerOrdersListComponent: User is not authenticated. Clearing orders.');
          this.orders.set(null); // Kullanıcı çıkış yaptığında veya token yoksa listeyi temizle
          this.isLoading.set(false); // Yükleniyor durumunu kapat
        }
      });
    }
  }

  ngOnInit(): void {
    // Initial load attempt is now handled by the effect in the constructor
    // for browser platform. For SSR, we might not load orders initially
    // or show a placeholder.
    if (isPlatformBrowser(this.platformId)) {
      if (this.authService.isAuthenticated() && this.authService.hasRole(Role.SELLER)) {
        console.log('MySellerOrdersListComponent ngOnInit (Browser): User already authenticated as SELLER, loading orders.');
        // this.loadOrders(); // Effect halledecek, dublike çağrıyı önle
      } else {
        console.log('MySellerOrdersListComponent ngOnInit (Browser): User not authenticated as SELLER or not on browser. Orders will be loaded by effect upon auth change.');
      }
    } else {
      console.log('MySellerOrdersListComponent ngOnInit (Server): Skipping initial order load on server.');
    }
  }

  loadOrders(page: number = this.currentPage()): void {
    if (!isPlatformBrowser(this.platformId)) {
      console.log('MySellerOrdersListComponent loadOrders: Attempted to load orders on server. Skipping.');
      this.isLoading.set(false); // Sunucuda yükleniyor durumunu hemen kapat
      return;
    }
    if (!this.authService.isAuthenticated() || !this.authService.hasRole(Role.SELLER)) {
      console.log('MySellerOrdersListComponent loadOrders: User not authenticated as SELLER. Skipping order load.');
      this.isLoading.set(false);
      // İsteğe bağlı olarak kullanıcıyı login'e yönlendirebilir veya bir mesaj gösterebilirsiniz.
      // this.notificationService.showError('You need to be logged in as a seller to view orders.');
      // this.router.navigate(['/auth/login']);
      return;
    }

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
    const formatted = status.replace(/_/g, ' ').replace(/\w\S*/g, (txt) => txt.charAt(0).toUpperCase() + txt.substring(1).toLowerCase());
    // console.log(`MySellerOrdersListComponent: formatOrderStatus input: ${status}, output: ${formatted}`); // İsteğe bağlı detaylı log
    return formatted;
  }

  formatPaymentStatus(status: PaymentStatus): string {
    if (!status) return '';
    return status.replace(/_/g, ' ').replace(/\w\S*/g, (txt) => txt.charAt(0).toUpperCase() + txt.substring(1).toLowerCase());
  }

  // Sipariş durumunu güncellemek için (ileride MySellerOrderDetailComponent'te daha detaylı olacak)
  // updateStatus(orderId: number, newStatus: OrderStatus): void { ... } // Bu metodun yerine daha kapsamlı handler'lar gelecek

  // AdminOrderListComponent'ten alınan metodlar (SellerService'e uyarlandı)
  getOrderEditData(orderId: number): { selectedStatus?: OrderStatus, trackingNumberInput?: string } {
    if (!this.editingOrdersData.has(orderId)) {
      const order = this.orders()?.content?.find(o => o.id === orderId);
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

    // Satıcıların değiştirebileceği durumları kontrol et (Örn: PROCESSING, SHIPPED)
    // Bu kontrol backend'de de olmalı, ancak frontend'de de kullanıcı deneyimi için yapılabilir.
    const allowedSellerStatuses: OrderStatus[] = [OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED]; // Örnek
    if (!this.authService.hasRole(Role.ADMIN) && !allowedSellerStatuses.includes(newStatus)) {
        this.notificationService.showError(`As a seller, you cannot set the order status to ${this.formatOrderStatus(newStatus)}.`);
        // Seçimi eski haline getir
        const editData = this.getOrderEditData(order.id);
        editData.selectedStatus = order.status;
        return;
    }


    this.isLoading.set(true);
    this.sellerService.updateMySellerOrderStatus(order.id, newStatus).subscribe({
      next: (updatedOrder) => {
        this.notificationService.showSuccess(`Order #${order.id} status updated to ${this.formatOrderStatus(newStatus)}.`);
        const currentOrders = this.orders()?.content || [];
        const index = currentOrders.findIndex(o => o.id === order.id);
        if (index > -1) {
          currentOrders[index] = updatedOrder;
          this.orders.set({ ...this.orders()!, content: [...currentOrders] });
        } else {
          this.loadOrders(this.currentPage());
        }
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.notificationService.showError(err.error?.message || `Failed to update status for order #${order.id}`);
        console.error('Error updating seller order status:', err);
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

    this.isLoading.set(true);
    this.sellerService.addMySellerTrackingNumber(order.id, trackNum).subscribe({
      next: (updatedOrder) => {
        this.notificationService.showSuccess(`Tracking number updated for order #${order.id}.`);
        const currentOrders = this.orders()?.content || [];
        const index = currentOrders.findIndex(o => o.id === order.id);
        if (index > -1) {
          currentOrders[index] = updatedOrder;
          this.orders.set({ ...this.orders()!, content: [...currentOrders] });
          const editData = this.getOrderEditData(order.id);
          editData.trackingNumberInput = updatedOrder.trackingNumber || '';
          editData.selectedStatus = updatedOrder.status; // Durum da değişmiş olabilir
        } else {
          this.loadOrders(this.currentPage());
        }
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.notificationService.showError(err.error?.message || `Failed to add tracking number for order #${order.id}`);
        console.error('Error adding seller tracking number:', err);
      }
    });
  }

  // Helper to check if current user is admin (for *ngIf in template, if needed for some actions)
  isAdmin(): boolean {
    return this.authService.hasRole(Role.ADMIN);
  }
}
