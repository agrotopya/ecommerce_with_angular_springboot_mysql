// src/app/features/checkout/checkout-page/checkout-page.component.ts
import { Component, inject, signal, OnInit } from '@angular/core'; // OnInit eklendi
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router'; // ActivatedRoute eklendi
import { FormsModule } from '@angular/forms'; // FormsModule eklendi
import { CartService } from '../../cart/cart.service';
import { Cart } from '../../../shared/models/cart.model';
import { ShippingAddressComponent, ShippingAddress } from '../shipping-address/shipping-address.component';
import { OrderSummaryComponent } from '../order-summary/order-summary.component';
import { PaymentMethodComponent, PaymentMethod } from '../payment-method/payment-method.component';
import { OrderService } from '../../orders/order.service'; // OrderService eklendi
import { CreateOrderRequestDto } from '../../../shared/models/order.model'; // OrderRequest -> CreateOrderRequestDto
import { AddressDto } from '../../../shared/models/address.model'; // Address -> AddressDto
import { CreateCheckoutSessionRequest } from '../../../shared/models/payment.model'; // CreateCheckoutSessionRequest eklendi
import { environment } from '../../../../environments/environment'; // environment importu eklendi
// import { NotificationService } from '../../../core/services/notification.service'; // İleride eklenecek

@Component({
  selector: 'app-checkout-page',
  standalone: true,
  imports: [CommonModule, RouterModule, ShippingAddressComponent, OrderSummaryComponent, PaymentMethodComponent, FormsModule], // FormsModule eklendi
  templateUrl: './checkout-page.component.html',
  styleUrls: ['./checkout-page.component.scss']
})
export class CheckoutPageComponent implements OnInit { // OnInit implement edildi
  private cartService = inject(CartService);
  private orderService = inject(OrderService); // OrderService inject edildi
  private router = inject(Router); // Router inject edildi
  private activatedRoute = inject(ActivatedRoute); // ActivatedRoute inject edildi
  // private notificationService = inject(NotificationService); // İleride

  cart = this.cartService.cart;
  isLoading = signal(false);
  isPlacingOrder = signal(false); // Sipariş oluşturma durumu için
  errorMessage = signal<string | null>(null);

  // Checkout adımları için durum yönetimi
  currentStep = signal(1); // 1: Adres, 2: Ödeme, 3: Onay
  shippingAddress: AddressDto | null = null; // Address -> AddressDto
  paymentMethod: PaymentMethod | null = null;
  couponCode: string | null = null; // Kupon kodu için alan eklendi


  constructor() {
    // Sepet boşsa ana sayfaya yönlendirme yapılabilir
    // Sepet verilerini yeniden yükleme
    // ngOnInit'e taşındı: this.loadCart();
  }

  ngOnInit(): void {
    this.loadCart();
    this.activatedRoute.queryParamMap.subscribe(params => {
      if (params.get('status') === 'cancelled') {
        this.errorMessage.set('Ödemeniz iptal edildi veya bir sorun nedeniyle tamamlanamadı. Lütfen sepetinizi kontrol edip tekrar deneyin.');
        this.currentStep.set(3); // Kullanıcıyı sipariş özeti/onay adımına geri döndür
        // URL'den query parametresini temizle, böylece yenilemede mesaj tekrar çıkmaz
        this.router.navigate([], {
          relativeTo: this.activatedRoute,
          queryParams: { status: null },
          queryParamsHandling: 'merge', // Diğer query parametrelerini koru (varsa)
          replaceUrl: true // Tarayıcı geçmişinde yeni bir kayıt oluşturma
        });
      }
    });
  }

  // OrderSummaryComponent'ten gelen kupon kodunu işle
  handleCouponApplied(appliedCouponCode: string | null): void {
    this.couponCode = appliedCouponCode;
    // İsteğe bağlı: Kupon uygulandığında kullanıcıya bir bildirim gösterilebilir
    // this.notificationService.showInfo(`Coupon '${appliedCouponCode}' will be applied.`);
    // Ya da sadece CheckoutPageComponent içinde saklayıp createOrder'da kullanırız.
    console.log('Coupon code to be used in order:', this.couponCode);
  }

  private loadCart(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.cartService.loadCart().subscribe({
      next: () => {
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set('Failed to load cart data. Please try again.');
        console.error('Error loading cart in checkout:', err);
      }
    });
  }

  // Adım değiştirme işlevi (ileride geliştirilecek)
  goToStep(step: number): void {
    // Adım geçişlerinde validasyon kontrolleri yapılabilir
    this.currentStep.set(step);
  }

