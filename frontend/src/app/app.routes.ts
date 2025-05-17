// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { AdminLayoutComponent } from './layout/admin-layout/admin-layout.component';
import { HomeComponent } from './features/home/home.component';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { Role } from './shared/enums/role.enum';
import { SellerProfileComponent } from './features/sellers/components/seller-profile/seller-profile.component'; // GEÇİCİ IMPORT

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
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
      {
        path: 'cart',
        loadChildren: () => import('./features/cart/cart.routes').then(m => m.CART_ROUTES),
        canActivate: [authGuard],
      },
      {
        path: 'checkout',
        loadChildren: () => import('./features/checkout/checkout.routes').then(m => m.CHECKOUT_ROUTES),
        canActivate: [authGuard],
      },
      {
        path: 'categories',
        loadChildren: () => import('./features/categories/categories.routes').then(m => m.CATEGORIES_ROUTES),
      },
      {
        path: 'subscriptions',
        loadChildren: () => import('./features/subscriptions/subscriptions.routes').then(m => m.SUBSCRIPTION_ROUTES),
      },
      {
        path: 'wishlist',
        loadChildren: () => import('./features/wishlist/wishlist.routes').then(m => m.WISHLIST_ROUTES),
        canActivate: [authGuard]
      },
      {
        path: 'orders',
        loadChildren: () => import('./features/orders/orders.routes').then(m => m.ORDERS_ROUTES),
        canActivate: [authGuard]
      },
    ]
  },
  {
    path: 'feels',
    loadChildren: () => import('./features/feels/feels.routes').then(m => m.FEEL_ROUTES),
  },
  {
    path: 'sellers/:sellerId', // Satıcı profil sayfası
    component: SellerProfileComponent, // GEÇİCİ DEĞİŞİKLİK: loadChildren yerine component
    // loadChildren: () => import('./features/sellers/sellers.routes').then(m => m.SELLERS_ROUTES) // GEÇİCİ YORUM
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES),
  },
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: [Role.ADMIN] },
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.ADMIN_ROUTES)
  },
  {
    path: 'seller',
    component: MainLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: [Role.SELLER] },
    loadChildren: () => import('./features/seller/seller.routes').then(m => m.SELLER_ROUTES)
  },
  { path: '**', redirectTo: '' }
];
