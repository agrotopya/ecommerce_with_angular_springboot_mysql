import { Component, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common'; // DatePipe ve CurrencyPipe kaldırıldı
import { RouterLink } from '@angular/router';
import { FeelResponseDto } from '@shared/models/feel.model';
import { AuthService } from '@core/services/auth.service';
import { NotificationService } from '@core/services/notification.service';
// import { FeelService } from '../../services/feel.service'; // Gerekirse eklenecek

@Component({
  selector: 'app-feel-card',
  standalone: true,
  imports: [CommonModule, RouterLink, NgOptimizedImage], // DatePipe ve CurrencyPipe kaldırıldı
  templateUrl: './feel-card.component.html',
  styleUrls: ['./feel-card.component.scss']
})
export class FeelCardComponent {
  @Input({ required: true }) feel!: FeelResponseDto;
  // @Output() likeClicked = new EventEmitter<number>(); // Beğenme işlemi parent'ta yönetilecekse

  // private feelService = inject(FeelService); // Eğer beğenme işlemi burada yapılacaksa
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);

  // isLiked = signal(this.feel?.isLikedByCurrentUser || false); // Başlangıç değeri
  // isLiking = signal(false);

  // ngOnInit(): void {
  //   this.isLiked.set(this.feel?.isLikedByCurrentUser || false);
  // }

  // onLikeToggle(): void {
  //   if (!this.authService.isAuthenticated()) {
  //     this.notificationService.showInfo('Please login to like feels.');
  //     return;
  //   }
  //   this.isLiking.set(true);
  //   const operation$ = this.isLiked()
  //     ? this.feelService.unlikeFeel(this.feel.id)
  //     : this.feelService.likeFeel(this.feel.id);

  //   operation$.subscribe({
  //     next: () => {
  //       this.isLiked.update(liked => !liked);
  //       this.feel.likeCount = this.isLiked() ? this.feel.likeCount + 1 : this.feel.likeCount - 1;
  //       this.isLiking.set(false);
  //     },
  //     error: () => this.isLiking.set(false)
  //   });
  // }
}
