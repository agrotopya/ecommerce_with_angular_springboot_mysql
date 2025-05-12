import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../../core/services/notification.service';
import { UiNotification, UiNotificationType } from '../../models/notification.model'; // UiNotificationType eklendi

@Component({
  selector: 'app-notification-container',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-container.component.html',
  styleUrls: ['./notification-container.component.scss']
})
export class NotificationContainerComponent {
  notificationService = inject(NotificationService);
  notifications = this.notificationService.notifications;

  constructor() {}

  // İkonları belirlemek için yardımcı fonksiyon
  getIconClass(notificationType: UiNotificationType): string {
    switch (notificationType) {
      case 'success':
        return 'fas fa-check-circle'; // Font Awesome class'ları varsayılıyor
      case 'error':
        return 'fas fa-times-circle';
      case 'info':
        return 'fas fa-info-circle';
      case 'warning':
        return 'fas fa-exclamation-triangle';
      default:
        return 'fas fa-bell'; // Varsayılan ikon
    }
  }

  // Bildirimi manuel olarak kaldırmak için
  removeNotification(id: string): void {
    this.notificationService.removeNotification(id);
  }
}