  // Teslimat adresi işleme
  handleAddressSubmitted(address: ShippingAddress): void {
    console.log('Shipping address submitted:', address);
    // ShippingAddress'i AddressDto tipine dönüştür
    this.shippingAddress = {
      fullName: address.fullName,
      street: address.addressLine1, // addressLine1 -> street olarak güncellendi
      addressLine2: address.addressLine2,
      city: address.city,
      state: address.state,
      zipCode: address.postalCode,
      country: address.country,
      phoneNumber: address.phone
      // addressTitle: 'Shipping' // Opsiyonel
    };
    this.goToStep(2); // Bir sonraki adıma geç
  }

  // Ödeme yöntemi işleme
  handlePaymentMethodSubmitted(paymentMethod: PaymentMethod): void {
    console.log('Payment method submitted:', paymentMethod);
    this.paymentMethod = paymentMethod; // Ödeme yöntemini sakla
    this.goToStep(3); // Bir sonraki adıma geç
  }

  // Sipariş oluşturma işlevi
  createOrder(): void {
    if (!this.shippingAddress || !this.paymentMethod || !this.cart()) {
      this.errorMessage.set('Missing order information. Please complete all steps.');
      console.error('Checkout Error: Missing shippingAddress, paymentMethod, or cart.');
      return;
    }

    this.isPlacingOrder.set(true);
    this.errorMessage.set(null);

    const currentCart = this.cart();
    if (!currentCart || !currentCart.items || currentCart.items.length === 0) {
      this.errorMessage.set('Your cart is empty.');
      this.isPlacingOrder.set(false);
      return;
    }

    const orderRequest: CreateOrderRequestDto = {
      shippingAddress: this.shippingAddress!,
      billingAddress: this.shippingAddress!, // Şimdilik aynı adres, gerekirse ayrı bir formdan alınabilir
      paymentMethod: this.paymentMethod!.type, // paymentDetails kaldırıldı, doğrudan paymentMethod kullanılıyor
      cartItems: currentCart.items.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.productPrice // Sipariş anındaki fiyat
      })),
      totalAmount: currentCart.cartSubtotal, // Sepet toplamı (indirimler hariç)
      couponCode: this.couponCode || undefined, // Kupon kodunu ekle
      notes: undefined // Notlar için ayrı bir alan varsa kullanılabilir
    };

    this.orderService.createOrder(orderRequest).subscribe({
      next: (createdOrder) => {
        console.log('Order created successfully:', createdOrder);
        this.cartService.clearCart().subscribe();

        if (this.paymentMethod?.type === 'credit_card' || this.paymentMethod?.type === 'STRIPE') {
          const baseUrl = window.location.origin; // Örneğin: http://localhost:4200
          const checkoutSessionRequest: CreateCheckoutSessionRequest = {
            orderId: createdOrder.id,
            successUrl: `${baseUrl}/orders/success?session_id={CHECKOUT_SESSION_ID}&order_id=${createdOrder.id}`, // order_id eklendi
            cancelUrl: `${baseUrl}/checkout?status=cancelled`, // status=cancelled query param eklendi
          };
          this.orderService.createCheckoutSession(checkoutSessionRequest).subscribe({
            next: (sessionResponse) => {
              console.log('Stripe Checkout session response:', sessionResponse);
              this.isPlacingOrder.set(false);
              if (sessionResponse.checkoutUrl) {
                window.location.href = sessionResponse.checkoutUrl;
              } else if (sessionResponse.sessionId) {
                // Stripe.js'in PaymentMethodComponent'te initialize edildiğini varsayıyoruz
                // ve global Stripe nesnesine erişebildiğimizi.
                // Eğer Stripe nesnesi burada yoksa, PaymentMethodComponent'ten bir şekilde alınmalı
                // veya burada tekrar initialize edilmeli.
                // En temizi, Stripe'ı bir servis olarak sarmalamak olabilir.
                // Şimdilik global Stripe'ı kullanmayı deneyelim.
                const stripe = (window as any).Stripe(environment.stripePublishableKey);
                if (stripe) {
                  stripe.redirectToCheckout({ sessionId: sessionResponse.sessionId }).then((result: any) => {
                    if (result.error) {
                      this.errorMessage.set(result.error.message);
                      console.error('Error redirecting to Stripe Checkout:', result.error);
                    }
                  });
                } else {
                  this.errorMessage.set('Stripe.js is not available to redirect.');
                  console.error('Stripe.js not found for redirectToCheckout.');
                }
              } else {
                this.errorMessage.set('Failed to get payment session details. Please contact support.');
                console.error('Neither checkoutUrl nor sessionId provided in sessionResponse.');
              }
            },
            error: (sessionError) => {
              this.isPlacingOrder.set(false);
              this.errorMessage.set('Failed to create payment session. Please try again or contact support.');
              console.error('Error creating Stripe Checkout session:', sessionError);
            }
          });
        } else {
          this.isPlacingOrder.set(false);
          this.router.navigate(['/orders/success', createdOrder.id]);
        }
      },
      error: (err) => {
        this.isPlacingOrder.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to place order. Please try again.');
        console.error('Error creating order:', err);
      }
    });
  }
}
