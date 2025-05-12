import { Injectable, signal } from '@angular/core';
import { UiNotification, UiNotificationType } from '../../shared/models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly DEFAULT_DURATION = 5000; // 5 saniye
  notifications = signal<UiNotification[]>([]);

  constructor() { }

  showSuccess(message: string, duration?: number): void {
    this.addNotification(message, 'success', duration);
  }

  showError(message: string, duration?: number): void {
    this.addNotification(message, 'error', duration);
  }

  showInfo(message: string, duration?: number): void {
    this.addNotification(message, 'info', duration);
  }

  showWarning(message: string, duration?: number): void {
    this.addNotification(message, 'warning', duration);
  }

  private addNotification(message: string, type: UiNotificationType, duration?: number): void {
    const id = Date.now().toString() + Math.random().toString(36).substring(2);
    const newNotification: UiNotification = {
      id,
      message,
      type,
      duration: duration || this.DEFAULT_DURATION
    };

    this.notifications.update(currentNotifications => [...currentNotifications, newNotification]);

    if (newNotification.duration) {
      setTimeout(() => {
        this.removeNotification(id);
      }, newNotification.duration);
    }
  }

  removeNotification(id: string): void {
    this.notifications.update(currentNotifications =>
      currentNotifications.filter(n => n.id !== id)
    );
  }

  clearAll(): void {
    this.notifications.set([]);
  }
}
