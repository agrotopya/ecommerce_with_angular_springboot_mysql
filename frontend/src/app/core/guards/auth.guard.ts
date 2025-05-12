// src/app/core/guards/auth.guard.ts
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { isPlatformBrowser } from '@angular/common';

export const authGuard: CanActivateFn = async (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  // SSR sırasında guard'ı atla
  if (!isPlatformBrowser(platformId)) {
    console.log('AuthGuard: SSR detected. Skipping authentication check.');
    return true; // SSR sırasında kontrolü atla
  }

  console.log('AuthGuard: Client-side. Waiting for auth state to initialize...');
  await authService.initAuthState();

  const isAuthenticated = authService.isAuthenticated();
  console.log('AuthGuard: Authentication status after init:', isAuthenticated);

  if (isAuthenticated) {
    console.log('AuthGuard: Access granted.');
    return true;
  } else {
    console.log('AuthGuard: Access denied. Redirecting to login.');
    router.navigate(['/auth/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }
};
