// src/app/features/orders/order-detail/order-detail.component.ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { OrderService } from '../order.service';
import { OrderResponseDto, OrderItemResponseDto } from '../../../shared/models/order.model'; // OrderItemDto -> OrderItemResponseDto
import { OrderStatus } from '../../../shared/enums/order-status.enum';
import { CreateCheckoutSessionRequest } from '../../../shared/models/payment.model';
import { environment } from '../../../../environments/environment';
// import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.scss']
})
export class OrderDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private orderService = inject(OrderService);
  // private notificationService = inject(NotificationService);

  order = signal<OrderResponseDto | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  isRetryingPayment = signal(false);
  retryPaymentErrorMessage = signal<string | null>(null);

  public OrderStatusEnum = OrderStatus; // Enum'u template'te kullanmak için

  private router = inject(Router);

  ngOnInit(): void {
    const orderIdParam = this.route.snapshot.paramMap.get('id');
    if (orderIdParam) {
      const orderId = +orderIdParam;
      this.loadOrderDetail(orderId);
    } else {
      this.isLoading.set(false);
      this.errorMessage.set('Order ID not found.');
      console.error('Order ID is missing from route parameters.');
    }

    // Ödeme iptalinden sonra gelen query parametresini kontrol et
    this.route.queryParamMap.subscribe(params => {
      if (params.get('payment_status') === 'cancelled_retry') {
        this.retryPaymentErrorMessage.set('Your payment attempt was cancelled. You can try again.');
        // URL'den query parametresini temizle
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { payment_status: null },
          queryParamsHandling: 'merge',
          replaceUrl: true
        });
      }
    });
  }

  loadOrderDetail(orderId: number): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.orderService.getMyOrderDetail(orderId).subscribe({
      next: (data) => {
        this.order.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set('Failed to load order details. Please try again.');
        console.error(`Error loading order details for ID ${orderId}:`, err);
        // this.notificationService.showError('Failed to load order details.');
      }
    });
  }

  // Siparişi iptal etme fonksiyonu (ileride eklenecek)
  // cancelOrder(): void {
  //   const currentOrder = this.order();
  //   if (currentOrder && confirm('Are you sure you want to cancel this order?')) {
  //     this.isLoading.set(true);
  //     this.orderService.cancelOrder(currentOrder.id).subscribe({
  //       next: (updatedOrder) => {
  //         this.order.set(updatedOrder);
  //         this.isLoading.set(false);
  //         // this.notificationService.showSuccess('Order cancelled successfully.');
  //       },
  //       error: (err) => {
  //         this.isLoading.set(false);
  //         this.errorMessage.set('Failed to cancel order.');
  //         console.error('Error cancelling order:', err);
  //         // this.notificationService.showError('Failed to cancel order.');
  //       }
  //     });
  //   }
  // }

  get canRetryPayment(): boolean {
    const currentOrder = this.order();
    if (!currentOrder) {
      return false;
    }
    // Backend'deki enum string değerleriyle eşleşmeli
    const isPendingPayment = currentOrder.status === 'PENDING_PAYMENT';
    const isPaymentFailed = currentOrder.paymentStatus === 'FAILED'; // PaymentStatus enum'una göre
    return isPendingPayment || isPaymentFailed;
  }

  retryPayment(): void {
    const currentOrder = this.order();
    if (!currentOrder || !currentOrder.id) {
      this.retryPaymentErrorMessage.set('Order information is missing to retry payment.');
      return;
    }

    this.isRetryingPayment.set(true);
    this.retryPaymentErrorMessage.set(null);

    const baseUrl = window.location.origin;
    const checkoutSessionRequest: CreateCheckoutSessionRequest = {
      orderId: currentOrder.id,
      successUrl: `${baseUrl}/orders/success?session_id={CHECKOUT_SESSION_ID}&order_id=${currentOrder.id}`,
      // İptal durumunda kullanıcıyı sipariş detayına geri yönlendirebiliriz veya checkout'a
      cancelUrl: `${baseUrl}/profile/orders/${currentOrder.id}?payment_status=cancelled_retry`,
    };

    this.orderService.createCheckoutSession(checkoutSessionRequest).subscribe({
      next: (sessionResponse) => {
        this.isRetryingPayment.set(false);
        if (sessionResponse.checkoutUrl) {
          window.location.href = sessionResponse.checkoutUrl;
        } else if (sessionResponse.sessionId) {
          const stripe = (window as any).Stripe(environment.stripePublishableKey);
          if (stripe) {
            stripe.redirectToCheckout({ sessionId: sessionResponse.sessionId }).then((result: any) => {
              if (result.error) {
                this.retryPaymentErrorMessage.set(result.error.message || 'Failed to redirect to Stripe.');
                console.error('Error redirecting to Stripe Checkout from order detail:', result.error);
              }
            });
          } else {
            this.retryPaymentErrorMessage.set('Stripe.js is not available to redirect.');
            console.error('Stripe.js not found for redirectToCheckout from order detail.');
          }
        } else {
          this.retryPaymentErrorMessage.set('Failed to get payment session details. Please contact support.');
          console.error('Neither checkoutUrl nor sessionId provided in sessionResponse from order detail.');
        }
      },
      error: (sessionError) => {
        this.isRetryingPayment.set(false);
        this.retryPaymentErrorMessage.set(sessionError.error?.message || 'Failed to create payment session. Please try again.');
        console.error('Error creating Stripe Checkout session from order detail:', sessionError);
      }
    });
  }

  // ngOnInit içinde payment_status=cancelled_retry kontrolü eklenebilir
  // ngOnInit(): void {
  //   // ... mevcut kod ...
  //   this.route.queryParamMap.subscribe(params => {
  //     if (params.get('payment_status') === 'cancelled_retry') {
  //       this.retryPaymentErrorMessage.set('Payment was cancelled. You can try again.');
  //       // URL'den query parametresini temizle
  //       this.router.navigate([], {
  //         relativeTo: this.route,
  //         queryParams: { payment_status: null },
  //         queryParamsHandling: 'merge',
  //         replaceUrl: true
  //       });
  //     }
  //   });
  // }
  // Not: ngOnInit'i iki kere tanımlayamayız, mevcut ngOnInit'e eklenmeli.
  // Şimdilik bu kısmı yorumda bırakıyorum, cancelUrl'den dönüldüğünde mesaj gösterme
  // ihtiyacı olursa mevcut ngOnInit'e entegre edilebilir.
}
