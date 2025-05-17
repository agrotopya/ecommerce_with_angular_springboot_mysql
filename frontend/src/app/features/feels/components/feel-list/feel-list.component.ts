import { Component, OnInit, OnDestroy, ViewChild, ElementRef, HostListener, AfterViewInit, inject, signal, WritableSignal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { Location } from '@angular/common';
import { Title, Meta } from '@angular/platform-browser';
import { Subscription, fromEvent, throwError } from 'rxjs';
import { debounceTime, throttleTime, catchError, finalize } from 'rxjs/operators';

import { FeelService } from '../../services/feel.service';
import { FeelResponseDto } from '@shared/models/feel.model';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { NotificationService } from '@core/services/notification.service';
import { FeelCardComponent } from '../feel-card/feel-card.component'; // FeelCardComponent import edildi

@Component({
  selector: 'app-feel-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    LoadingSpinnerComponent,
    FeelCardComponent, // FeelCardComponent imports'a eklendi
  ],
  templateUrl: './feel-list.component.html',
  styleUrls: ['./feel-list.component.scss']
})
export class FeelListComponent implements OnInit, AfterViewInit, OnDestroy {
  private feelService = inject(FeelService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private location = inject(Location);
  private titleService = inject(Title);
  private metaService = inject(Meta);

  feels: WritableSignal<FeelResponseDto[]> = signal([]);
  currentIndex = signal(0);
  isLoading = signal(true);
  error = signal<string | null>(null);
  loadingMore = signal(false);

  private page = 0;
  private readonly PAGE_SIZE = 5; // Bir seferde kaç feel yükleneceği
  private allDataLoaded = false;

  private subscriptions: Subscription[] = [];
  // IntersectionObserver veya scroll listener için
  @ViewChild('feelsContainer') feelsContainerRef!: ElementRef<HTMLDivElement>;
  private intersectionObserver?: IntersectionObserver;
  private videoElements: Map<number, HTMLVideoElement> = new Map(); // Bu muhtemelen kaldırılacak veya değişecek
  private scrollTimeout: any;


  constructor() {}

  ngOnInit(): void {
    this.titleService.setTitle('Fibiyo Feels | Keşfet');
    this.metaService.updateTag({ name: 'description', content: 'Fibiyo ürün videolarını keşfedin - Son trendler ve yeni ürünler hakkında kısa videolar.' });

    const paramsSubscription = this.route.params.subscribe(params => {
      const id = params['id'];
      if (id && !isNaN(+id)) {
        this.loadFeelById(+id);
      } else {
        this.loadFeels(true); // Initial load
      }
    });
    this.subscriptions.push(paramsSubscription);
  }

  ngAfterViewInit(): void {
    this.setupIntersectionObserver();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    if (this.intersectionObserver) {
      this.intersectionObserver.disconnect();
    }
    this.videoElements.forEach(video => {
      video.pause();
      video.src = ''; // Kaynakları temizle
    });
    this.videoElements.clear();
  }

  loadFeelById(id: number): void {
    this.isLoading.set(true);
    this.error.set(null);
    const sub = this.feelService.getFeelById(id).pipe(
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: (feel) => {
        if (feel) {
          this.feels.set([feel]);
          this.currentIndex.set(0);
          this.updateUrlWithCurrentFeel(feel.id);
          this.page = 0; // Sayfalamayı sıfırla
          this.allDataLoaded = false; // Daha fazla veri yüklenebilir
          // playCurrentVideo çağrısı kaldırıldı. Aktif video yönetimi FeelCardComponent'e veya
          // currentIndex değişikliğine bağlı olarak FeelCardComponent içinde ele alınacak.
          // setTimeout(() => this.playCurrentVideo(), 0);
        } else {
          this.error.set('İstenen video bulunamadı.');
        }
      },
      error: (err) => {
        console.error('Feel yüklenirken hata:', err);
        this.error.set('Video yüklenirken bir hata oluştu.');
        this.notificationService.showError('Video yüklenemedi.');
      }
    });
    this.subscriptions.push(sub);
  }

