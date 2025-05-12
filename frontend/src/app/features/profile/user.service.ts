// src/app/features/profile/user.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { User, UserProfileUpdateRequest } from '../../shared/models/user.model'; // UserResponse -> User
import { USER_ENDPOINTS } from '../../core/constants/api-endpoints';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiService = inject(ApiService);

  getUserProfile(): Observable<User> {
    return this.apiService.get<User>(USER_ENDPOINTS.ME);
  }

  updateUserProfile(updateRequest: UserProfileUpdateRequest): Observable<User> {
    return this.apiService.put<User>(USER_ENDPOINTS.ME, updateRequest);
  }

  // Şifre değiştirme gibi diğer kullanıcı işlemleri buraya eklenebilir
}
