// src/app/features/checkout/order-summary/order-summary.component.ts
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms'; // FormControl ve ReactiveFormsModule eklendi
import { Cart } from '../../../shared/models/cart.model';
import { CouponService } from '../../coupons/services/coupon.service'; // CouponService eklendi
import { CouponValidationResponse } from '../../coupons/models/coupon.model'; // Model eklendi

@Component({
  selector: 'app-order-summary',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule], // ReactiveFormsModule eklendi
  templateUrl: './order-summary.component.html',
  styleUrls: ['./order-summary.component.scss']
})
export class OrderSummaryComponent {
  @Input() cart!: Cart;
  @Input() showActions = true;
  @Input() shippingCost = 0;
  @Input() isPlacingOrder = false;

  @Output() backClicked = new EventEmitter<void>();
  @Output() placeOrderClicked = new EventEmitter<void>();
  @Output() couponApplied = new EventEmitter<string | null>(); // Kupon kodu veya null emit eder

  private couponService = inject(CouponService);

  couponCodeControl = new FormControl('');
  couponValidationMessage = signal<string | null>(null);
  calculatedDiscount = signal<number>(0);
  isApplyingCoupon = signal<boolean>(false);

  // Ara toplam (indirimler hariç, kargo dahil değil)
  get subTotal(): number {
    return this.cart?.cartSubtotal || 0;
  }

  // İndirim sonrası ara toplam
  get subTotalAfterDiscount(): number {
    return this.subTotal - this.calculatedDiscount();
  }

  // Kargo dahil, indirimli genel toplam
  get grandTotal(): number {
    if (!this.cart) return this.shippingCost; // Sepet yoksa sadece kargo (veya 0)
    return this.subTotalAfterDiscount + this.shippingCost;
  }

  applyCoupon(): void {
    const code = this.couponCodeControl.value?.trim();
    if (!code) {
      this.couponValidationMessage.set('Please enter a coupon code.');
      return;
    }

    if (!this.cart || this.cart.cartSubtotal === undefined) {
      this.couponValidationMessage.set('Cart total is not available to validate coupon.');
      return;
    }

    this.isApplyingCoupon.set(true);
    this.couponValidationMessage.set(null);
    // calculatedDiscount'ı sıfırlamıyoruz, kullanıcı yeni kupon girene kadar eski indirim görünsün.
    // Eğer yeni kupon geçersizse veya boşsa o zaman sıfırlanır.

    this.couponService.validateCoupon(code, this.cart.cartSubtotal).subscribe({
      next: (response: CouponValidationResponse) => {
        this.couponValidationMessage.set(response.message);
        if (response.valid && response.discountAmountCalculated !== undefined) {
          this.calculatedDiscount.set(response.discountAmountCalculated);
          this.couponApplied.emit(code); // Geçerli kupon kodunu üst bileşene gönder
        } else {
          this.calculatedDiscount.set(0); // Kupon geçersizse indirimi sıfırla
          this.couponApplied.emit(null); // Geçersiz veya boş kuponu null olarak bildir
        }
        this.isApplyingCoupon.set(false);
      },
      error: (err) => {
        this.couponValidationMessage.set(err.error?.message || 'Failed to validate coupon. Please try again.');
        this.calculatedDiscount.set(0);
        this.couponApplied.emit(null);
        this.isApplyingCoupon.set(false);
        console.error('Error validating coupon:', err);
      }
    });
  }

  removeCoupon(): void {
    this.couponCodeControl.setValue('');
    this.couponValidationMessage.set(null);
    this.calculatedDiscount.set(0);
    this.couponApplied.emit(null); // Kuponun kaldırıldığını bildir
  }

  onBackClick(): void {
    this.backClicked.emit();
  }

  onPlaceOrderClick(): void {
    console.log('OrderSummaryComponent: Place Order button clicked.');
    this.placeOrderClicked.emit();
  }
}
