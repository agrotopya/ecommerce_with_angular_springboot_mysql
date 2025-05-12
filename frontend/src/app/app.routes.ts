// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { AdminLayoutComponent } from './layout/admin-layout/admin-layout.component'; // AdminLayoutComponent import edildi
import { HomeComponent } from './features/home/home.component';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard'; // roleGuard import edildi
import { Role } from './shared/enums/role.enum'; // Role enum import edildi

// src/app/app.routes.ts
// ... importlar ...

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    // canActivate: [authGuard], // Artık MainLayout guard'sız, çocuklar kendileri korunacak
    children: [
      { path: '', component: HomeComponent, pathMatch: 'full' },
      {
        path: 'products',
        loadChildren: () => import('./features/products/products.routes').then(m => m.PRODUCT_ROUTES),
      },
      {
        path: 'profile',
        loadChildren: () => import('./features/profile/profile.routes').then(m => m.PROFILE_ROUTES),
        canActivate: [authGuard],
      },
      { // EKLENDİ
        path: 'cart',
        loadChildren: () => import('./features/cart/cart.routes').then(m => m.CART_ROUTES),
        canActivate: [authGuard], // Sepet için login gerekli
      },
      { // EKLENDİ
        path: 'checkout',
        loadChildren: () => import('./features/checkout/checkout.routes').then(m => m.CHECKOUT_ROUTES),
        canActivate: [authGuard], // Ödeme için login gerekli
      },
      {
        path: 'categories',
        loadChildren: () => import('./features/categories/categories.routes').then(m => m.CATEGORIES_ROUTES),
        // canActivate ve data özellikleri categories.routes.ts içinde tanımlandığı için burada tekrar gerekmez.
        // Ancak, eğer tüm /categories altındaki yollar belirli bir rol gerektiriyorsa,
        // burada da bir canActivate eklenebilir. Şimdilik alt rotalara bırakıyoruz.
      },
      // Feels rotası zaten ekli, sadece yorum satırı kaldırılacak veya teyit edilecek.
      // Eğer zaten varsa, bu blok bir değişiklik yapmayacaktır.
      {
        path: 'feels',
        loadChildren: () => import('./features/feels/feels.routes').then(m => m.FEEL_ROUTES),
      },
      {
        path: 'subscriptions',
        loadChildren: () => import('./features/subscriptions/subscriptions.routes').then(m => m.SUBSCRIPTION_ROUTES),
      },
      {
        path: 'wishlist',
        loadChildren: () => import('./features/wishlist/wishlist.routes').then(m => m.WISHLIST_ROUTES),
        canActivate: [authGuard] // İstek listesi için login gerekli (roleGuard Customer için wishlist.routes içinde)
      },
      {
        path: 'orders', // Siparişler için ana yol
        loadChildren: () => import('./features/orders/orders.routes').then(m => m.ORDERS_ROUTES),
        canActivate: [authGuard] // Siparişler için login gerekli
      },
    ]
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES),
  },
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: [Role.ADMIN] }, // Sadece ADMIN rolü erişebilir
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.ADMIN_ROUTES)
  },
  {
    path: 'seller',
    component: MainLayoutComponent, // Şimdilik MainLayout, gerekirse SellerLayout oluşturulur
    canActivate: [authGuard, roleGuard],
    data: { roles: [Role.SELLER] }, // Sadece SELLER rolü erişebilir
    loadChildren: () => import('./features/seller/seller.routes').then(m => m.SELLER_ROUTES)
  },
  { path: '**', redirectTo: '' }
];
