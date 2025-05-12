// src/app/features/checkout/shipping-address/shipping-address.component.ts
import { Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

// Adres modeli (ileride shared/models/address.model.ts'e taşınabilir)
export interface ShippingAddress {
  fullName: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  phone: string;
}

@Component({
  selector: 'app-shipping-address',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './shipping-address.component.html',
  styleUrls: ['./shipping-address.component.scss']
})
export class ShippingAddressComponent implements OnInit {
  private fb = inject(FormBuilder);

  @Output() addressSubmitted = new EventEmitter<ShippingAddress>();
  @Output() cancelClicked = new EventEmitter<void>();

  addressForm!: FormGroup;

  // Ülke listesi (ileride API'den alınabilir)
  countries = [
    { code: 'TR', name: 'Turkey' },
    { code: 'US', name: 'United States' },
    { code: 'GB', name: 'United Kingdom' },
    { code: 'DE', name: 'Germany' },
    { code: 'FR', name: 'France' }
  ];

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    this.addressForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(3)]],
      addressLine1: ['', Validators.required],
      addressLine2: [''],
      city: ['', Validators.required],
      state: ['', Validators.required],
      postalCode: ['', [Validators.required, Validators.pattern(/^\d{5}(?:[-\s]\d{4})?$/)]],
      country: ['TR', Validators.required],
      phone: ['', [Validators.required, Validators.pattern(/^\+?[0-9\s-]{10,15}$/)]]
    });
  }

  onSubmit(): void {
    if (this.addressForm.valid) {
      this.addressSubmitted.emit(this.addressForm.value);
    } else {
      // Form geçerli değilse tüm kontrolleri dokunulmuş olarak işaretle
      Object.keys(this.addressForm.controls).forEach(key => {
        const control = this.addressForm.get(key);
        control?.markAsTouched();
      });
    }
  }

  onCancel(): void {
    this.cancelClicked.emit();
  }

  // Form kontrollerinin durumunu kontrol etmek için yardımcı metodlar
  isFieldInvalid(fieldName: string): boolean {
    const control = this.addressForm.get(fieldName);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  getErrorMessage(fieldName: string): string {
    const control = this.addressForm.get(fieldName);
    if (!control) return '';

    if (control.errors?.['required']) {
      return 'This field is required';
    }

    if (control.errors?.['minlength']) {
      return `Minimum length is ${control.errors['minlength'].requiredLength} characters`;
    }

    if (control.errors?.['pattern']) {
      switch (fieldName) {
        case 'postalCode':
          return 'Please enter a valid postal code (e.g., 12345 or 12345-6789)';
        case 'phone':
          return 'Please enter a valid phone number';
        default:
          return 'Invalid format';
      }
    }

    return 'Invalid value';
  }
}