  loadFeels(isInitialLoad: boolean = false): void {
    if (this.loadingMore() || this.allDataLoaded) return;

    if (isInitialLoad) {
      this.isLoading.set(true);
      this.page = 0;
      this.allDataLoaded = false;
      this.feels.set([]); // İlk yüklemede listeyi temizle
    }
    this.loadingMore.set(true);
    this.error.set(null);

    const sub = this.feelService.getPublicFeels(this.page, this.PAGE_SIZE).pipe(
      finalize(() => {
        this.isLoading.set(false);
        this.loadingMore.set(false);
      })
    ).subscribe({
      next: (response) => {
        if (response.content && response.content.length > 0) {
          this.feels.update(currentFeels => [...currentFeels, ...response.content]);
          this.page++;
          if (isInitialLoad && this.feels().length > 0) {
            this.currentIndex.set(0);
            this.updateUrlWithCurrentFeel(this.feels()[0].id);
            // playCurrentVideo çağrısı kaldırıldı.
            // setTimeout(() => this.playCurrentVideo(), 0);
          }
        } else {
          this.allDataLoaded = true; // Daha fazla veri yok
        }
        // Observer'ları yeniden kurmaya gerek yok, yeni elemanlar için otomatik izleyecek
      },
      error: (err) => {
        this.error.set('Feels yüklenirken bir hata oluştu.');
        this.notificationService.showError('Feels yüklenemedi.');
        console.error('Error loading feels:', err);
      }
    });
    this.subscriptions.push(sub);
  }

