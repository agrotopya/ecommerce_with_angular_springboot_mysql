import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router'; // ActivatedRoute ve RouterModule eklendi

@Component({
  selector: 'app-order-success',
  standalone: true,
  imports: [CommonModule, RouterModule], // RouterModule eklendi
  templateUrl: './order-success.component.html',
  styleUrls: ['./order-success.component.scss'] // styleUrl -> styleUrls
})
export class OrderSuccessComponent implements OnInit {
  private activatedRoute = inject(ActivatedRoute);

  orderId = signal<string | null>(null);
  sessionId = signal<string | null>(null);
  // Opsiyonel: Sipariş detaylarını çekmek için
  // private orderService = inject(OrderService);
  // orderDetails = signal<OrderResponseDto | null>(null);
  // isLoading = signal<boolean>(false);
  // errorMessage = signal<string | null>(null);

  constructor() {}

  ngOnInit(): void {
    this.activatedRoute.queryParamMap.subscribe(params => {
      this.orderId.set(params.get('order_id'));
      this.sessionId.set(params.get('session_id'));

      // Opsiyonel: orderId varsa sipariş detaylarını çek
      // const currentOrderId = this.orderId();
      // if (currentOrderId) {
      //   this.loadOrderDetails(currentOrderId);
      // }
    });
  }

  // Opsiyonel: Sipariş detaylarını yükleme fonksiyonu
  // loadOrderDetails(orderId: string): void {
  //   this.isLoading.set(true);
  //   this.errorMessage.set(null);
  //   this.orderService.findMyOrderById(Number(orderId)).subscribe({
  //     next: (data) => {
  //       this.orderDetails.set(data);
  //       this.isLoading.set(false);
  //     },
  //     error: (err) => {
  //       this.errorMessage.set('Failed to load order details.');
  //       this.isLoading.set(false);
  //       console.error('Error loading order details on success page:', err);
  //     }
  //   });
  // }
}
