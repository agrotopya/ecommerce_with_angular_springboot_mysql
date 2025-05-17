// src/app/core/services/auth.service.ts
import { Injectable, inject, signal, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, Observable, tap, throwError } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { ApiService } from './api.service';
import { StorageService } from './storage.service';
import { AUTH_ENDPOINTS, USER_ENDPOINTS } from '../constants/api-endpoints'; // USER_ENDPOINTS eklendi
import { Role } from '../../shared/enums/role.enum'; // Ensure Role enum is correctly defined
import { LoginRequest, User, JwtAuthResponse, RegisterRequest, ApiResponse, ChangePasswordRequest } from '../../shared/models/user.model'; // UserResponse -> User, ChangePasswordRequest eklendi

const USER_KEY = 'auth-user';
const TOKEN_KEY = 'auth-token';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiService = inject(ApiService);
  private storageService = inject(StorageService);
  private router = inject(Router);
  private platformId = inject(PLATFORM_ID);

  public currentUser = signal<User | null>(null);
  public isAuthenticated = signal<boolean>(false);

  constructor() {
    console.log(
      `AuthService constructor: Instance created. Running on ${isPlatformBrowser(this.platformId) ? 'browser' : 'server'}. Auth state will be initialized by APP_INITIALIZER on the browser.`
    );
    if (isPlatformBrowser(this.platformId)) {
      console.log('AuthService constructor (Browser): Token from StorageService at construction:', this.storageService.getItem<string>(TOKEN_KEY));
    }


  }

  public initAuthState(): Promise<void> {
    return new Promise((resolve) => {
      if (isPlatformBrowser(this.platformId)) {
        console.log('AuthService initAuthState (Browser): Token from StorageService at initAuthState start:', this.storageService.getItem<string>(TOKEN_KEY));
        console.log('AuthService initAuthState: Running in browser. Loading state from storage.');
        this.loadAuthStateFromStorage();
        this.checkTokenValidity();
      } else {
        console.log('AuthService initAuthState: Running on server. Skipping storage access.');
      }
      resolve();
    });
  }

  private checkTokenValidity(): void {
    const token = this.getToken();
    if (!token) {
      console.log('AuthService checkTokenValidity: No token found.');
      this.isAuthenticated.set(false);
      this.currentUser.set(null);
      return;
    }

    try {
      // Token'ın geçerliliğini kontrol et (basit bir kontrol)
      const tokenParts = token.split('.');
      if (tokenParts.length !== 3) {
        console.error('AuthService checkTokenValidity: Invalid token format.');
        this.logout();
        return;
      }

      // Token payload'ını decode et
      const payload = JSON.parse(atob(tokenParts[1]));
      console.log('AuthService checkTokenValidity: Token payload:', payload);

      // Token'da roles alanı var mı kontrol et
      if (!payload.roles) {
        console.error('AuthService checkTokenValidity: Token does not contain roles.');
        // Roles yoksa logout yapmayalım, sadece log atalım
      }

      const expiryTime = payload.exp * 1000; // saniyeden milisaniyeye çevir
      const currentTime = Date.now();

      console.log('AuthService checkTokenValidity: Token expiry time:', new Date(expiryTime));
      console.log('AuthService checkTokenValidity: Current time:', new Date(currentTime));
      console.log('AuthService checkTokenValidity: Token expires in:', Math.floor((expiryTime - currentTime) / 1000 / 60), 'minutes');

      if (expiryTime < currentTime) {
        console.error('AuthService checkTokenValidity: Token expired.');
        this.logout();
        return;
      }

      console.log('AuthService checkTokenValidity: Token is valid.');
    } catch (error) {
      console.error('AuthService checkTokenValidity: Error checking token validity:', error);
      this.logout();
    }
  }

  private loadAuthStateFromStorage(): void {
    const token = this.storageService.getItem<string>(TOKEN_KEY);
    const user = this.storageService.getItem<User>(USER_KEY);
    console.log('AuthService loadAuthStateFromStorage: User from storage:', JSON.stringify(user)); // Detaylı log

    if (token && user) {
      console.log('AuthService loadAuthStateFromStorage: User roles from storage before check:', JSON.stringify(user.roles));
      // Eğer storage'dan gelen user.roles null veya undefined ise, boş dizi ata
      if (user.roles === null || user.roles === undefined) {
        console.warn('AuthService loadAuthStateFromStorage: roles field was null or undefined in stored user, defaulting to empty array.');
        user.roles = [];
      }
      console.log('AuthService loadAuthStateFromStorage: User roles from storage after check:', JSON.stringify(user.roles));
      this.currentUser.set(user);
      this.isAuthenticated.set(true);
      console.log('AuthService: User is authenticated from storage. User:', user.username, 'Roles:', JSON.stringify(user.roles));
    } else {
      this.currentUser.set(null);
      this.isAuthenticated.set(false);
      console.log('AuthService: No valid authentication state in storage.');
      if (token || user) {
        this.clearStoredAuthState('Inconsistent state detected during load');
      }
    }
  }

  private clearStoredAuthState(reason: string): void {
    this.storageService.removeItem(TOKEN_KEY);
    this.storageService.removeItem(USER_KEY);
    console.log(`AuthService: Cleared stored authentication state. Reason: ${reason}`);
  }

  login(credentials: LoginRequest): Observable<JwtAuthResponse> {
    console.log('AuthService login: Attempting to login with credentials:', credentials);
    return this.apiService.post<JwtAuthResponse>(AUTH_ENDPOINTS.LOGIN, credentials).pipe(
      tap((response: JwtAuthResponse) => {
        console.log('AuthService login - TAP: Full API Response from backend:', JSON.stringify(response, null, 2));

        if (!response || !response.accessToken) {
          console.error('AuthService login - TAP: Invalid response from server or missing accessToken.');
          this.isAuthenticated.set(false);
          this.currentUser.set(null);
          return;
        }

        try {
          console.log('AuthService login - TAP: Raw token:', response.accessToken);
          const tokenParts = response.accessToken.split('.');
          if (tokenParts.length !== 3) {
            console.error('AuthService login - TAP: Invalid token format.');
            this.isAuthenticated.set(false);
            this.currentUser.set(null);
            return;
          }
          const payload = JSON.parse(atob(tokenParts[1]));
          console.log('AuthService login - TAP: Token payload:', payload);

          const expiryTime = payload.exp * 1000;
          const currentTime = Date.now();
          console.log('AuthService login - TAP: Token expiry time:', new Date(expiryTime));
          console.log('AuthService login - TAP: Current time:', new Date(currentTime));
          console.log('AuthService login - TAP: Token expires in:', Math.floor((expiryTime - currentTime) / 1000 / 60), 'minutes');

          if (expiryTime < currentTime) {
            console.error('AuthService login - TAP: Token expired.');
            this.isAuthenticated.set(false);
            this.currentUser.set(null);
            return;
          }
        } catch (error) {
          console.error('AuthService login - TAP: Error checking token:', error);
          this.isAuthenticated.set(false);
          this.currentUser.set(null);
          return;
        }

        // JwtAuthResponse'dan gelen bilgileri kullanarak User oluştur
        // firstName, lastName gibi detaylı bilgiler /users/me çağrısıyla alınacak.
        const initialUser: User = {
          id: response.userId,
          username: response.username,
          email: response.email,
          roles: response.roles || [], // response.roles doğrudan kullanılacak
          active: true, // Varsayılan, /users/me ile güncellenecek (isActive -> active)
          createdAt: new Date().toISOString(), // Geçici, /users/me ile güncellenecek
          updatedAt: new Date().toISOString(), // Geçici, /users/me ile güncellenecek
          authProvider: 'LOCAL', // Varsayılan, /users/me ile güncellenecek
          // Diğer alanlar (firstName, lastName, avatar vb.) fetchAndStoreFullCurrentUser ile doldurulacak
        };

        console.log('AuthService login - TAP: Initial user object constructed from JwtAuthResponse:', JSON.stringify(initialUser, null, 2));
        this.storageService.setItem(TOKEN_KEY, response.accessToken);
        this.storageService.setItem(USER_KEY, initialUser); // Başlangıçta minimal kullanıcı bilgisi saklanır
        this.currentUser.set(initialUser);
        this.isAuthenticated.set(true);
        console.log('AuthService login - TAP: Storage and signals updated. User:', initialUser.username, 'isAuthenticated:', this.isAuthenticated());

        // Tam kullanıcı bilgisini (firstName, lastName vb.) /users/me'den çek ve güncelle.
        this.fetchAndStoreFullCurrentUser().subscribe({
          next: (fullUser) => {
            console.log('AuthService login - TAP: Successfully fetched and stored full user details:', fullUser);
          },
          error: (err) => {
            console.error('AuthService login - TAP: Error fetching full user details after login:', err);
            // Hata durumunda bile minimal kullanıcı bilgisiyle devam edilebilir.
          }
        });
      }),
      catchError(err => {
        console.error('AuthService login - Error caught in catchError:', err);
        this.isAuthenticated.set(false);
        this.currentUser.set(null);
        return throwError(() => err);
      })
    );
  }

  register(userData: RegisterRequest): Observable<ApiResponse> {
    console.log('AuthService register: Attempting to register user:', userData);
    return this.apiService.post<ApiResponse>(AUTH_ENDPOINTS.REGISTER, userData).pipe(
      tap((response: ApiResponse) => {
        console.log('AuthService register - TAP: Registration response:', response);
      }),
      catchError(err => {
        console.error('AuthService register - Error caught in catchError:', err);
        return throwError(() => err);
      })
    );
  }

  forgotPassword(email: string): Observable<ApiResponse> {
    console.log('AuthService forgotPassword: Attempting to send reset link for email:', email);
    return this.apiService.post<ApiResponse>(AUTH_ENDPOINTS.FORGOT_PASSWORD, { email }).pipe(
      tap((response: ApiResponse) => {
        console.log('AuthService forgotPassword - TAP: Response:', response);
      }),
      catchError(err => {
        console.error('AuthService forgotPassword - Error caught in catchError:', err);
        return throwError(() => err);
      })
    );
  }

  resetPassword(token: string, newPassword: string): Observable<ApiResponse> {
    console.log('AuthService resetPassword: Attempting to reset password with token.');
    return this.apiService.post<ApiResponse>(AUTH_ENDPOINTS.RESET_PASSWORD, { token, newPassword }).pipe(
      tap((response: ApiResponse) => {
        console.log('AuthService resetPassword - TAP: Response:', response);
      }),
      catchError(err => {
        console.error('AuthService resetPassword - Error caught in catchError:', err);
        return throwError(() => err);
      })
    );
  }

  logout(): void {
    this.clearStoredAuthState('User logout');
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
    console.log('AuthService logout: User logged out. isAuthenticated:', this.isAuthenticated());
    this.router.navigate(['/auth/login']);
  }

  getToken(): string | null {
    if (isPlatformBrowser(this.platformId)) {
        const tokenFromStorage = this.storageService.getItem<string>(TOKEN_KEY);
        console.log('AuthService getToken (Browser): Token from StorageService:', tokenFromStorage); // YENİ LOG
        return tokenFromStorage;
    }
    console.log('AuthService getToken (Server): Returning null as not in browser.'); // YENİ LOG
    return null;
  }

  hasRole(requiredRole: Role): boolean {
    const user = this.currentUser();
    if (!user || !user.roles) {
      return false;
    }
    // Roller 'ROLE_ADMIN', 'ROLE_SELLER' formatında geliyorsa direkt kontrol et, değilse 'ROLE_' ekle.
    // Backend'den gelen rollerin formatına göre bu kısım ayarlanmalı.
    // Şimdilik hem direkt eşleşme hem de 'ROLE_' prefix'li eşleşmeyi kontrol ediyoruz.
    const hasDirectRole = user.roles.includes(requiredRole);
    const hasPrefixedRole = user.roles.includes(`ROLE_${requiredRole}` as Role);
    return hasDirectRole || hasPrefixedRole;
  }

  updateCurrentUser(updatedUserFields: Partial<User>): void {
    const currentUser = this.currentUser();
    if (currentUser) {
      // Eğer updatedUserFields.roles tanımsızsa, mevcut rolleri koru.
      // Eğer updatedUserFields.roles tanımlıysa (boş dizi bile olsa), onu kullan.
      // Bu, /users/me'den gelen rollerin her zaman doğru ve güncel olduğu varsayımına dayanır.
      const newRoles = updatedUserFields.roles !== undefined ? updatedUserFields.roles : currentUser.roles;
      const newUser = { ...currentUser, ...updatedUserFields, roles: newRoles };

      this.currentUser.set(newUser);
      this.storageService.setItem(USER_KEY, newUser);
      console.log('AuthService: Current user updated in signal and storage:', JSON.stringify(newUser, null, 2));
    }
  }

  changePassword(request: ChangePasswordRequest): Observable<ApiResponse> {
    console.log('AuthService changePassword: Attempting to change password.');
    return this.apiService.patch<ApiResponse>(USER_ENDPOINTS.CHANGE_PASSWORD, request).pipe(
      tap((response: ApiResponse) => {
        console.log('AuthService changePassword - TAP: Response:', response);
        // Başarılı şifre değişikliğinden sonra token'ı yenilemek veya kullanıcıyı bilgilendirmek gerekebilir.
        // Şimdilik sadece logluyoruz.
      }),
      catchError(err => {
        console.error('AuthService changePassword - Error caught in catchError:', err);
        return throwError(() => err);
      })
    );
  }

  private fetchAndStoreFullCurrentUser(): Observable<User> {
    return this.apiService.get<User>(USER_ENDPOINTS.ME).pipe(
      tap(fullUser => {
        console.log('AuthService: Fetched full user details from /users/me:', fullUser);
        this.updateCurrentUser(fullUser);
      }),
      catchError(err => {
        console.error('AuthService: Error fetching full user details from /users/me:', err);
        // Kullanıcı bilgisi çekilemezse, mevcut (eksik) bilgiyle devam et.
        return throwError(() => err);
      })
    );
  }
}
