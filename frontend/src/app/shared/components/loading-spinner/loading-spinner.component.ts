import { Component, Input } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule, NgIf],
  templateUrl: './loading-spinner.component.html',
  styleUrls: ['./loading-spinner.component.scss']
})
export class LoadingSpinnerComponent {
  @Input() isLoading: boolean = true; // Varsayılan olarak gösterilir
  @Input() message: string | null = null; // Opsiyonel mesaj
  @Input() size: 'small' | 'medium' | 'large' = 'medium'; // Spinner boyutu
  @Input() overlay: boolean = false; // Arka planı kaplayan bir overlay olarak mı gösterilsin?
}
