import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard'; // authGuard olarak düzeltildi
import { SubscriptionPlansComponent } from './components/subscription-plans/subscription-plans.component'; // Yol güncellendi

export const SUBSCRIPTION_ROUTES: Routes = [
  {
    path: 'plans',
    component: SubscriptionPlansComponent,
    canActivate: [authGuard] // All authenticated users can see plans
  }
  // MySubscriptionsComponent için rota /profile/my-subscriptions altında olacak.
];
