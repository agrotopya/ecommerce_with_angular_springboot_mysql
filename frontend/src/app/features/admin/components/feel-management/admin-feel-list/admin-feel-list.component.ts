import { Component, OnInit, inject, signal, ViewChild } from '@angular/core';
import { CommonModule, NgIf, NgFor, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http'; // HttpErrorResponse import edildi
import { FeelService } from '../../../../feels/services/feel.service'; // ../../../../feels/services/feel.service
import { FeelResponseDto } from '@shared/models/feel.model';
import { Page } from '@shared/models/page.model';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { PaginatorComponent } from '@shared/components/paginator/paginator.component';
import { NotificationService } from '@core/services/notification.service';
import { ConfirmationModalComponent } from '@shared/components/confirmation-modal/confirmation-modal.component'; // Import edildi

@Component({
  selector: 'app-admin-feel-list',
  standalone: true,
  imports: [
    CommonModule,
    NgIf,
    NgFor,
    RouterLink,
    LoadingSpinnerComponent,
    PaginatorComponent,
    DatePipe,
    ConfirmationModalComponent // Eklendi
  ],
  templateUrl: './admin-feel-list.component.html',
  styleUrls: ['./admin-feel-list.component.scss']
})
export class AdminFeelListComponent implements OnInit {
  private feelService = inject(FeelService);
  private notificationService = inject(NotificationService);

  feelsResponse = signal<Page<FeelResponseDto> | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  currentPage = signal(0);
  pageSize = signal(15);
  filterByActive = signal<boolean | undefined>(undefined); // undefined, true, false

  @ViewChild(ConfirmationModalComponent) deleteConfirmModal!: ConfirmationModalComponent;
  feelToDeleteId = signal<number | null>(null); // Silinecek feel ID'si

  ngOnInit(): void {
    this.loadAllFeels();
  }

  loadAllFeels(page: number = 0): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.currentPage.set(page);

    this.feelService.getAllFeelsForAdmin(page, this.pageSize(), this.filterByActive()).subscribe({
      next: (response) => {
        this.feelsResponse.set(response);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set('Failed to load feels for admin. Please try again.');
        this.notificationService.showError('Could not load feels for admin.');
        console.error('Error loading feels for admin:', err);
      }
    });
  }

  onPageChange(page: number): void {
    this.loadAllFeels(page);
  }

  applyFilter(status?: string): void {
    if (status === 'active') {
      this.filterByActive.set(true);
    } else if (status === 'inactive') {
      this.filterByActive.set(false);
    } else {
      this.filterByActive.set(undefined);
    }
    this.loadAllFeels(); // Filtre uygulandığında ilk sayfadan başla
  }

  toggleFeelStatus(feel: FeelResponseDto): void {
    const newStatus = !feel.active;
    this.feelService.updateFeelStatusByAdmin(feel.id, newStatus).subscribe({
      next: (updatedFeel) => {
        this.notificationService.showSuccess(`Feel status updated to ${newStatus ? 'Active' : 'Inactive'}.`);
        // Listeyi güncellemek için en basit yol yeniden yüklemek,
        // veya sadece güncellenen feel'i listede bulup değiştirmek.
        this.loadAllFeels(this.currentPage());
      },
      error: (err) => {
        // Hata mesajı serviste gösteriliyor.
        console.error('Error updating feel status:', err);
      }
    });
  }

  // Admin için silme işlemi
  promptDeleteFeel(feelId: number): void {
    this.feelToDeleteId.set(feelId);
    this.deleteConfirmModal.title = 'Confirm Feel Deletion (Admin)';
    this.deleteConfirmModal.message = `Are you sure you want to delete feel with ID: ${feelId}? This action cannot be undone.`;
    this.deleteConfirmModal.confirmButtonText = 'Delete Feel (Admin)';
    this.deleteConfirmModal.open();
  }

  handleDeleteConfirmation(confirmed: boolean): void {
    if (confirmed && this.feelToDeleteId() !== null) {
      this.isLoading.set(true);
      // Admin'in silme yetkisi olan bir servis metodu çağrılmalı.
      // Şimdilik genel deleteFeel kullanılıyor, backend yetkilendirmesi önemli.
      this.feelService.deleteFeelByAdmin(this.feelToDeleteId()!).subscribe({
        next: () => {
          this.notificationService.showSuccess('Feel deleted successfully by admin.');
          this.feelToDeleteId.set(null);
          // Listeyi yenile
          if (this.feelsResponse()?.content?.length === 1 && this.currentPage() > 0) {
            this.loadAllFeels(this.currentPage() - 1);
          } else {
            this.loadAllFeels(this.currentPage());
          }
        },
        error: (err: HttpErrorResponse) => { // err tip eklendi
          this.isLoading.set(false);
          this.feelToDeleteId.set(null);
          console.error('Error deleting feel by admin:', err);
          // Hata mesajı NotificationService ile gösteriliyor (ErrorInterceptor veya servis içinde)
        }
      });
    } else {
      this.feelToDeleteId.set(null);
    }
  }
}
