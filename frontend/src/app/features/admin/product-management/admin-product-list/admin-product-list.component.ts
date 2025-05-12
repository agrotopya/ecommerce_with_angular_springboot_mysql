// src/app/features/admin/product-management/admin-product-list/admin-product-list.component.ts
import { Component, inject, OnInit, signal, WritableSignal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // FormsModule eklendi
import { AdminService } from '../../../admin/admin.service';
import { Product } from '../../../../shared/models/product.model';
import { Page } from '../../../../shared/models/page.model';
import { HttpErrorResponse } from '@angular/common/http';
import { NotificationService } from '../../../../core/services/notification.service'; // NotificationService eklendi

@Component({
  selector: 'app-admin-product-list',
  standalone: true,
  imports: [CommonModule, FormsModule], // FormsModule eklendi
  templateUrl: './admin-product-list.component.html',
  styleUrls: ['./admin-product-list.component.scss']
})
export class AdminProductListComponent implements OnInit {
  private adminService = inject(AdminService);
  private notificationService = inject(NotificationService); // NotificationService inject edildi

  productsResponse: WritableSignal<Page<Product> | null> = signal(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  // Filtreleme ve Sayfalama için
  currentPage = signal(0);
  pageSize = signal(10);
  currentSort = signal('createdAt,desc');

  // Template'de [(ngModel)] için
  filterApprovalStatus: string = ''; // 'approved', 'not_approved', '' (all)

  constructor() { }

  ngOnInit(): void {
    this.loadAllProducts();
  }

  loadAllProducts(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    let approvalStatus: boolean | undefined;
    if (this.filterApprovalStatus === 'approved') {
      approvalStatus = true;
    } else if (this.filterApprovalStatus === 'not_approved') {
      approvalStatus = false;
    }

    this.adminService.getAllProductsForAdmin(
      this.currentPage(),
      this.pageSize(),
      this.currentSort(),
      approvalStatus
    ).subscribe({
      next: (response: Page<Product>) => {
        this.productsResponse.set(response);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        const errorMsg = err.error?.message || 'Failed to load products.';
        this.errorMessage.set(errorMsg);
        this.notificationService.showError(errorMsg); // Bildirim eklendi
        this.isLoading.set(false);
        console.error('Error loading products for admin:', err);
      }
    });
  }

  onPageChange(page: number): void {
    if (page >= 0 && page < (this.productsResponse()?.totalPages ?? 0)) {
      this.currentPage.set(page);
      this.loadAllProducts();
    }
  }

  approveProduct(productId: number): void {
    this.isLoading.set(true); // Veya sadece ilgili satır için bir loading state
    this.adminService.approveProduct(productId).subscribe({
      next: () => {
        this.notificationService.showSuccess('Product approved successfully.');
        this.loadAllProducts();
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        const errorMsg = err.error?.message || 'Failed to approve product.';
        this.errorMessage.set(errorMsg);
        this.notificationService.showError(errorMsg);
        console.error('Error approving product:', err);
      }
    });
  }

  rejectProduct(productId: number): void {
    // Reddetme nedeni için bir dialog/prompt eklenebilir.
    // Şimdilik direkt reddediyoruz.
    this.isLoading.set(true); // Veya sadece ilgili satır için bir loading state
    this.adminService.rejectProduct(productId).subscribe({
      next: () => {
        this.notificationService.showSuccess('Product rejected successfully.');
        this.loadAllProducts();
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        const errorMsg = err.error?.message || 'Failed to reject product.';
        this.errorMessage.set(errorMsg);
        this.notificationService.showError(errorMsg);
        console.error('Error rejecting product:', err);
      }
    });
  }

  deleteProduct(productId: number): void {
    if (confirm('Are you sure you want to PERMANENTLY DELETE this product? This action cannot be undone.')) {
      this.isLoading.set(true); // Veya sadece ilgili satır için bir loading state
      this.adminService.deleteProductByAdmin(productId).subscribe({
        next: () => {
          this.notificationService.showSuccess('Product deleted successfully.');
          this.loadAllProducts();
        },
        error: (err: HttpErrorResponse) => {
          this.isLoading.set(false);
          const errorMsg = err.error?.message || 'Failed to delete product.';
          this.errorMessage.set(errorMsg);
          this.notificationService.showError(errorMsg);
          console.error('Error deleting product:', err);
        }
      });
    }
  }

  toggleProductActiveStatus(product: Product): void {
    if (!product || product.id === undefined) return;
    const newStatus = !product.active;
    this.isLoading.set(true); // Veya sadece ilgili satır için bir loading state

    this.adminService.setAdminProductActiveStatus(product.id, newStatus).subscribe({
      next: () => {
        this.notificationService.showSuccess(`Product status updated to ${newStatus ? 'Active' : 'Inactive'}.`);
        this.loadAllProducts();
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        const errorMsg = err.error?.message || 'Failed to update product active status.';
        this.errorMessage.set(errorMsg);
        this.notificationService.showError(errorMsg);
        console.error('Error updating product active status:', err);
      }
    });
  }

  applyFilters(): void {
    this.currentPage.set(0); // Filtre uygulandığında ilk sayfaya dön
    this.loadAllProducts();
  }

  clearFilters(): void {
    this.filterApprovalStatus = '';
    this.currentPage.set(0);
    this.loadAllProducts();
  }

  get pageNumbers(): number[] {
    const totalPages = this.productsResponse()?.totalPages ?? 0;
    return Array(totalPages).fill(0).map((x, i) => i);
  }
}