  setupIntersectionObserver(): void {
    if (this.intersectionObserver) {
      this.intersectionObserver.disconnect();
    }

    const options = {
      root: this.feelsContainerRef?.nativeElement, // Kaydırılabilir alan
      rootMargin: '0px',
      threshold: 0.75 // Elemanın %75'i göründüğünde
    };

    this.intersectionObserver = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        // IntersectionObserver içindeki videoElement.play() ve pause() kaldırılıyor.
        // Bu mantık FeelCardComponent'e ait olacak.
        // const videoElement = entry.target.querySelector('video');
        const feelItemWrapper = entry.target as HTMLElement;
        const feelIdAttr = feelItemWrapper.id.replace('feel-item-', '');
        const index = this.feels().findIndex(f => f.id.toString() === feelIdAttr);


        if (entry.isIntersecting) {
          if (index !== -1 && index !== this.currentIndex()) {
            this.currentIndex.set(index);
            this.updateUrlWithCurrentFeel(this.feels()[index].id);
            this.trackViewEvent(this.feels()[index].id);
          }
          // videoElement?.play().catch(e => console.warn('Video play interrupted:', e));

          // Sonsuz kaydırma için kontrol
          // index'in geçerli olduğundan ve feels dizisinin sınırları içinde olduğundan emin ol
          if (index !== -1 && index === this.feels().length - 2 && !this.loadingMore() && !this.allDataLoaded) {
            this.loadFeels();
          }
        } else {
          // videoElement?.pause();
        }
      });
    }, options);

    this.observeFeelItems(); // Bu metodun çağrıldığından emin olalım.
  }

  observeFeelItems(): void {
    if (!this.feelsContainerRef?.nativeElement || !this.intersectionObserver) return;
    const items = this.feelsContainerRef.nativeElement.querySelectorAll('.feel-item-wrapper');
    // Önceki observer'ları temizle (eğer varsa ve yeniden çağrılıyorsa)
    this.intersectionObserver.disconnect();
    items.forEach(item => this.intersectionObserver!.observe(item));
  }

  // playCurrentVideo, onVideoEnded ve registerVideoElement metodları artık burada yönetilmeyecek.
  // Bu işlevler FeelCardComponent içinde veya farklı bir yaklaşımla ele alınacak.
  // videoElements map'i de bu durumda gereksizleşebilir.

  trackByFeelId(index: number, feel: FeelResponseDto): number {
    return feel.id;
  }

  onScroll(): void {
    // Basit bir scroll ile yükleme mantığı. IntersectionObserver daha iyi bir seçenek olabilir.
    // Bu metod, HTML'deki (scroll) event'ine bağlı.
    // Eğer IntersectionObserver düzgün çalışıyorsa bu metoda gerek kalmayabilir.
    // Şimdilik, scroll event'i çok sık tetikleneceği için debounce eklenebilir.
    clearTimeout(this.scrollTimeout);
    this.scrollTimeout = setTimeout(() => {
      const container = this.feelsContainerRef.nativeElement;
      const threshold = 200; // Scroll bitimine ne kadar kala yükleneceği (pixel)
      const atBottom = container.scrollTop + container.clientHeight >= container.scrollHeight - threshold;

      if (atBottom && !this.loadingMore() && !this.allDataLoaded) {
        console.log('Scrolled to bottom, loading more feels...');
        this.loadFeels();
      }
    }, 100); // 100ms debounce
  }

  navigateToPreviousFeel(): void {
    if (this.currentIndex() > 0) {
      this.scrollToFeel(this.currentIndex() - 1);
    }
  }

  navigateToNextFeel(): void {
    if (this.currentIndex() < this.feels().length - 1) {
      this.scrollToFeel(this.currentIndex() + 1);
    }
  }
  // scrollToFeel metodu .feel-item-wrapper'a göre güncellenmeli
  scrollToFeel(index: number): void {
    if (!this.feelsContainerRef?.nativeElement || index < 0 || index >= this.feels().length) return;
    const feelId = this.feels()[index].id;
    const element = this.feelsContainerRef.nativeElement.querySelector(`#feel-item-${feelId}`);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
      // IntersectionObserver'ın tetiklenmesi beklenir, bu da currentIndex'i günceller.
      // Eğer hemen güncelleme gerekiyorsa: this.currentIndex.set(index); this.updateUrlWithCurrentFeel(feelId);
    }
  }

  private updateUrlWithCurrentFeel(feelId: number): void {
    this.location.replaceState(`/feels/${feelId}`);
    const currentFeel = this.feels().find(f => f.id === feelId);
    if (currentFeel) {
      this.titleService.setTitle(`${currentFeel.title || 'Feel'} | Fibiyo Feels`);
      this.metaService.updateTag({ name: 'description', content: currentFeel.description || `Fibiyo Feel: ${currentFeel.title}` });
    }
  }

  private trackViewEvent(feelId: number): void {
    // this.feelService.incrementViewCount(feelId).subscribe(); // Serviste bu metod varsa
    console.log(`Track view for feel ID: ${feelId}`);
  }

  // HostListener'lar (Klavye ve Dokunma)
  @HostListener('window:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent): void {
    if (!this.feelsContainerRef?.nativeElement.contains(document.activeElement)) {
        // Sadece feels container focus olduğunda çalışsın (opsiyonel)
        // return;
    }
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      if (this.currentIndex() < this.feels().length - 1) {
        this.scrollToFeel(this.currentIndex() + 1);
      }
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      if (this.currentIndex() > 0) {
        this.scrollToFeel(this.currentIndex() - 1);
      }
    }
  }

  // Dokunma eventleri için basit bir implementasyon
  private touchStartY: number = 0;
  @HostListener('touchstart', ['$event'])
  handleTouchStart(event: TouchEvent): void {
    if (event.touches.length === 1) {
      this.touchStartY = event.touches[0].clientY;
    }
  }

  @HostListener('touchend', ['$event'])
  handleTouchEnd(event: TouchEvent): void {
    if (this.touchStartY === 0 || event.changedTouches.length === 0) return;
    const touchEndY = event.changedTouches[0].clientY;
    const diffY = this.touchStartY - touchEndY;
    this.touchStartY = 0; // Reset

    if (Math.abs(diffY) > 50) { // Minimum swipe mesafesi
      if (diffY > 0 && this.currentIndex() < this.feels().length - 1) { // Swipe Up
        this.scrollToFeel(this.currentIndex() + 1);
      } else if (diffY < 0 && this.currentIndex() > 0) { // Swipe Down
        this.scrollToFeel(this.currentIndex() - 1);
      }
    }
  }
}
