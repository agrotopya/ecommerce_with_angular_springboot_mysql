// src/app/layout/header/header.component.ts
import { Component, inject, WritableSignal, Signal, signal, AfterViewInit, ElementRef, Renderer2, OnDestroy, HostListener, PLATFORM_ID, Inject } from '@angular/core'; // PLATFORM_ID ve Inject eklendi
import { CommonModule, isPlatformBrowser, DOCUMENT } from '@angular/common'; // isPlatformBrowser ve DOCUMENT eklendi
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../features/cart/cart.service';
import { User } from '../../shared/models/user.model';
import { Role } from '../../shared/enums/role.enum';
import { CategoryService } from '../../features/categories/category.service';
import { CategoryTreeNodeDto } from '../../shared/models/category.model';
import { OnInit } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
})
export class HeaderComponent implements OnInit, AfterViewInit, OnDestroy {
  authService = inject(AuthService);
  cartService = inject(CartService);
  private categoryService = inject(CategoryService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private elementRef = inject(ElementRef);
  private renderer = inject(Renderer2);

  isAuthenticated: WritableSignal<boolean> = this.authService.isAuthenticated;
  currentUser: WritableSignal<User | null> = this.authService.currentUser;
  totalCartItems: Signal<number> = this.cartService.totalCartItems;

  isUserDropdownOpen = signal<boolean>(false);
  isMobileMenuOpen = signal<boolean>(false);
  isCategoryDropdownOpen = signal<boolean>(false);

  // Kategori menüsü için sinyaller
  categoriesForMegaMenu = signal<CategoryTreeNodeDto[]>([]);

  // Aktif kategori ID'si (URL parametresinden gelen categoryId)
  activeUrlCategoryId = signal<number | null>(null);

  // Kategori navigasyon için değişkenler
  activeCategoryId = signal<number | null>(null);
  private categoryDropdownCloseTimer: any = null;
  private readonly CATEGORY_DROPDOWN_CLOSE_DELAY = 200; // ms cinsinden gecikme

  // Resize handler için cleanup
  private resizeListener: (() => void) | null = null;

  constructor(@Inject(PLATFORM_ID) private platformId: Object, @Inject(DOCUMENT) private document: Document) {}

  ngOnInit(): void {
    this.loadCategoriesForMegaMenu();
    if (isPlatformBrowser(this.platformId)) {
      this.checkActiveCategoryFromUrl();
      this.router.events.subscribe(() => {
        this.checkActiveCategoryFromUrl();
      });
    }
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => {
        this.updateElementHeights();
        this.document.addEventListener('click', this.positionUserDropdown.bind(this));
        this.resizeListener = this.renderer.listen('window', 'resize', () => {
          this.updateElementHeights();
          this.positionUserDropdown();
          this.positionCategoryDropdowns();
        });
      }, 100);
    }
  }

  ngOnDestroy(): void {
    if (isPlatformBrowser(this.platformId)) {
      if (this.resizeListener) {
        this.resizeListener();
      }
      this.document.removeEventListener('click', this.positionUserDropdown.bind(this));
    }
  }

  // Kullanıcı menüsü dropdown'ının konumunu ayarla
  private positionUserDropdown(): void {
    if (isPlatformBrowser(this.platformId)) {
      const userMenuTrigger = this.elementRef.nativeElement.querySelector('.user-menu .profile-action');
      const userDropdown = this.elementRef.nativeElement.querySelector('.user-menu .dropdown-menu') as HTMLElement;

      if (userMenuTrigger && userDropdown) {
        const rect = userMenuTrigger.getBoundingClientRect();
        userDropdown.style.right = `${this.document.defaultView!.innerWidth - rect.right}px`;
      }
    }
  }

  // Kategori dropdown'larının konumunu ayarla
  private positionCategoryDropdowns(): void {
    if (isPlatformBrowser(this.platformId)) {
      const categoryItems = this.elementRef.nativeElement.querySelectorAll('.main-category-item');
      categoryItems.forEach((item: HTMLElement) => {
        const trigger = item.querySelector('a') as HTMLElement;
        const dropdown = item.querySelector('.category-dropdown') as HTMLElement;

        if (trigger && dropdown) {
          this.renderer.listen(trigger, 'mouseenter', () => {
            const rect = trigger.getBoundingClientRect();
            dropdown.style.left = `${rect.left}px`;
            const dropdownWidth = dropdown.offsetWidth;
            if (rect.left + dropdownWidth > this.document.defaultView!.innerWidth) {
              dropdown.style.left = 'auto';
              dropdown.style.right = '0px';
            }
          });
        }
      });
    }
  }

  // Yükseklikleri dinamik olarak hesaplayıp CSS değişkenlerine ata
  @HostListener('window:load')
  private updateElementHeights(): void {
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => {
        const navbar = this.elementRef.nativeElement.querySelector('.navbar');
        if (navbar) {
          const navbarHeight = navbar.offsetHeight;
          this.document.documentElement.style.setProperty('--header-height', `${navbarHeight}px`);
        }

        const categoryBar = this.elementRef.nativeElement.querySelector('.category-navigation-bar');
        if (categoryBar) {
          const categoryHeight = categoryBar.offsetHeight;
          this.document.documentElement.style.setProperty('--category-height', `${categoryHeight}px`);
          const totalHeight = (navbar ? navbar.offsetHeight : 0) + categoryHeight;
          this.document.body.style.paddingTop = `${totalHeight}px`;
        } else if (navbar) {
          this.document.body.style.paddingTop = `${navbar.offsetHeight}px`;
        }
      }, 100);
    }
  }

  // URL'den aktif kategori ID'sini kontrol et
  checkActiveCategoryFromUrl(): void {
    if (isPlatformBrowser(this.platformId)) {
      const urlParams = new URLSearchParams(this.document.defaultView!.location.search);
      const categoryId = urlParams.get('categoryId');
      if (categoryId) {
        this.activeUrlCategoryId.set(Number(categoryId));
      } else {
        this.activeUrlCategoryId.set(null);
      }
    }
  }

  // Bir kategorinin aktif olup olmadığını kontrol et
  isCategoryActive(categoryId: number): boolean {
    return this.activeUrlCategoryId() === categoryId;
  }

  loadCategoriesForMegaMenu(): void {
    this.categoryService.findRootCategories().pipe(
      switchMap(rootCategories => {
        if (!rootCategories || rootCategories.length === 0) {
          return of([]);
        }
        const observables = rootCategories.map(rootCat =>
          this.categoryService.getSubCategories(rootCat.id).pipe(
            map(subCategories => ({
              ...rootCat,
              children: subCategories || [], // Alt kategori yoksa boş dizi
            } as CategoryTreeNodeDto))
          )
        );
        return forkJoin(observables);
      })
    ).subscribe({
      next: (categoriesWithData: CategoryTreeNodeDto[]) => {
        this.categoriesForMegaMenu.set(categoriesWithData);
      },
      error: (err: any) => {
        console.error('Error loading categories for mega menu:', err);
      }
    });
  }

  // Kategori dropdown kontrolleri
  setActiveCategoryId(categoryId: number): void {
    if (isPlatformBrowser(this.platformId)) {
      if (this.categoryDropdownCloseTimer) {
        clearTimeout(this.categoryDropdownCloseTimer);
        this.categoryDropdownCloseTimer = null;
      }
      this.activeCategoryId.set(categoryId);

      // Dropdown'ın konumunu güncelle
      setTimeout(() => this.positionCategoryDropdowns(), 10);
    }
  }

  scheduleCloseCategoryDropdown(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.categoryDropdownCloseTimer = setTimeout(() => {
        this.activeCategoryId.set(null);
      }, this.CATEGORY_DROPDOWN_CLOSE_DELAY);
    }
  }

  cancelCloseCategoryDropdown(): void {
    if (isPlatformBrowser(this.platformId)) {
      if (this.categoryDropdownCloseTimer) {
        clearTimeout(this.categoryDropdownCloseTimer);
        this.categoryDropdownCloseTimer = null;
      }
    }
  }

  // Kategori dropdown menüsünü açıp kapatma
  toggleCategoryDropdown(event: Event): void {
    event.stopPropagation(); // Event'in üst elementlere iletilmesini engelle
    this.isCategoryDropdownOpen.update(isOpen => !isOpen);

    // Kategori menüsü açıldığında, kullanıcı dropdown'ı açıksa onu kapat
    if (this.isCategoryDropdownOpen() && this.isUserDropdownOpen()) {
      this.isUserDropdownOpen.set(false);
    }
  }

  logout(): void {
    this.authService.logout();
  }

  isAdmin(): boolean {
    return this.authService.hasRole(Role.ADMIN);
  }

  isSeller(): boolean {
    return this.authService.hasRole(Role.SELLER);
  }

  onSearch(searchTerm: string): void {
    if (searchTerm.trim()) {
      this.router.navigate(['/products'], { queryParams: { search: searchTerm.trim() } });
    } else {
      this.router.navigate(['/products']); // Boş arama ise sadece ürünler sayfasına git
    }
  }

  toggleUserDropdown(): void {
    this.isUserDropdownOpen.update((isOpen: boolean) => !isOpen);

    // Kullanıcı menüsü açıldığında, kategori dropdown'ı açıksa kapat
    if (this.isUserDropdownOpen() && this.isCategoryDropdownOpen()) {
      this.isCategoryDropdownOpen.set(false);
    }
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(isOpen => !isOpen);
    // Mobil menü açıldığında, açık olan tüm dropdown'ları kapat
    if (this.isMobileMenuOpen()) {
      if (this.isUserDropdownOpen()) {
        this.isUserDropdownOpen.set(false);
      }
      if (this.isCategoryDropdownOpen()) {
        this.isCategoryDropdownOpen.set(false);
      }
    }
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen.set(false);
  }
}
