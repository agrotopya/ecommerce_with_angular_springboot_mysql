// src/app/shared/models/notification.model.ts

export interface UnreadNotificationCountDto {
  unreadCount: number;
}

export interface NotificationResponseDto { // Notification -> NotificationResponseDto
  id: number;
  userId?: number; // Opsiyonel yapıldı, apidocs'ta yok
  type: string; // Örn: 'ORDER_STATUS_UPDATE', 'NEW_PROMOTION', 'REVIEW_APPROVED'
  message: string;
  isRead: boolean;
  link?: string; // Bildirimin yönlendireceği link (örn: /orders/123)
  createdAt: string; // ISO Date string
}

// Page<Notification> için Page arayüzü page.model.ts'den kullanılabilir.

// For UI Toast Notifications
export type UiNotificationType = 'success' | 'error' | 'info' | 'warning';

export interface UiNotification {
  id: string; // Unique ID for a toast, e.g., timestamp + random
  message: string;
  type: UiNotificationType;
  duration?: number; // Optional duration in ms, defaults to a global value
  icon?: string; // Optional icon class or SVG path
  // title?: string; // Optional title for the notification
}
