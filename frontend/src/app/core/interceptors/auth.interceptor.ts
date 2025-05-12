// src/app/core/interceptors/auth.interceptor.ts
import { HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authService = inject(AuthService);
  const token = authService.getToken();
  const apiUrl = environment.apiUrl; // API URL'miz

  console.log('AuthInterceptor: Processing request for URL:', req.url);
  console.log('AuthInterceptor: API URL:', apiUrl);
  console.log('AuthInterceptor: Token exists:', !!token);
  console.log('AuthInterceptor: URL starts with API URL:', req.url.startsWith(apiUrl));

  // Sadece kendi API'mize giden isteklere token ekle
  if (token && req.url.startsWith(apiUrl)) {
    // Auth endpoint'leri (login/register) için token göndermeye gerek yok
    if (req.url.includes('/auth/login') || req.url.includes('/auth/register')) {
      console.log('AuthInterceptor: Auth endpoint, no token needed.');
      return next(req);
    }

    // Temel token format kontrolü (JWT genellikle 3 bölümden oluşur)
    const tokenParts = token.split('.');
    if (tokenParts.length !== 3) {
      console.error('AuthInterceptor: Invalid token format. Token will not be used.');
      // Token formatı bozuksa, isteği token'sız göndermek veya hata vermek yerine
      // authService.logout() çağrılabilir ve kullanıcı login'e yönlendirilebilir.
      // Şimdilik sadece token'sız devam edelim, bu backend'den 401 almasına neden olacaktır.
      return next(req);
    }

    // Backend'den gelen token'ın zaten "Bearer " ile başlayıp başlamadığını kontrol et.
    // AuthService.getToken() saf token'ı mı yoksa "Bearer " ile mi döndürüyor?
    // AuthService.login() metodunda token'ı "Bearer " olmadan sakladığımızı varsayarsak:
    const authToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`;

    const authReq = req.clone({
      headers: req.headers.set('Authorization', authToken),
    });

    console.log('AuthInterceptor: Token added to request for URL:', authReq.url);
    console.log('AuthInterceptor: Authorization header set.'); // Değeri loglamak güvenlik riski olabilir.
    return next(authReq);

  } else {
    console.log('AuthInterceptor: No token to add or not an API URL.');
    if (!token) {
      console.warn('AuthInterceptor: Token is null or undefined when trying to process URL:', req.url);
    }
    if (!req.url.startsWith(apiUrl)) {
      console.log('AuthInterceptor: URL does not start with API URL. URL:', req.url, 'API URL:', apiUrl);
    }
    return next(req);
  }
};


/*AuthInterceptor: Her giden API isteğine otomatik olarak JWT (Bearer) token'ını ekleyecek. */
/*HttpInterceptorFn fonksiyonel interceptor'lar için kullanılır.
AuthService'ten mevcut token'ı alır.
Eğer token varsa ve istek bizim backend API'mize (environment.apiUrl ile başlayan) gidiyorsa, isteği klonlayarak Authorization header'ına Bearer <token> değerini ekler.
Aksi halde isteği değiştirmeden devam ettirir. */
