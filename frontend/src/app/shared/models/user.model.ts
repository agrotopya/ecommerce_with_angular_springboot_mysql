// src/app/shared/models/user.model.ts
// import { Role } from '../enums/role.enum'; // Bu import kaldırıldı.

// src/app/shared/models/user.model.ts
export interface JwtAuthResponse {
  accessToken: string;
  tokenType: string; // Genellikle 'Bearer', apidocs.md'ye göre zorunlu
  userId: number;    // apidocs.md'ye göre zorunlu
  username: string;  // apidocs.md'ye göre zorunlu
  email: string;     // apidocs.md'ye göre zorunlu
  roles: string[];   // apidocs.md'ye göre zorunlu
  // refreshToken ve user alanları apidocs.md'de yok, kaldırıldı.
  // firstName ve lastName alanları apidocs.md'de yok, kaldırıldı. Bunlar UserResponse'dan gelecek.
}

// UserResponse tanımı auth.models.ts'den ve apidocs.md'den gelen bilgilerle güncellendi
export interface User {
  id: number; // API dokümanına ve auth.models.ts'e göre number
  username: string;
  email: string;
  firstName?: string; // apidocs.md'de var, auth.models.ts'de var
  lastName?: string;  // apidocs.md'de var, auth.models.ts'de var
  roles: string[]; // API dokümanına ve auth.models.ts'e göre string[]
  active: boolean; // isActive -> active olarak backend yanıtına göre düzeltildi
  avatar?: string; // apidocs.md'de var, auth.models.ts'de var
  createdAt: string; // Or Date, apidocs.md'de var, auth.models.ts'de var
  updatedAt: string; // Or Date, apidocs.md'de var, auth.models.ts'de var
  authProvider: 'LOCAL' | 'GOOGLE'; // apidocs.md'de var, auth.models.ts'de var
  subscriptionType?: string | null; // apidocs.md'de var, auth.models.ts'de var (nullable olabilir)
  subscriptionExpiryDate?: string | null; // Or Date, apidocs.md'de var, auth.models.ts'de var
  loyaltyPoints?: number; // apidocs.md'de var, auth.models.ts'de var
  imageGenQuota?: number | null; // apidocs.md'de var, auth.models.ts'de var
}

export interface LoginRequest {
  usernameOrEmail: string;
  password?: string;
  provider?: string;
  idToken?: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password?: string;
  firstName?: string; // apidocs.md'ye göre RegisterRequest'te var
  lastName?: string;  // apidocs.md'ye göre RegisterRequest'te var
  provider?: 'LOCAL' | 'GOOGLE'; // AuthProvider enum yerine string literalleri
  role?: 'CUSTOMER' | 'SELLER'; // Role enum yerine string literalleri (apidocs.md RegisterRequest'te role belirtmiyor, ama AuthModule'deki RegisterComponent'te vardı)
                                // API dokümanındaki RegisterRequest'e göre bu alan olmayabilir. Şimdilik tutuyoruz.
}

// ApiResponse tanımı auth.models.ts'den alındı ve generik yapıldı
export interface ApiResponse<T = any> {
  success: boolean;
  message: string;
  data?: T;
}

export interface UserProfileUpdateRequest {
  firstName?: string;
  lastName?: string;
  // Avatar güncelleme gibi diğer alanlar eklenebilir
}

// auth.models.ts'den taşınan arayüzler:
export interface ForgotPasswordRequest { email: string; }
export interface ResetPasswordRequest { token: string; newPassword?: string; } // newPassword zorunlu olmalı
export interface ChangePasswordRequest { currentPassword?: string; newPassword?: string; }
// export interface UnreadNotificationCountDto { unreadCount: number; } // Bu notification.model.ts'e taşınacak
