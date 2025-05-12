// src/app/features/wishlist/wishlist.routes.ts
import { Routes } from '@angular/router';
import { WishlistViewComponent } from './components/wishlist-view/wishlist-view.component';
import { authGuard } from '@core/guards/auth.guard'; // AuthGuard -> authGuard

export const WISHLIST_ROUTES: Routes = [
  {
    path: '',
    component: WishlistViewComponent,
    canActivate: [authGuard], // AuthGuard -> authGuard
    title: 'My Wishlist | Fibiyo'
    // data: { breadcrumb: 'My Wishlist' }
  }
];
