// src/app/core/guards/role.guard.ts
import { inject, PLATFORM_ID } from '@angular/core'; // PLATFORM_ID eklendi
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Role } from '../../shared/enums/role.enum';
import { isPlatformBrowser } from '@angular/common'; // isPlatformBrowser eklendi
// import { NotificationService } from '../services/notification.service';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID); // PLATFORM_ID enjekte edildi
  // const notificationService = inject(NotificationService);
  const expectedRoles = route.data['roles'] as Role[];
  console.log('RoleGuard: Expected roles:', expectedRoles);

  // Sunucu tarafında çalışıyorsa, guard'ı atla ve devam et.
  // Asıl kontrol istemci tarafında AuthInitializer tamamlandıktan sonra yapılacak.
  if (!isPlatformBrowser(platformId)) {
    console.log('RoleGuard: SSR detected. Skipping role check on server.');
    return true; // Sunucuda her zaman izin ver, istemci tarafı guard'ı halleder.
  }

  // İstemci tarafı kontrolleri
  const isAuthenticated = authService.isAuthenticated();
  console.log('RoleGuard: Client-side - isAuthenticated:', isAuthenticated);

  if (!isAuthenticated) {
    console.log('RoleGuard: Client-side - User not authenticated. Redirecting to login.');
    router.navigate(['/auth/login']);
    return false;
  }

  const currentUser = authService.currentUser();
  console.log('RoleGuard: Client-side - Current user:', JSON.stringify(currentUser)); // Detaylı log

  if (!currentUser || !currentUser.roles || currentUser.roles.length === 0) {
    console.log('RoleGuard: Client-side - Current user has no roles or user data is missing. Redirecting to home.');
    // notificationService.showError('You do not have the necessary permissions.');
    router.navigate(['/']); // Veya yetkisiz erişim sayfasına
    return false;
  }

  console.log('RoleGuard: Client-side - Current user roles:', currentUser.roles);

  let userHasRequiredRole = false;
  for (const expectedRole of expectedRoles) {
    const checkRoleResult = authService.hasRole(expectedRole);
    console.log(`RoleGuard: Client-side - Checking for role '${expectedRole}': ${checkRoleResult}`);
    if (checkRoleResult) {
      userHasRequiredRole = true;
      break;
    }
  }

  if (userHasRequiredRole) {
    console.log('RoleGuard: Client-side - User has an expected role. Access granted.');
    return true;
  } else {
    console.log('RoleGuard: Client-side - User does not have an expected role. Redirecting to home.');
    // notificationService.showError('You do not have permission to access this page.');
    router.navigate(['/']); // Veya yetkisiz erişim sayfasına
    return false;
  }
};
