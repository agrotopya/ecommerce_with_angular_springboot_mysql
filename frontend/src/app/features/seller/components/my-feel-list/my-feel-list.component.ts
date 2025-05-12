import { Component, OnInit, inject, signal, ViewChild } from '@angular/core'; // ViewChild eklendi
import { CommonModule, NgIf, NgFor } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FeelService } from '../../../feels/services/feel.service';
import { FeelResponseDto } from '@shared/models/feel.model';
import { Page } from '@shared/models/page.model';
import { FeelCardComponent } from '../../../feels/components/feel-card/feel-card.component'; // Genel kartı kullanabiliriz
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { PaginatorComponent } from '@shared/components/paginator/paginator.component';
import { NotificationService } from '@core/services/notification.service';
import { ConfirmationModalComponent } from '@shared/components/confirmation-modal/confirmation-modal.component'; // Import edildi

@Component({
  selector: 'app-my-feel-list',
  standalone: true,
  imports: [
    CommonModule,
    NgIf,
    NgFor,
    RouterLink,
    FeelCardComponent,
    LoadingSpinnerComponent,
    PaginatorComponent,
    ConfirmationModalComponent // Eklendi
  ],
  templateUrl: './my-feel-list.component.html',
  styleUrls: ['./my-feel-list.component.scss']
})
export class MyFeelListComponent implements OnInit {
  private feelService = inject(FeelService);
  private notificationService = inject(NotificationService);

  myFeelsResponse = signal<Page<FeelResponseDto> | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  currentPage = signal(0);
  pageSize = signal(10); // Sayfa başına gösterilecek feel sayısı

  showDeleteConfirmation = signal(false); // Bu artık modal component'i tarafından yönetilecek
  feelToDeleteId = signal<number | null>(null);

  @ViewChild(ConfirmationModalComponent) deleteConfirmModal!: ConfirmationModalComponent;

  ngOnInit(): void {
    this.loadMyFeels();
  }

  loadMyFeels(page: number = 0): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.currentPage.set(page);

    this.feelService.getMyFeels(page, this.pageSize()).subscribe({
      next: (response) => {
        this.myFeelsResponse.set(response);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set('Failed to load your feels. Please try again.');
        this.notificationService.showError('Could not load your feels.');
        console.error('Error loading my feels:', err);
      }
    });
  }

  onPageChange(page: number): void {
    this.loadMyFeels(page);
  }

  promptDeleteFeel(feelId: number): void {
    this.feelToDeleteId.set(feelId);
    // Modal'a mesaj ve başlık gönderilebilir
    this.deleteConfirmModal.title = 'Confirm Feel Deletion';
    this.deleteConfirmModal.message = `Are you sure you want to delete feel with ID: ${feelId}? This action cannot be undone.`;
    this.deleteConfirmModal.confirmButtonText = 'Delete Feel';
    this.deleteConfirmModal.open();
  }

  handleDeleteConfirmation(confirmed: boolean): void {
    if (confirmed && this.feelToDeleteId() !== null) {
      this.isLoading.set(true);
      this.feelService.deleteFeel(this.feelToDeleteId()!).subscribe({
        next: () => {
          this.notificationService.showSuccess('Feel deleted successfully.');
          this.feelToDeleteId.set(null);
          // Listeyi yenile
          if (this.myFeelsResponse()?.content?.length === 1 && this.currentPage() > 0) {
            this.loadMyFeels(this.currentPage() - 1);
          } else {
            this.loadMyFeels(this.currentPage());
          }
        },
        error: (err) => {
          this.isLoading.set(false);
          this.feelToDeleteId.set(null);
          // Hata mesajı NotificationService ile gösteriliyor (ErrorInterceptor veya servis içinde)
          console.error('Error deleting feel:', err);
        }
      });
    } else {
      this.feelToDeleteId.set(null); // Kullanıcı iptal etti veya ID null
    }
  }

  // cancelDelete metodu artık ConfirmationModalComponent içinde
}
