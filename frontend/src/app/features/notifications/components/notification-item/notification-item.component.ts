import { Component, Input } from '@angular/core'; // Input eklendi
import { CommonModule } from '@angular/common';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
// Notification modelini import etmemiz gerekecek
// import { Notification } from '../../../shared/models/notification.model'; // Örnek yol

@Component({
  selector: 'app-notification-item',
  standalone: true,
  imports: [CommonModule, MatListModule, MatIconModule],
  templateUrl: './notification-item.component.html',
  styleUrl: './notification-item.component.scss'
})
export class NotificationItemComponent {
  // @Input() notification!: Notification; // Örnek Input
}
