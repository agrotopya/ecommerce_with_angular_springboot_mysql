// src/app/features/admin/admin.routes.ts
import { Routes } from '@angular/router';
import { AdminDashboardComponent } from './dashboard/admin-dashboard.component';
import { UserManagementComponent } from './user-management/user-management.component'; // UserManagementComponent import edildi

export const ADMIN_ROUTES: Routes = [
  {
    path: '', // /admin yolu (AdminLayoutComponent tarafından sarmalanacak)
    redirectTo: 'dashboard', // Varsayılan olarak dashboard'a yönlendir
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    component: AdminDashboardComponent
  },
  {
    path: 'user-management',
    component: UserManagementComponent // UserManagementComponent eklendi
  },
  {
    path: 'product-management',
    // component: AdminProductListComponent // Oluşturulacak component
    // Şimdilik direkt component, ileride alt route'lar için loadChildren olabilir
    loadChildren: () => import('./product-management/product-management.routes').then(m => m.PRODUCT_MANAGEMENT_ROUTES) // Alt route'lar için
  },
  {
    path: 'coupon-management', // frontend.md'ye göre /admin/coupon-management
    // Bileşenler src/app/modules/coupons/components/ altında, bu yol düzeltilmeli
    // veya bileşenler src/app/features/admin/coupon-management/ altına taşınmalı.
    // Şimdilik modül yolunu kullanıyoruz, taşıma sonraki adımda.
    children: [
      {
        path: '',
        // Bileşenler src/app/features/coupons/components/ altına taşındı.
        loadComponent: () => import('../coupons/components/admin-coupon-list/admin-coupon-list.component').then(m => m.AdminCouponListComponent),
        // canActivate ve data burada tanımlanabilir veya alt rotanın kendi içinde.
        // CouponsRoutingModule'deki guardlar burada geçerli olacak.
      },
      {
        path: 'new',
        loadComponent: () => import('../coupons/components/admin-coupon-form/admin-coupon-form.component').then(m => m.AdminCouponFormComponent),
      },
      {
        path: 'edit/:id',
        loadComponent: () => import('../coupons/components/admin-coupon-form/admin-coupon-form.component').then(m => m.AdminCouponFormComponent),
      }
    ]
  },
  {
    path: 'feel-management',
    loadComponent: () => import('./components/feel-management/admin-feel-list/admin-feel-list.component').then(m => m.AdminFeelListComponent),
    title: 'Feel Management | Admin Fibiyo'
  },
  {
    path: 'review-management', // frontend.md'ye göre /admin/review-management
    // Bileşen src/app/features/reviews/components/admin-review-management/ altına taşındı.
    loadComponent: () => import('../reviews/components/admin-review-management/admin-review-management.component').then(m => m.AdminReviewManagementComponent),
  },
  {
    path: 'orders', // Sipariş listesi
    loadComponent: () => import('./order-management/admin-order-list/admin-order-list.component').then(m => m.AdminOrderListComponent),
  },
  {
    path: 'orders/:id', // Sipariş detayı (admin için)
    loadComponent: () => import('./order-management/admin-order-detail/admin-order-detail.component').then(m => m.AdminOrderDetailComponent),
  }
  // Diğer admin alt route'ları buraya eklenecek
];
