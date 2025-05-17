// src/app/layout/header/header.component.ts
import { Component, inject, WritableSignal, Signal, signal } from '@angular/core'; // signal eklendi
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
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
export class HeaderComponent implements OnInit {
  authService = inject(AuthService);
  cartService = inject(CartService);
  private categoryService = inject(CategoryService); // Geri eklendi
  private router = inject(Router);

  isAuthenticated: WritableSignal<boolean> = this.authService.isAuthenticated;
  currentUser: WritableSignal<User | null> = this.authService.currentUser;
  totalCartItems: Signal<number> = this.cartService.totalCartItems;

  isUserDropdownOpen = signal<boolean>(false);
  isMobileMenuOpen = signal<boolean>(false);

  // Yeni kategori/mega menü için sinyaller ve özellikler
  categoriesForMegaMenu = signal<CategoryTreeNodeDto[]>([]);
  activeMegaMenuCategoryId = signal<number | null>(null);
  private megaMenuCloseTimer: any = null;
  private readonly MEGA_MENU_CLOSE_DELAY = 200; // ms cinsinden gecikme

  ngOnInit(): void {
    this.loadCategoriesForMegaMenu();
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
              // expanded özelliği artık kullanılmıyor, activeMegaMenuCategoryId kullanılacak
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

  openMegaMenu(category: CategoryTreeNodeDto): void {
    console.log('openMegaMenu for:', category.name, 'ID:', category.id);
    if (this.megaMenuCloseTimer) {
      clearTimeout(this.megaMenuCloseTimer);
      this.megaMenuCloseTimer = null;
      console.log('Mega menu close timer cancelled.');
    }
    this.activeMegaMenuCategoryId.set(category.id);
    console.log('activeMegaMenuCategoryId set to:', category.id);
  }

  scheduleCloseMegaMenu(): void {
    console.log('scheduleCloseMegaMenu called. Timer will fire in', this.MEGA_MENU_CLOSE_DELAY, 'ms to set activeMegaMenuCategoryId to null.');
    this.megaMenuCloseTimer = setTimeout(() => {
      console.log('Mega menu close timer fired. Setting activeMegaMenuCategoryId to null.');
      this.activeMegaMenuCategoryId.set(null);
    }, this.MEGA_MENU_CLOSE_DELAY);
  }

  cancelCloseMegaMenu(): void {
    console.log('cancelCloseMegaMenu called.');
    if (this.megaMenuCloseTimer) {
      clearTimeout(this.megaMenuCloseTimer);
      this.megaMenuCloseTimer = null;
      console.log('Mega menu close timer cancelled by cancelCloseMegaMenu.');
    }
  }

  // Alt kategorileri sütunlara bölmek için yardımcı metod
  getSubCategoryColumns(children: CategoryTreeNodeDto[] | undefined, columns: number = 3): CategoryTreeNodeDto[][] {
    if (!children || children.length === 0) {
      return [];
    }
    const itemsPerColumn = Math.ceil(children.length / columns);
    const result: CategoryTreeNodeDto[][] = [];
    for (let i = 0; i < columns; i++) {
      const start = i * itemsPerColumn;
      const end = start + itemsPerColumn;
      if (start < children.length) {
        result.push(children.slice(start, end));
      }
    }
    return result.filter(col => col.length > 0); // Boş sütunları filtrele
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
    this.isUserDropdownOpen.update((isOpen: boolean) => !isOpen); // isOpen parametresine tip eklendi
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(isOpen => !isOpen);
    // Mobil menü açıldığında, kullanıcı dropdown'ı açıksa onu kapatabiliriz.
    if (this.isMobileMenuOpen() && this.isUserDropdownOpen()) {
      this.isUserDropdownOpen.set(false);
    }
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen.set(false);
  }

  // Opsiyonel: Dışarıya tıklandığında dropdown'ı kapatmak için
  // @HostListener('document:click', ['$event'])
  // onDocumentClick(event: MouseEvent): void {
  //   const targetElement = event.target as HTMLElement;
  //   // Dropdown ve onu açan buton dışına tıklandıysa kapat
  //   if (this.isUserDropdownOpen() && !this.elementRef.nativeElement.contains(targetElement)) {
  //     this.isUserDropdownOpen.set(false);
  //   }
  // }
  // Not: HostListener için ElementRef inject etmek gerekebilir: private elementRef = inject(ElementRef);
  // Şimdilik bu kısmı eklemiyoruz, gerekirse sonra eklenebilir.
}
