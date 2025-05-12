// src/app/features/home/home.component.ts
import { Component, OnInit, OnDestroy, inject, signal, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser, NgOptimizedImage } from '@angular/common'; // NgOptimizedImage eklendi
import { RouterModule } from '@angular/router';
import { ProductService } from '../products/product.service';
import { CategoryService } from '../categories/category.service';
import { Product } from '../../shared/models/product.model'; // ProductResponseDto -> Product
import { CategoryResponseDto } from '../../shared/models/category.model';
import { ProductCardComponent } from '../products/product-card/product-card.component';
import { HttpParams } from '@angular/common/http';
import { NotificationService } from '../../core/services/notification.service'; // NotificationService eklendi

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, ProductCardComponent, NgOptimizedImage], // NgOptimizedImage eklendi
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit, OnDestroy {
  private productService = inject(ProductService);
  private categoryService = inject(CategoryService);
  private notificationService = inject(NotificationService);
  private platformId = inject(PLATFORM_ID); // PLATFORM_ID inject edildi

  // Sinyaller
  heroProducts = signal<Product[]>([]); // heroProduct -> heroProducts, Product[] tipi
  heroProductImageUrl = signal<string>('assets/images/fibiyo-maskot-v2.png'); // Yeni maskot görseli yolu
  currentHeroIndex = signal(0); // Carousel için aktif ürün indeksi

  featuredCategories = signal<CategoryResponseDto[]>([]);
  popularProducts = signal<Product[]>([]); // ProductResponseDto -> Product
  newProducts = signal<Product[]>([]); // Yeni ürünler için, ProductResponseDto -> Product

  isLoadingCategories = signal(false);
  isLoadingPopularProducts = signal(false);
  isLoadingNewProducts = signal(false);

  private autoScrollInterval: any; // Otomatik kaydırma için interval ID'si
  private readonly autoScrollDelay = 5000; // 5 saniye

  // Kategori ikonları için manuel bir liste (backend'den gelenle birleştirilebilir veya direkt kullanılabilir)
  // Görseldeki ikonlara göre: Yemek, Oyuncak, Avantajlı Ürün, İndirim Kuponları, Kediler, Yeni Gelen Ürünler
  // Bunlar için backend'den gelen kategorilerle eşleştirme veya özel bir yapı gerekebilir.
  // Şimdilik backend'den gelen ilk 6 kategoriyi alacağız.
  categoryIcons = [
    { name: 'Yemek', icon: 'assets/icons/food.svg', slug: 'yemek' }, // Varsayımsal ikon yolları
    { name: 'Oyuncak', icon: 'assets/icons/toy.svg', slug: 'oyuncak' },
    { name: 'Avantajlı Ürün', icon: 'assets/icons/offer.svg', slug: 'avantajli-urunler' },
    { name: 'İndirim Kuponları', icon: 'assets/icons/coupon.svg', slug: 'kuponlar' }, // Bu özel bir sayfa olabilir
    { name: 'Kediler', icon: 'assets/icons/cat.svg', slug: 'kediler' }, // Kedi ürünleri kategorisi
    { name: 'Yeni Gelen Ürünler', icon: 'assets/icons/new.svg', slug: 'yeni-urunler' } // Bu bir filtre olabilir
  ];


  ngOnInit(): void {
    this.loadHeroProducts();
    this.loadFeaturedCategories();
    this.loadPopularProducts();
    this.loadNewProducts();
    if (isPlatformBrowser(this.platformId)) {
      this.startAutoScroll();
    }
  }

  ngOnDestroy(): void { // ngOnDestroy eklendi
    if (isPlatformBrowser(this.platformId)) {
      this.stopAutoScroll();
    }
  }

  startAutoScroll(): void { // startAutoScroll metodu eklendi
    this.stopAutoScroll(); // Önceki interval varsa temizle
    if (isPlatformBrowser(this.platformId)) {
      this.autoScrollInterval = setInterval(() => {
        this.nextHeroProduct();
      }, this.autoScrollDelay);
    }
  }

  stopAutoScroll(): void { // stopAutoScroll metodu eklendi
    if (this.autoScrollInterval) {
      clearInterval(this.autoScrollInterval);
      this.autoScrollInterval = null;
    }
  }

  loadHeroProducts(): void {
    // Carousel'de gösterilecek ürün sayısını (örn: 8) ve sıralamayı (en popülerler) güncelleyelim
    const params = new HttpParams().set('page', '0').set('size', '8').set('sort', 'averageRating,desc'); // createdAt,desc -> averageRating,desc
    this.productService.getPublicProducts(params).subscribe({
      next: (page) => {
        if (page.content && page.content.length > 0) {
          this.heroProducts.set(page.content); // heroProduct -> heroProducts
        }
      },
      error: (err) => {
        console.error('Error loading hero products:', err);
      }
    });
  }

  // Carousel kontrol metodları
  nextHeroProduct(): void {
    this.currentHeroIndex.update(index => (index + 1) % this.heroProducts().length);
  }

  prevHeroProduct(): void {
    this.currentHeroIndex.update(index => (index - 1 + this.heroProducts().length) % this.heroProducts().length);
  }

  get currentHeroProduct(): Product | null {
    const products = this.heroProducts();
    if (products.length === 0) {
      return null;
    }
    return products[this.currentHeroIndex()];
  }

  goToHeroProduct(index: number): void { // Gösterge noktaları için metod eklendi
    if (index >= 0 && index < this.heroProducts().length) {
      this.currentHeroIndex.set(index);
      this.stopAutoScroll(); // Kullanıcı manuel seçim yapınca otomatik kaydırmayı durdur
      // İsteğe bağlı: Bir süre sonra otomatik kaydırmayı tekrar başlatmak için bir zamanlayıcı kurulabilir.
      // Şimdilik, kullanıcı tekrar manuel navigasyon yapana veya sayfadan ayrılana kadar duracak.
      // Veya fare carousel'den çekildiğinde tekrar başlatılabilir (HTML'e (mouseleave) eklenerek).
    }
  }

  loadFeaturedCategories(): void {
    this.isLoadingCategories.set(true);
    // Header'daki gibi kök veya aktif kategorilerden ilk 6 tanesini alabiliriz.
    this.categoryService.getActiveCategories().subscribe({
      next: (categories) => {
        this.featuredCategories.set(categories.slice(0, 6)); // İlk 6 kategori
        this.isLoadingCategories.set(false);
      },
      error: (err) => {
        console.error('Error loading featured categories:', err);
        this.isLoadingCategories.set(false);
      }
    });
  }

  loadPopularProducts(): void { // Bu metod artık "En Çok Satanlar" için kullanılacak
    this.isLoadingPopularProducts.set(true);
    const params = new HttpParams()
      .set('page', '0')
      .set('size', '4') // Örnek: En çok satan ilk 4 ürün
      .set('sort', 'salesCount,desc'); // Satış miktarına göre sırala

    this.productService.getPublicProducts(params).subscribe({
      next: (page) => {
        this.popularProducts.set(page.content);
        this.isLoadingPopularProducts.set(false);
      },
      error: (err) => {
        console.error('Error loading popular products:', err);
        this.isLoadingPopularProducts.set(false);
      }
    });
  }

  loadNewProducts(): void {
    this.isLoadingNewProducts.set(true);
    const params = new HttpParams()
      .set('page', '0')
      .set('size', '4') // Örnek: 4 yeni ürün
      .set('sort', 'createdAt,desc'); // Yeni ürünler için oluşturulma tarihine göre sırala

    this.productService.getPublicProducts(params).subscribe({
      next: (page) => {
        this.newProducts.set(page.content);
        this.isLoadingNewProducts.set(false);
      },
      error: (err) => {
        console.error('Error loading new products:', err);
        this.isLoadingNewProducts.set(false);
      }
    });
  }
}
