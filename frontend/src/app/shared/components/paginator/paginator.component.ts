import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, computed, signal } from '@angular/core';
import { CommonModule, NgFor, NgIf } from '@angular/common';

@Component({
  selector: 'app-paginator',
  standalone: true,
  imports: [CommonModule, NgFor, NgIf],
  templateUrl: './paginator.component.html',
  styleUrls: ['./paginator.component.scss']
})
export class PaginatorComponent implements OnChanges {
  @Input() currentPage: number = 0; // 0-indexed
  @Input() totalPages: number = 0;
  @Input() itemsPerPage: number = 10;
  @Input() totalItems: number = 0;
  @Input() pageRangeDisplayed: number = 2; // Mevcut sayfanın her iki yanında kaç sayfa gösterileceği

  @Output() pageChange = new EventEmitter<number>();

  // Sayfa numaralarını hesaplamak için signal kullanalım
  pages = computed<(number | string)[]>(() => { // Açık tip tanımı eklendi
    const pageNumbers: (number | string)[] = [];
    const cp = this.currentPage; // Mevcut sayfa (0-indexed)
    const tp = this.totalPages; // Toplam sayfa
    const range = this.pageRangeDisplayed;

    if (tp <= (range * 2) + 3) { // Eğer toplam sayfa sayısı çok azsa, tümünü göster
      for (let i = 0; i < tp; i++) {
        pageNumbers.push(i);
      }
    } else {
      pageNumbers.push(0); // İlk sayfa her zaman

      let startRange = Math.max(1, cp - range);
      let endRange = Math.min(tp - 2, cp + range);

      if (cp - range <= 1) { // Eğer baştaysak, son aralığı genişlet
        endRange = Math.min(tp - 2, 1 + (range * 2));
      }
      if (cp + range >= tp - 2) { // Eğer sondaysak, baş aralığı genişlet
        startRange = Math.max(1, tp - 2 - (range * 2));
      }

      if (startRange > 1) {
        pageNumbers.push('...');
      }

      for (let i = startRange; i <= endRange; i++) {
        pageNumbers.push(i);
      }

      if (endRange < tp - 2) {
        pageNumbers.push('...');
      }

      if (tp > 1) { // Son sayfa her zaman (eğer 1'den fazlaysa)
        pageNumbers.push(tp - 1);
      }
    }
    return pageNumbers;
  });

  ngOnChanges(changes: SimpleChanges): void {
    // Input değişikliklerinde bir şey yapmaya gerek yok, computed signal otomatik güncellenecek.
    // Ancak, doğrulamalar eklenebilir.
    if (this.currentPage < 0) this.currentPage = 0;
    if (this.currentPage >= this.totalPages && this.totalPages > 0) this.currentPage = this.totalPages - 1;
  }

  goToPage(page: number | string): void {
    if (typeof page === 'number' && page >= 0 && page < this.totalPages) {
      this.pageChange.emit(page);
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.pageChange.emit(this.currentPage + 1);
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.pageChange.emit(this.currentPage - 1);
    }
  }

  // Şablonda kullanmak için yardımcı metod
  asNumber(value: any): number {
    return Number(value);
  }
}
