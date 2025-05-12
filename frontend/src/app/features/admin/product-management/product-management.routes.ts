// src/app/features/admin/product-management/product-management.routes.ts
import { Routes } from '@angular/router';
import { AdminProductListComponent } from './admin-product-list/admin-product-list.component'; // Oluşturulacak

export const PRODUCT_MANAGEMENT_ROUTES: Routes = [
  {
    path: '', // Bu, /admin/product-management yoluna denk gelecek
    component: AdminProductListComponent,
    // data: { breadcrumb: 'Product Approvals' } // Opsiyonel
  }
  // Admin için ürün detay veya düzenleme sayfası gerekirse buraya eklenebilir
  // Örneğin: { path: 'view/:productId', component: AdminProductDetailComponent }
];
