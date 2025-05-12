import { Component, Input } from '@angular/core'; // Input eklendi (genellikle stats card'lar veri alır)
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card'; // Örnek Material modülü
import { MatIconModule } from '@angular/material/icon';   // Örnek Material modülü

@Component({
  selector: 'app-stats-card',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule], // Temel ve örnek Material importları
  templateUrl: './stats-card.component.html',
  styleUrls: ['./stats-card.component.scss'] // styleUrls olarak düzeltildi
})
export class StatsCardComponent {
  @Input() title: string = '';
  @Input() value: string | number = '';
  @Input() icon?: string;
  @Input() trend?: {
    direction: 'up' | 'down' | 'neutral';
    percentage?: number;
    label?: string;
  };
  @Input() link?: string; // Opsiyonel link
  @Input() linkLabel?: string = 'View Details';

  constructor() { }
}
