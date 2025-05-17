// src/app/core/interceptors/error.interceptor.ts
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError, tap } from 'rxjs';
import { inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service'; // AuthService import edildi
import { isPlatformBrowser } from '@angular/common'; // isPlatformBrowser import edildi
import { NotificationService } from '../services/notification.service'; // NotificationService import edildi

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);
  const notificationService = inject(NotificationService); // NotificationService inject edildi

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      console.error('ErrorInterceptor caught an error:', error);

      // Hata mesajını göstermek için NotificationService kullanılabilir
      let errorMessage = 'An unexpected error occurred. Please try again.';
      if (error.error && typeof error.error.message === 'string') {
        errorMessage = error.error.message;
      } else if (typeof error.message === 'string') {
        errorMessage = error.message;
      }

      switch (error.status) {
        case 0: // Ağ hatası, CORS sorunu veya sunucuya ulaşılamıyor
          console.error('ErrorInterceptor: Network error, CORS issue, or server unreachable. Status: 0', error);
          notificationService.showError('Could not connect to the server. Please check your internet connection or try again later.');
          break;
        case 401:
          console.warn('ErrorInterceptor: 401 Unauthorized.');
          // Sunucu taraflı render sırasında window/localStorage erişimi yok
          if (isPlatformBrowser(platformId)) {
            // Token geçersiz veya yoksa logout yap ve login sayfasına yönlendir
            // /auth/login veya /auth/register gibi endpoint'lerden 401 gelirse logout yapma
            if (!req.url.includes('/api/auth/')) {
              notificationService.showError('Session expired or unauthorized. Please login again.');
              authService.logout(); // Token'ı ve kullanıcı bilgisini temizle
              router.navigate(['/auth/login'], { queryParams: { returnUrl: router.url } });
            } else {
              // Auth endpoint'lerinden gelen 401'ler için genel hata mesajı
              notificationService.showError(errorMessage || 'Authentication failed.');
            }
          }
          break;
        case 403:
          console.warn('ErrorInterceptor: 403 Forbidden.');
          notificationService.showError(errorMessage || 'You do not have permission to access this resource.');
          // İsteğe bağlı olarak kullanıcıyı ana sayfaya veya başka bir sayfaya yönlendirebilirsiniz.
          // router.navigate(['/']);
          break;
        case 404:
          console.warn('ErrorInterceptor: 404 Not Found.');
          notificationService.showError(errorMessage || 'The requested resource was not found.');
          break;
        case 500:
          console.error('ErrorInterceptor: 500 Internal Server Error.');
          notificationService.showError(errorMessage || 'An internal server error occurred. Please try again later.');
          break;
        default:
          console.error(`ErrorInterceptor: Unexpected Error. Status: ${error.status}`, error);
          notificationService.showError(errorMessage);
      }

      // Orijinal hatayı veya daha anlamlı bir hata nesnesini döndür
      return throwError(() => error); // error.message yerine tüm error objesini döndürmek daha iyi olabilir
    })
  );
};
