import { Component, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { CommonModule, DatePipe, CurrencyPipe } from '@angular/common';
import { HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router'; // RouterModule eklendi
import { CouponService } from '../../services/coupon.service';
import { CouponResponse, CouponRequest } from '../../models/coupon.model';
import { Page } from '../../../../shared/models/page.model';
import { NotificationService } from '../../../../core/services/notification.service';
import { DiscountType } from '../../../../shared/enums/discount-type.enum'; // DiscountType enum import edildi

@Component({
  selector: 'app-admin-coupon-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, DatePipe, CurrencyPipe], // RouterModule, DatePipe, CurrencyPipe eklendi
  templateUrl: './admin-coupon-list.component.html',
  styleUrls: ['./admin-coupon-list.component.scss'] // styleUrl -> styleUrls
})
export class AdminCouponListComponent implements OnInit {
  private couponService = inject(CouponService);
  private notificationService = inject(NotificationService);
  private router = inject(Router); // Router inject edildi

  couponsResponse: WritableSignal<Page<CouponResponse> | null> = signal(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  // Filtreleme ve Sayfalama
  currentPage = signal(0);
  pageSize = signal(10);
  currentSort = signal('createdAt,desc'); // Varsayılan sıralama
  currentFilterActive = signal<boolean | undefined>(undefined); // true: active, false: inactive, undefined: all

  filterOptions = [
    { label: 'All Coupons', value: undefined },
    { label: 'Active Coupons', value: true },
    { label: 'Inactive Coupons', value: false }
  ];
  selectedFilter: string = ''; // ngModel için

  ngOnInit(): void {
    this.loadCoupons();
  }

  loadCoupons(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.couponService.getCoupons(
      this.currentPage(),
      this.pageSize(),
      this.currentSort(),
      this.currentFilterActive()
    ).subscribe({
      next: (response) => {
        console.log('AdminCouponList: Raw coupons response from service:', response);
        if (response && response.content) {
          response.content.forEach(coupon => console.log(`AdminCouponList: Coupon ID ${coupon.id}, Code: ${coupon.code}, active: ${coupon.active}`));
        }
        this.couponsResponse.set(response);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Failed to load coupons.');
        this.notificationService.showError(this.errorMessage()!);
        this.isLoading.set(false);
        console.error('Error loading coupons for admin:', err);
      }
    });
  }

  onPageChange(page: number): void {
    if (page >= 0 && page < (this.couponsResponse()?.totalPages ?? 0)) {
      this.currentPage.set(page);
      this.loadCoupons();
    }
  }

  onFilterChange(): void {
    this.currentPage.set(0);
    if (this.selectedFilter === 'true') {
      this.currentFilterActive.set(true);
    } else if (this.selectedFilter === 'false') {
      this.currentFilterActive.set(false);
    } else {
      this.currentFilterActive.set(undefined);
    }
    this.loadCoupons();
  }

  editCoupon(couponId: number): void {
    this.router.navigate(['/admin/coupon-management/edit', couponId]);
  }

  deleteCoupon(couponId: number, couponCode: string): void {
    if (confirm(`Are you sure you want to delete coupon "${couponCode}" (ID: ${couponId})? This action cannot be undone.`)) {
      this.couponService.deleteCoupon(couponId).subscribe({
        next: () => {
          this.notificationService.showSuccess(`Coupon "${couponCode}" deleted successfully.`);
          this.loadCoupons(); // Listeyi yenile
        },
        error: (err) => {
          this.notificationService.showError('Failed to delete coupon.');
          console.error('Error deleting coupon:', err);
        }
      });
    }
  }

  // Kuponun aktif/pasif durumunu değiştirmek için
  toggleCouponStatus(coupon: CouponResponse): void {
    const newStatus = !coupon.active;
    const action = newStatus ? 'activate' : 'deactivate';
    if (confirm(`Are you sure you want to ${action} coupon "${coupon.code}"?`)) {
      // updateCoupon metodu CouponRequest bekliyor. Sadece isActive'i değiştirmek için
      // tüm kupon bilgilerini göndermemiz gerekebilir veya backend'de sadece status update için
      // ayrı bir PATCH endpoint'i olabilir. Şimdilik tüm bilgileri gönderiyoruz.
      const updateRequest: CouponRequest = {
        code: coupon.code,
        description: coupon.description,
        discountType: coupon.discountType,
        discountValue: coupon.discountValue,
        expiryDate: coupon.expiryDate, // Bu string formatında olmalı, backend'in beklediği gibi
        minPurchaseAmount: coupon.minPurchaseAmount,
        isActive: newStatus, // Burası CouponRequest'e göre isActive kalmalı
        usageLimit: coupon.usageLimit
      };
      this.couponService.updateCoupon(coupon.id, updateRequest).subscribe({
        next: () => {
          this.notificationService.showSuccess(`Coupon "${coupon.code}" ${action}d successfully.`);
          this.loadCoupons();
        },
        error: (err) => {
          this.notificationService.showError(`Failed to ${action} coupon.`);
          console.error(`Error ${action}ing coupon:`, err);
        }
      });
    }
  }

  getDiscountTypeLabel(type: DiscountType): string {
    switch (type) {
      case DiscountType.PERCENTAGE: return 'Percentage';
      case DiscountType.FIXED_AMOUNT: return 'Fixed Amount';
      default: return type;
    }
  }

  get pageNumbers(): number[] {
    const totalPages = this.couponsResponse()?.totalPages ?? 0;
    return Array(totalPages).fill(0).map((x, i) => i);
  }
}
