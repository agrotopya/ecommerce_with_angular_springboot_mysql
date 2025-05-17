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
import { FeelCardComponent } from '../feel-card/feel-card.component';

@Component({
  selector: 'app-feel-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    LoadingSpinnerComponent,
    FeelCardComponent,
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
  private readonly PAGE_SIZE = 2; // Test için düşürüldü
  private allDataLoaded = false;

  private subscriptions: Subscription[] = [];
  @ViewChild('feelsContainer') feelsContainerRef!: ElementRef<HTMLDivElement>;
  private intersectionObserver?: IntersectionObserver;
  private scrollTimeout: any;

  constructor() {
    console.log('FeelListComponent constructor called. Current URL:', this.router.url);
  }

  ngOnInit(): void {
    console.log('FeelListComponent ngOnInit called. Current URL:', this.router.url);
    this.titleService.setTitle('Fibiyo Feels | Keşfet');
    this.metaService.updateTag({ name: 'description', content: 'Fibiyo ürün videolarını keşfedin - Son trendler ve yeni ürünler hakkında kısa videolar.' });

    const paramsSubscription = this.route.params.subscribe(params => {
      const id = params['id'];
      console.log('FeelListComponent route params:', params);
      if (id && !isNaN(+id)) {
        console.log('FeelListComponent loading feel by ID:', id);
        this.loadFeelById(+id);
      } else {
        console.log('FeelListComponent loading initial feels list.');
        this.loadFeels(true);
      }
    });
    this.subscriptions.push(paramsSubscription);
  }

  ngAfterViewInit(): void {
    this.setupIntersectionObserver();
  }

  ngOnDestroy(): void {
    console.log('FeelListComponent ngOnDestroy called');
    this.subscriptions.forEach(sub => sub.unsubscribe());
    if (this.intersectionObserver) {
      this.intersectionObserver.disconnect();
    }
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
          this.page = 0; // Sayfalamayı sıfırla, çünkü tek bir feel yüklüyoruz, devamı olmayacak.
          this.allDataLoaded = true; // Tek bir feel yüklendiği için daha fazla veri yok.
          console.log('loadFeelById: allDataLoaded set to true.');
          setTimeout(() => this.observeFeelItems(), 0);
        } else {
          this.error.set('İstenen video bulunamadı.');
          this.notificationService.showError('İstenen video bulunamadı.');
        }
      },
      error: (err) => {
        console.error('Feel yüklenirken hata (loadFeelById):', err);
        this.error.set('Video yüklenirken bir hata oluştu.');
        this.notificationService.showError('Video yüklenemedi.');
      }
    });
    this.subscriptions.push(sub);
  }

  loadFeels(isInitialLoad: boolean = false): void {
    console.log('loadFeels called. isInitialLoad:', isInitialLoad, 'loadingMore:', this.loadingMore(), 'allDataLoaded:', this.allDataLoaded);
    if (this.loadingMore() || this.allDataLoaded) {
      if(this.allDataLoaded) console.log('loadFeels: Aborting, all data already loaded.');
      if(this.loadingMore()) console.log('loadFeels: Aborting, already loading more.');
      return;
    }

    if (isInitialLoad) {
      this.isLoading.set(true);
      this.page = 0;
      this.allDataLoaded = false; // İlk yüklemede sıfırla
      this.feels.set([]);
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
            if (!this.route.snapshot.params['id']) {
              this.updateUrlWithCurrentFeel(this.feels()[0].id);
            }
          }
          // Eğer dönen içerik PAGE_SIZE'dan azsa, tüm veriler yüklenmiş demektir.
          if (response.content.length < this.PAGE_SIZE) {
            this.allDataLoaded = true;
            console.log('loadFeels: All data loaded (less than page size).');
          }
          setTimeout(() => this.observeFeelItems(), 0);
        } else {
          this.allDataLoaded = true;
          console.log('loadFeels: All data loaded (empty content).');
        }
      },
      error: (err) => {
        this.error.set('Feels yüklenirken bir hata oluştu.');
        this.notificationService.showError('Feels yüklenemedi.');
        console.error('Error loading feels (loadFeels):', err);
      }
    });
    this.subscriptions.push(sub);
  }

  setupIntersectionObserver(): void {
    if (this.intersectionObserver) {
      this.intersectionObserver.disconnect();
    }

    const options = {
      root: this.feelsContainerRef?.nativeElement,
      rootMargin: '0px',
      threshold: 0.5
    };

    this.intersectionObserver = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        const feelItemWrapper = entry.target as HTMLElement;
        const feelIdAttr = feelItemWrapper.id.replace('feel-item-', '');
        const index = this.feels().findIndex(f => f.id.toString() === feelIdAttr);

        console.log('IntersectionObserver: Entry details', {
          isIntersecting: entry.isIntersecting,
          targetId: feelIdAttr,
          foundIndex: index,
          currentIndex: this.currentIndex()
        });

        if (entry.isIntersecting) {
          if (index !== -1 && index !== this.currentIndex()) {
            console.log('IntersectionObserver: Updating current index and URL', { newIndex: index, feelId: this.feels()[index].id });
            this.currentIndex.set(index);
            this.updateUrlWithCurrentFeel(this.feels()[index].id);
            this.trackViewEvent(this.feels()[index].id);
          }
          // Sonsuz kaydırma için kontrol: Son eleman göründüğünde yükle
          if (index !== -1 && index === this.feels().length - 1 && !this.loadingMore() && !this.allDataLoaded) {
            console.log('IntersectionObserver: Loading more feels (last item visible).');
            this.loadFeels();
          }
        }
      });
    }, options);

    if (this.feels().length > 0) {
        this.observeFeelItems();
    }
  }

  observeFeelItems(): void {
    if (!this.feelsContainerRef?.nativeElement || !this.intersectionObserver) {
      console.warn('IntersectionObserver: Container or observer not ready for observing items.');
      return;
    }
    const items = this.feelsContainerRef.nativeElement.querySelectorAll('.feel-item-wrapper');
    console.log('IntersectionObserver: Observing items count:', items.length);
    this.intersectionObserver.disconnect();
    items.forEach(item => this.intersectionObserver!.observe(item));
  }

  trackByFeelId(index: number, feel: FeelResponseDto): number {
    return feel.id;
  }

  onScroll(): void {
    clearTimeout(this.scrollTimeout);
    this.scrollTimeout = setTimeout(() => {
      if (!this.feelsContainerRef?.nativeElement) return;
      const container = this.feelsContainerRef.nativeElement;
      const threshold = 200;
      const atBottom = container.scrollTop + container.clientHeight >= container.scrollHeight - threshold;

      if (atBottom && !this.loadingMore() && !this.allDataLoaded) {
        console.log('Scrolled to bottom, loading more feels (onScroll)...');
        this.loadFeels();
      }
    }, 100);
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

  scrollToFeel(index: number): void {
    if (!this.feelsContainerRef?.nativeElement || index < 0 || index >= this.feels().length) return;
    const feelId = this.feels()[index].id;
    console.log('Scrolling to feel with ID:', feelId, 'at index:', index);
    const element = this.feelsContainerRef.nativeElement.querySelector(`#feel-item-${feelId}`);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  private updateUrlWithCurrentFeel(feelId: number): void {
    console.log('Updating URL with feel ID:', feelId);
    this.location.replaceState(`/feels/${feelId}`);
    const currentFeel = this.feels().find(f => f.id === feelId);
    if (currentFeel) {
      this.titleService.setTitle(`${currentFeel.title || 'Feel'} | Fibiyo Feels`);
      this.metaService.updateTag({ name: 'description', content: currentFeel.description || `Fibiyo Feel: ${currentFeel.title}` });
    }
  }

  private trackViewEvent(feelId: number): void {
    console.log(`Track view for feel ID: ${feelId}`);
  }

  @HostListener('window:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent): void {
    if (!this.feelsContainerRef?.nativeElement.contains(document.activeElement)) {
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
    this.touchStartY = 0;

    if (Math.abs(diffY) > 50) {
      if (diffY > 0 && this.currentIndex() < this.feels().length - 1) {
        this.scrollToFeel(this.currentIndex() + 1);
      } else if (diffY < 0 && this.currentIndex() > 0) {
        this.scrollToFeel(this.currentIndex() - 1);
      }
    }
  }
}
