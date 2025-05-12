// src/app/features/seller/product-management/my-product-list/my-product-list.component.ts
import { Component, inject, OnInit, signal, WritableSignal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { SellerService } from '../../seller.service'; // SellerService kullanacağız
import { Product } from '../../../../shared/models/product.model'; // PaginatedProducts kaldırıldı
import { Page } from '../../../../shared/models/page.model'; // Page import edildi
import { HttpErrorResponse } from '@angular/common/http';
// ProductCardComponent importu kaldırıldı

@Component({
  selector: 'app-my-product-list',
  standalone: true,
  imports: [CommonModule, RouterLink], // ProductCardComponent import listesinden kaldırıldı
  templateUrl: './my-product-list.component.html',
  styleUrls: ['./my-product-list.component.scss']
})
export class MyProductListComponent implements OnInit {
  private sellerService = inject(SellerService);
  private router = inject(Router);

  productsResponse: WritableSignal<Page<Product> | null> = signal(null); // PaginatedProducts -> Page<Product>
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  // Sayfalama için
  currentPage = signal(0);
  pageSize = signal(10);
  currentSort = signal('name,asc');

  constructor() { }

  ngOnInit(): void {
    this.loadMyProducts();
  }

  loadMyProducts(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.sellerService.getMyProducts(
      this.currentPage(),
      this.pageSize(),
      this.currentSort()
    ).subscribe({
      next: (response) => {
        this.productsResponse.set(response);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(err.error?.message || 'Failed to load your products.');
        this.isLoading.set(false);
        console.error('Error loading seller products:', err);
      }
    });
  }

  onPageChange(page: number): void {
    if (page >= 0 && page < (this.productsResponse()?.totalPages ?? 0)) {
      this.currentPage.set(page);
      this.loadMyProducts();
    }
  }

  editProduct(productId: number): void {
    this.router.navigate(['/seller/products/edit', productId]);
  }

  deleteProduct(productId: number): void {
    if (confirm('Are you sure you want to delete this product? This action cannot be undone.')) {
      this.isLoading.set(true); // Silme işlemi sırasında yükleme göstergesi
      this.sellerService.deleteProduct(productId).subscribe({
        next: () => {
          this.isLoading.set(false);
          this.loadMyProducts(); // Listeyi yenile
          // Başarı mesajı gösterilebilir (NotificationService ile)
        },
        error: (err: HttpErrorResponse) => {
          this.isLoading.set(false);
          this.errorMessage.set(err.error?.message || 'Failed to delete product.');
          console.error('Error deleting product:', err);
        }
      });
    }
  }

  // Sayfalama için array oluşturma
  get pageNumbers(): number[] {
    const totalPages = this.productsResponse()?.totalPages ?? 0;
    return Array(totalPages).fill(0).map((x, i) => i);
  }
}
