import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, NgIf, NgFor } from '@angular/common';
import { FeelService } from '../../services/feel.service';
import { FeelResponseDto } from '@shared/models/feel.model';
import { Page } from '@shared/models/page.model';
import { FeelCardComponent } from '../feel-card/feel-card.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { PaginatorComponent } from '@shared/components/paginator/paginator.component';
import { NotificationService } from '@core/services/notification.service';

@Component({
  selector: 'app-feel-list',
  standalone: true,
  imports: [
    CommonModule,
    NgIf,
    NgFor,
    FeelCardComponent,
    LoadingSpinnerComponent,
    PaginatorComponent
  ],
  templateUrl: './feel-list.component.html',
  styleUrls: ['./feel-list.component.scss']
})
export class FeelListComponent implements OnInit {
  private feelService = inject(FeelService);
  private notificationService = inject(NotificationService);

  feelsResponse = signal<Page<FeelResponseDto> | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  currentPage = signal(0);
  pageSize = signal(12); // Sayfa başına gösterilecek feel sayısı

  ngOnInit(): void {
    this.loadFeels();
  }

  loadFeels(page: number = 0): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.currentPage.set(page);

    this.feelService.getPublicFeels(page, this.pageSize()).subscribe({
      next: (response) => {
        this.feelsResponse.set(response);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set('Failed to load feels. Please try again.');
        this.notificationService.showError('Could not load feels.');
        console.error('Error loading feels:', err);
      }
    });
  }

  onPageChange(page: number): void {
    this.loadFeels(page);
  }

  // Beğenme işlevi FeelCardComponent içinde veya burada yönetilebilir.
  // Eğer burada yönetilecekse:
  // handleLikeToggle(feelId: number): void {
  //   // ... FeelService üzerinden like/unlike işlemleri ...
  //   // Sonrasında listeyi veya ilgili feel'i güncelle
  // }
}
