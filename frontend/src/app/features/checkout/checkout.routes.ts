// src/app/features/checkout/checkout.routes.ts
import { Routes } from '@angular/router';
import { CheckoutPageComponent } from './checkout-page/checkout-page.component';

export const CHECKOUT_ROUTES: Routes = [
  { path: '', component: CheckoutPageComponent }
  // İleride adımlar (shipping, payment) ayrı component/route olabilir.
];
