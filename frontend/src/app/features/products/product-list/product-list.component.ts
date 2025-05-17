// src/app/features/products/product-list/product-list.component.ts
import { Component, inject, OnInit, signal, WritableSignal, computed } from '@angular/core'; // computed eklendi
import { CommonModule } from '@angular/common';
import { HttpParams } from '@angular/common/http'; // HttpParams importu eklendi
import { ProductService } from '../product.service';
import { CategoryService } from '../../categories/category.service'; // CategoryService eklendi
import { CategoryResponseDto } from '../../../shared/models/category.model'; // CategoryResponseDto eklendi
import { Product } from '../../../shared/models/product.model';
import { Page } from '../../../shared/models/page.model';
import { ProductCardComponent } from '../product-card/product-card.component';
import { ActivatedRoute, Router } from '@angular/router'; // Query params için
import { FormsModule } from '@angular/forms'; // NgModel için

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, ProductCardComponent, FormsModule],
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss'],
})
export class ProductListComponent implements OnInit {
  private productService = inject(ProductService);
  private categoryService = inject(CategoryService); // CategoryService inject edildi
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  productsResponse: WritableSignal<Page<Product> | null> = signal(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  // Filtreleme ve Sayfalama için
  currentPage = signal(0);
  pageSize = signal(12); // Sayfa başına ürün sayısı
  currentSort = signal('name,asc'); // Varsayılan sıralama
  currentSearchTerm = signal('');
  currentCategoryId = signal<number | undefined>(undefined);

  // Kategori filtresi, arama terimi vb. için ngModel değişkenleri
  searchTerm: string = '';
  selectedCategoryId: string = ''; // Kategori ID'si string olarak alınıp number'a çevrilecek
  selectedSort: string = 'name,asc'; // Sıralama için ngModel

  categories = signal<CategoryResponseDto[]>([]); // Kategori listesi için sinyal
  isLoadingCategories = signal(false); // Kategori yükleme durumu

  sortOptions = [
    { value: 'name,asc', label: 'Name (A-Z)' },
    { value: 'name,desc', label: 'Name (Z-A)' },
    { value: 'price,asc', label: 'Price (Low to High)' },
    { value: 'price,desc', label: 'Price (High to Low)' },
    { value: 'createdAt,desc', label: 'Newest First' },
    { value: 'createdAt,asc', label: 'Oldest First' }
  ];

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.currentPage.set(params['page'] ? +params['page'] : 0);
      this.pageSize.set(params['size'] ? +params['size'] : 12);
      this.selectedSort = params['sort'] || 'name,asc';
      this.currentSort.set(this.selectedSort);
      this.searchTerm = params['search'] || '';
      this.currentSearchTerm.set(this.searchTerm);
      this.selectedCategoryId = params['categoryId'] || '';
      this.currentCategoryId.set(this.selectedCategoryId ? +this.selectedCategoryId : undefined);

      this.loadProducts();
      this.loadCategories(); // Kategorileri yükle
    });
  }

  loadCategories(): void {
    this.isLoadingCategories.set(true);
    this.categoryService.getActiveCategories().subscribe({ // Veya findRootCategories()
      next: (cats) => {
        this.categories.set(cats);
        this.isLoadingCategories.set(false);
      },
      error: (err) => {
        console.error('Error loading categories for filter:', err);
        this.isLoadingCategories.set(false);
      }
    });
  }

  loadProducts(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    let params = new HttpParams()
      .set('page', this.currentPage().toString())
      .set('size', this.pageSize().toString())
      .set('sort', this.currentSort());

    const categoryId = this.currentCategoryId();
    if (categoryId != null) { // undefined veya null kontrolü
      params = params.set('categoryId', categoryId.toString());
    }

    const searchTerm = this.currentSearchTerm();
    if (searchTerm && searchTerm.trim() !== '') {
      params = params.set('search', searchTerm.trim());
    }

    this.productService.getPublicProducts(params).subscribe({
      next: (response) => {
        this.productsResponse.set(response);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Failed to load products. Please try again later.');
        this.isLoading.set(false);
        console.error('Error loading products:', err);
      }
    });
  }

  onPageChange(page: number): void {
    if (page >= 0 && page < (this.productsResponse()?.totalPages ?? 0)) {
      this.currentPage.set(page);
      this.updateUrlAndLoadProducts();
    }
  }

  onSearch(): void {
    this.currentPage.set(0); // Arama yapıldığında ilk sayfaya dön
    this.currentSearchTerm.set(this.searchTerm);
    this.updateUrlAndLoadProducts();
  }

  onCategoryChange(): void {
    this.currentPage.set(0);
    this.currentCategoryId.set(this.selectedCategoryId ? +this.selectedCategoryId : undefined);
    this.updateUrlAndLoadProducts();
  }

  onSortChange(): void {
    this.currentPage.set(0); // Sıralama değiştiğinde ilk sayfaya dön
    this.currentSort.set(this.selectedSort);
    this.updateUrlAndLoadProducts();
  }

  private updateUrlAndLoadProducts(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        page: this.currentPage(),
        size: this.pageSize(),
        sort: this.currentSort(),
        search: this.currentSearchTerm() || null, // Boşsa null yap
        categoryId: this.currentCategoryId() ?? null // Undefined ise null yap
      },
      queryParamsHandling: 'merge', // Mevcut query params'ları koru/birleştir
      replaceUrl: true // Tarayıcı geçmişinde yığılma yapma
    });
    // URL güncellendikten sonra ngOnInit içindeki queryParams.subscribe tetiklenip loadProducts'ı çağıracak.
    // Veya doğrudan this.loadProducts(); çağrılabilir ama subscribe daha reaktif.
  }

  // Sayfalama için array oluşturma (template'te *ngFor için)
  get pageNumbers(): number[] {
    const totalPages = this.productsResponse()?.totalPages ?? 0;
    return Array(totalPages).fill(0).map((x, i) => i);
  }

  pageTitle = computed(() => {
    const categoryId = this.currentCategoryId();
    const searchTerm = this.currentSearchTerm();
    const categories = this.categories();

    if (searchTerm) {
      return `Search Results for "${searchTerm}"`;
    }
    if (categoryId && categories.length > 0) {
      const selectedCat = categories.find(cat => cat.id === categoryId);
      return selectedCat ? selectedCat.name : 'Products';
    }
    return 'All Products';
  });
}
