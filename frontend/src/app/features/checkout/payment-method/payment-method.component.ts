// src/app/features/checkout/payment-method/payment-method.component.ts
import { Component, EventEmitter, OnInit, Output, inject, ViewChild, ElementRef, signal, AfterViewInit } from '@angular/core'; // AfterViewInit eklendi
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { environment } from '../../../../environments/environment'; // Environment import

declare var Stripe: any; // Stripe'ı global olarak tanımla

// Ödeme yöntemi modeli (ileride shared/models/payment.model.ts'e taşınabilir)
export interface PaymentMethod {
  type: 'credit_card' | 'bank_transfer' | 'cash_on_delivery' | 'STRIPE'; // 'STRIPE' eklendi
  cardNumber?: string; // Stripe kullanılıyorsa bu alanlara gerek kalmayabilir
  cardHolder?: string; // Stripe kullanılıyorsa bu alanlara gerek kalmayabilir
  expiryMonth?: string; // Stripe kullanılıyorsa bu alanlara gerek kalmayabilir
  expiryYear?: string; // Stripe kullanılıyorsa bu alanlara gerek kalmayabilir
  cvv?: string; // Stripe kullanılıyorsa bu alanlara gerek kalmayabilir
  stripePaymentMethodId?: string; // Stripe PaymentMethod ID'si
}

@Component({
  selector: 'app-payment-method',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './payment-method.component.html',
  styleUrls: ['./payment-method.component.scss']
})
export class PaymentMethodComponent implements OnInit { // AfterViewInit kaldırıldı
  private fb = inject(FormBuilder);

  @Output() paymentMethodSubmitted = new EventEmitter<PaymentMethod>();
  @Output() backClicked = new EventEmitter<void>();

  paymentForm!: FormGroup;

  // Stripe ile ilgili alanlar kaldırıldı (ViewChild, stripe, elements, card, stripeError, isProcessing)
  // isProcessing sinyali kalabilir, genel bir yükleme durumu için.
  isProcessing = signal(false);

  // Ödeme yöntemi seçenekleri
  paymentTypes = [
    { id: 'STRIPE', name: 'Credit / Debit Card (Stripe)' }, // 'credit_card' -> 'STRIPE' olarak değiştirildi ve adı güncellendi
    { id: 'bank_transfer', name: 'Bank Transfer' },
    { id: 'cash_on_delivery', name: 'Cash on Delivery' }
  ];

  // Ay ve yıl seçenekleri (Stripe Card Element kullanılmayacağı için kaldırıldı)
  months = Array.from({ length: 12 }, (_, i) => {
    const month = i + 1;
    return { value: month.toString().padStart(2, '0'), label: month.toString().padStart(2, '0') };
  });

  years = Array.from({ length: 10 }, (_, i) => {
    const year = new Date().getFullYear() + i;
    return { value: year.toString(), label: year.toString() };
  });

  ngOnInit(): void {
    this.initForm();
    // Stripe Element kurulumu kaldırıldı
  }

  // ngAfterViewInit ve setupStripeElement kaldırıldı

  private initForm(): void {
    this.paymentForm = this.fb.group({
      type: ['STRIPE', Validators.required] // Varsayılan 'STRIPE'
      // cardDetails grubu kaldırıldı
    });

    // Ödeme yöntemi değiştiğinde validasyon güncelleme mantığı kaldırıldı
  }

  onSubmit(): void { // async kaldırıldı
    this.isProcessing.set(true);
    // this.stripeError.set(null); // stripeError kaldırıldı

    if (this.paymentForm.valid) {
      const paymentType = this.paymentForm.get('type')?.value;
      this.paymentMethodSubmitted.emit({ type: paymentType });
    } else {
      this.markFormGroupTouched(this.paymentForm);
    }
    this.isProcessing.set(false);
  }

  onBack(): void {
    this.backClicked.emit();
  }

  // Form kontrollerinin durumunu kontrol etmek için yardımcı metodlar
  isFieldInvalid(formGroup: any, fieldName: string): boolean {
    const control = formGroup.get(fieldName);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  getErrorMessage(formGroup: any, fieldName: string): string {
    const control = formGroup.get(fieldName);
    if (!control) return '';

    if (control.errors?.['required']) {
      return 'This field is required';
    }

    if (control.errors?.['pattern']) {
      switch (fieldName) {
        case 'cardNumber':
          return 'Please enter a valid 16-digit card number';
        case 'cvv':
          return 'Please enter a valid 3 or 4 digit CVV';
        default:
          return 'Invalid format';
      }
    }

    return 'Invalid value';
  }

  // Tüm form kontrollerini dokunulmuş olarak işaretle
  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();

      if ((control as FormGroup).controls) {
        this.markFormGroupTouched(control as FormGroup);
      }
    });
  }

  // Kredi kartı numarasını formatlama (4 haneli gruplar halinde)
  formatCardNumber(event: any): void {
    const input = event.target;
    let value = input.value.replace(/\D/g, '');
    if (value.length > 16) {
      value = value.substring(0, 16);
    }
    input.value = value;
  }
}
