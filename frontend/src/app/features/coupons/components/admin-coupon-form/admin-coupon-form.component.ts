import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common'; // DatePipe eklendi
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router'; // RouterModule eklendi
import { CouponService } from '../../services/coupon.service';
import { CouponRequest, CouponResponse } from '../../models/coupon.model';
import { NotificationService } from '../../../../core/services/notification.service';
import { DiscountType } from '../../../../shared/enums/discount-type.enum';

@Component({
  selector: 'app-admin-coupon-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, DatePipe], // RouterModule ve DatePipe eklendi
  templateUrl: './admin-coupon-form.component.html',
  styleUrls: ['./admin-coupon-form.component.scss'] // styleUrl -> styleUrls
})
export class AdminCouponFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private couponService = inject(CouponService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  couponForm!: FormGroup;
  isEditMode = signal(false);
  couponId = signal<number | null>(null);
  isLoading = signal(false);
  pageTitle = signal('Add New Coupon');

  // DiscountType enum'ını template'te kullanmak için
  discountTypeValues = Object.values(DiscountType);

  constructor() {
    this.couponForm = this.fb.group({
      code: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      description: [''],
      discountType: [DiscountType.PERCENTAGE, Validators.required],
      discountValue: [null, [Validators.required, Validators.min(0.01)]],
      expiryDate: ['', Validators.required],
      minPurchaseAmount: [0, [Validators.min(0)]],
      isActive: [true],
      usageLimit: [null, [Validators.min(0)]]
    });
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode.set(true);
      this.couponId.set(+idParam);
      this.pageTitle.set('Edit Coupon');
      this.loadCouponForEdit();
    }
  }

  loadCouponForEdit(): void {
    const id = this.couponId();
    if (!id) return;

    this.isLoading.set(true);
    this.couponService.getCouponById(id).subscribe({
      next: (coupon) => {
        // Tarihi YYYY-MM-DDTHH:mm formatına çevir (datetime-local input için)
        const expiryDate = coupon.expiryDate ? new Date(coupon.expiryDate).toISOString().substring(0, 16) : '';
        this.couponForm.patchValue({
          ...coupon,
          expiryDate: expiryDate
        });
        this.isLoading.set(false);
      },
      error: (err) => {
        this.notificationService.showError('Failed to load coupon details.');
        this.isLoading.set(false);
        console.error('Error loading coupon for edit:', err);
        this.router.navigate(['/admin/coupon-management']);
      }
    });
  }

  onSubmit(): void {
    if (this.couponForm.invalid) {
      this.notificationService.showError('Please fill in all required fields correctly.');
      this.couponForm.markAllAsTouched(); // Tüm alanları dokunulmuş olarak işaretle
      return;
    }

    this.isLoading.set(true);
    const formValue = this.couponForm.value;

    // expiryDate'i backend'in beklediği ISO string formatına dönüştür
    const expiryDateTime = new Date(formValue.expiryDate).toISOString();

    const couponRequest: CouponRequest = {
      ...formValue,
      expiryDate: expiryDateTime,
      // usageLimit null değilse number'a çevir, aksi halde null bırak
      usageLimit: formValue.usageLimit !== null && formValue.usageLimit !== '' ? Number(formValue.usageLimit) : null
    };

    if (this.isEditMode() && this.couponId()) {
      this.couponService.updateCoupon(this.couponId()!, couponRequest).subscribe({
        next: () => {
          this.notificationService.showSuccess('Coupon updated successfully.');
          this.router.navigate(['/admin/coupon-management']);
        },
        error: (err) => {
          this.notificationService.showError('Failed to update coupon.');
          this.isLoading.set(false);
          console.error('Error updating coupon:', err);
        }
      });
    } else {
      this.couponService.createCoupon(couponRequest).subscribe({
        next: () => {
          this.notificationService.showSuccess('Coupon created successfully.');
          this.router.navigate(['/admin/coupon-management']);
        },
        error: (err) => {
          this.notificationService.showError('Failed to create coupon.');
          this.isLoading.set(false);
          console.error('Error creating coupon:', err);
        }
      });
    }
  }

  // Helper for template to get form controls
  get f() { return this.couponForm.controls; }
}
