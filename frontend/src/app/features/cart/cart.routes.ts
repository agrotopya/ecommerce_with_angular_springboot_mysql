// src/app/features/cart/cart.routes.ts
import { Routes } from '@angular/router';
import { CartViewComponent } from './cart-view/cart-view.component';

export const CART_ROUTES: Routes = [
  { path: '', component: CartViewComponent }
];