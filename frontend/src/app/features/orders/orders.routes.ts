// src/app/features/orders/orders.routes.ts
import { Routes } from '@angular/router';
import { OrderListComponent } from './order-list/order-list.component';
import { OrderDetailComponent } from './order-detail/order-detail.component';
import { OrderSuccessComponent } from './order-success/order-success.component';
import { authGuard } from '../../core/guards/auth.guard';
import { Role } from '../../shared/enums/role.enum';

export const ORDERS_ROUTES: Routes = [
  {
    path: '',
    component: OrderListComponent,
    canActivate: [authGuard]
  },
  {
    path: 'success', // ':id' kaldırıldı, query params kullanılacak
    component: OrderSuccessComponent,
    canActivate: [authGuard] // data: { roles: [Role.CUSTOMER] } burada gereksiz olabilir, authGuard yeterli.
    // Gerekirse roleGuard da eklenebilir: canActivate: [authGuard, roleGuard], data: { roles: [Role.CUSTOMER] }
  },
  {
    path: ':id',
    component: OrderDetailComponent,
    canActivate: [authGuard]
  }
];
