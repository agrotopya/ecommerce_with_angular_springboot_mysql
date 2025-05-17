import { Injectable, inject } from '@angular/core';
import { Observable, ReplaySubject, tap, catchError, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { User } from '@shared/models/user.model'; // User modelini import et
import { ApiService } from './api.service';
import { USER_ENDPOINTS } from '../constants/api-endpoints';
import { NotificationService } from './notification.service';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiService = inject(ApiService);
  private notificationService = inject(NotificationService);

  // Mevcut kullanıcı profilini getirmek için
  getCurrentUserProfile(): Observable<User> {
    return this.apiService.get<User>(USER_ENDPOINTS.ME).pipe(
      catchError((error: HttpErrorResponse) => {
        this.notificationService.showError('Failed to load user profile.');
        return throwError(() => error);
      })
    );
  }

  // TODO: Backend'e eklendikten sonra public satıcı profilini getirmek için bir metod
  // getPublicSellerProfileByUsername(username: string): Observable<User> {
  //   // Örnek endpoint: return this.apiService.get<User>(`/users/username/${username}/profile`);
  //   console.warn('getPublicSellerProfileByUsername: Backend endpoint not implemented yet.');
  //   return throwError(() => new Error('Backend endpoint for public seller profile not implemented.'));
  // }

  // TODO: Backend'e eklendikten sonra public satıcı profilini ID ile getirmek için bir metod
  // getPublicSellerProfileById(userId: number): Observable<User> {
  //   // Örnek endpoint: return this.apiService.get<User>(`/users/${userId}/public-profile`);
  //   console.warn('getPublicSellerProfileById: Backend endpoint not implemented yet.');
  //   return throwError(() => new Error('Backend endpoint for public seller profile not implemented.'));
  // }
}
