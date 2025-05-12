// src/app/features/products/products.routes.ts
import { Routes } from '@angular/router';
import { ProductListComponent } from './product-list/product-list.component';
import { ProductDetailComponent } from './product-detail/product-detail.component';
import { ProductFormComponent } from './product-form/product-form.component'; // ProductFormComponent import edildi
import { authGuard } from '../../core/guards/auth.guard'; // authGuard import edildi
import { roleGuard } from '../../core/guards/role.guard'; // roleGuard import edildi
import { Role } from '../../shared/enums/role.enum'; // Role enum import edildi

export const PRODUCT_ROUTES: Routes = [
  {
    path: '',
    component: ProductListComponent,
    // ProductListComponent için guard ProductsRoutingModule'de AuthGuard idi,
    // ancak public ürün listesi için guard gerekmeyebilir.
    // Eğer tüm ürünler sadece giriş yapmış kullanıcılar tarafından görülecekse canActivate: [authGuard] eklenebilir.
    // Şimdilik guardsız bırakıyoruz, çünkü public bir ürün listesi olabilir.
  },
  {
    path: 'new',
    component: ProductFormComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: [Role.SELLER, Role.ADMIN] } // expectedRoles -> roles ve Role enum kullanıldı
  },
  {
    path: 'edit/:id', // :id parametresi ProductFormComponent tarafından okunacak
    component: ProductFormComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: [Role.SELLER, Role.ADMIN] } // Seller kendi ürününü, Admin herhangi bir ürünü düzenleyebilir (backend kontrol etmeli)
  },
  {
    path: ':slug', // :slug parametresi ProductDetailComponent tarafından okunacak
    component: ProductDetailComponent,
    // ProductDetailComponent için guard ProductsRoutingModule'de AuthGuard idi.
    // Eğer ürün detayları sadece giriş yapmış kullanıcılar tarafından görülecekse canActivate: [authGuard] eklenebilir.
    // Şimdilik guardsız bırakıyoruz.
  }
  // Not: ':slug' ve ':id' (eğer edit için kullanılacaksa) çakışmaması için sıralama önemli.
  // Genellikle daha spesifik olan '/new' ve '/edit/:id' önce gelir.
  // Eğer hem slug hem de id ile detay gösterimi olacaksa, path'ler farklı olmalı veya bir resolver kullanılmalı.
  // Mevcut durumda ProductDetailComponent slug kullanıyor, ProductFormComponent id. Bu ayrım iyi.
];
