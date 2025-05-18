// src/app/layout/admin-layout/admin-layout.component.ts
import { Component, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { NotificationContainerComponent } from '../../shared/components/notification-container/notification-container.component'; // Eklendi
// İleride admin header/sidebar bileşenleri import edilebilir
// import { AdminHeaderComponent } from './admin-header/admin-header.component';
// import { AdminSidebarComponent } from './admin-sidebar/admin-sidebar.component';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterModule, NotificationContainerComponent], // Eklendi // AdminHeaderComponent, AdminSidebarComponent eklenecek
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.scss']
})
export class AdminLayoutComponent {
  // Admin sidebar durumu için signal
  private sidebarCollapsed = signal(false);

  /**
   * Sidebar'ın daraltılmış durumda olup olmadığını döndürür
   */
  isSidebarCollapsed() {
    return this.sidebarCollapsed();
  }

  /**
   * Sidebar'ın görünümünü değiştirir
   */
  toggleSidebar() {
    this.sidebarCollapsed.update(collapsed => !collapsed);
  }
}
