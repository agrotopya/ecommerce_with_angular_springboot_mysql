// src/app/layout/header/header.component.ts
import { Component, inject, WritableSignal, Signal, signal } from '@angular/core'; // signal eklendi
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router'; // Router import edildi
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../features/cart/cart.service';
import { User } from '../../shared/models/user.model'; // UserResponse -> User
import { Role } from '../../shared/enums/role.enum';
import { CategoryService } from '../../features/categories/category.service'; // CategoryService eklendi
import { CategoryResponseDto } from '../../shared/models/category.model'; // Import yolu ve tipi düzeltildi
import { OnInit } from '@angular/core'; // OnInit eklendi

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
})
export class HeaderComponent implements OnInit { // OnInit implement edildi
  authService = inject(AuthService);
  cartService = inject(CartService);
  private categoryService = inject(CategoryService); // CategoryService inject edildi
  private router = inject(Router);

  isAuthenticated: WritableSignal<boolean> = this.authService.isAuthenticated;
  currentUser: WritableSignal<User | null> = this.authService.currentUser;
  totalCartItems: Signal<number> = this.cartService.totalCartItems;

  isUserDropdownOpen = signal<boolean>(false);
  isMobileMenuOpen = signal<boolean>(false);

  categories = signal<CategoryResponseDto[]>([]); // Tip CategoryResponseDto[] olarak düzeltildi

  ngOnInit(): void {
    this.loadTopCategories();
  }

  loadTopCategories(): void {
    // Şimdilik aktif kök kategorileri veya en çok kullanılanları alabiliriz.
    // Örnek olarak findRootCategories kullanılıyor, findActiveCategories de olabilir.
    // Ya da özel bir "topNavCategories" endpoint'i de olabilir backend'de.
    this.categoryService.findRootCategories().subscribe({ // Veya getActiveCategories()
      next: (cats: CategoryResponseDto[]) => { // Tip eklendi
        // Belki ilk 6-7 kategoriyi göstermek isteyebiliriz.
        this.categories.set(cats.slice(0, 7)); // Örnek: İlk 7 kategori
      },
      error: (err: any) => { // Tip eklendi
        console.error('Error loading categories for header navigation:', err);
      }
    });
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
