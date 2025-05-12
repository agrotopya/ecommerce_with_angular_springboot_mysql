// src/app/features/profile/profile.routes.ts
import { Routes } from '@angular/router';
import { ProfileComponent } from './profile/profile.component';
import { authGuard } from '../../core/guards/auth.guard'; // AuthGuard import et

// OrderListComponent ve OrderDetailComponent importları kaldırıldı, loadChildren kullanılacak.

export const PROFILE_ROUTES: Routes = [
  {
    path: '', // /profile yolu
    component: ProfileComponent, // ProfileComponent'in standalone olduğundan emin olunmalı.
    canActivate: [authGuard], // Sadece giriş yapmış kullanıcılar erişebilir
    children: [
      // /profile altındaki diğer yollar buraya eklenebilir, örneğin /profile/edit, /profile/change-password
      // Şimdilik /profile ana sayfası ProfileComponent'i gösteriyor.
      // Eğer ProfileComponent'in içinde bir <router-outlet> varsa ve /profile/orders gibi
      // alt yolların ProfileComponent layout'u içinde render edilmesi isteniyorsa,
      // orders rotası buraya children olarak eklenebilir.
      // Ancak frontend.md planına göre /profile/orders ayrı bir sayfa gibi duruyor.
      // Bu durumda orders rotasını ProfileComponent'in child'ı yapmak yerine
      // app.routes.ts'de /profile altında ayrı bir loadChildren olarak tanımlamak daha mantıklı olabilir.
      // Ya da ProfileComponent'in kendisi bir layout ve içinde <router-outlet> var ise,
      // o zaman aşağıdaki gibi orders'ı child yapmak uygun olur.
      // Şimdilik, orders'ı /profile altında ayrı bir loadChildren olarak bırakalım.
    ]
  },
  {
    path: 'orders', // /profile/orders yolu
    loadChildren: () => import('../orders/orders.routes').then(m => m.ORDERS_ROUTES),
    canActivate: [authGuard] // Bu guard ORDERS_ROUTES içindeki guardlarla birleşir.
  },
  {
    path: 'change-password',
    // ChangePasswordComponent'in doğru yolunu import etmemiz gerekiyor.
    // Şu anki yapıda src/app/features/users/change-password/change-password.component.ts altında.
    // TODO.md'ye göre bu bileşen /profile/change-password altına taşınmalı. (Taşındı)
    loadComponent: () => import('./change-password/change-password.component').then(m => m.ChangePasswordComponent),
    canActivate: [authGuard]
  }
  // MySubscriptionsComponent için rota da buraya eklenecek.
  // İleride /profile/settings gibi diğer alt route'lar eklenebilir
];
