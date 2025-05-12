// src/app/features/categories/categories.routes.ts
import { Routes } from '@angular/router';
import { CategoryListComponent } from './category-list/category-list.component';
import { CategoryFormComponent } from './category-form/category-form.component';
import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
import { Role } from '../../shared/enums/role.enum';

export const CATEGORIES_ROUTES: Routes = [
  {
    path: '',
    component: CategoryListComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: [Role.ADMIN, Role.SELLER] }
  },
  {
    path: 'new',
    component: CategoryFormComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: [Role.ADMIN] }
  },
  {
    path: 'edit/:id',
    component: CategoryFormComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: [Role.ADMIN] }
  }
];
