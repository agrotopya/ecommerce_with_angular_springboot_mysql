// src/app/features/seller/seller.routes.ts
import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard.component';
import { MyProductListComponent } from './product-management/my-product-list/my-product-list.component';
import { ProductFormComponent } from './product-management/product-form/product-form.component';
import { CreateFeelComponent } from './components/feel-management/create-feel/create-feel.component'; // Düzeltilmiş yol
import { MyFeelListComponent } from './components/my-feel-list/my-feel-list.component'; // Bu bileşen henüz oluşturulmadı, sonraki adımda
import { MySellerOrdersListComponent } from './order-management/my-seller-orders-list/my-seller-orders-list.component'; // Eklendi
import { MySellerOrderDetailComponent } from './order-management/my-seller-order-detail/my-seller-order-detail.component'; // Eklendi

export const SELLER_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    // data: { breadcrumb: 'Dashboard' }
  },
  {
    path: 'products', // Satıcının ürün listesi
    component: MyProductListComponent,
    // data: { breadcrumb: 'My Products' }
  },
  {
    path: 'products/new', // Yeni ürün ekleme formu
    component: ProductFormComponent,
    // data: { breadcrumb: 'New Product' }
  },
  {
    path: 'products/edit/:productId', // Ürün düzenleme formu
    component: ProductFormComponent,
    // data: { breadcrumb: 'Edit Product' }
  },
  {
    path: 'feels/create', // Yeni feel oluşturma
    component: CreateFeelComponent,
    title: 'Create New Feel | Fibiyo Seller'
    // data: { breadcrumb: 'Create Feel' }
  },
  {
    path: 'feels/my', // Satıcının kendi feel'lerini listeleme
    component: MyFeelListComponent,
    title: 'My Feels | Fibiyo Seller'
    // data: { breadcrumb: 'My Feels' }
  },
  {
    path: 'orders',
    component: MySellerOrdersListComponent,
    title: 'My Orders | Fibiyo Seller',
    // data: { breadcrumb: 'My Orders' }
  },
  {
    path: 'orders/:orderId',
    component: MySellerOrderDetailComponent,
    title: 'Order Details | Fibiyo Seller',
    // data: { breadcrumb: 'Order Details' } // Dinamik breadcrumb için resolver gerekebilir
  },
  // Diğer satıcı paneli yolları buraya eklenecek
  { path: 'feels/edit/:feelId', component: CreateFeelComponent, title: 'Edit Feel | Fibiyo Seller' }, // Aktif edildi
];
