import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { HttpParams } from '@angular/common/http';
import { FeelService } from '../../../feels/services/feel.service';
import { ProductService } from '../../../products/product.service';
import { UserService } from '@core/services/user.service'; // Yorum kaldırıldı
import { FeelResponseDto } from '@shared/models/feel.model';
import { Product } from '@shared/models/product.model';
import { User } from '@shared/models/user.model';
import { Page } from '@shared/models/page.model';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { FeelCardComponent } from '../../../feels/components/feel-card/feel-card.component';
import { ProductCardComponent } from '../../../products/product-card/product-card.component';

@Component({
  selector: 'app-seller-profile',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    LoadingSpinnerComponent,
    FeelCardComponent,
    ProductCardComponent
  ],
  templateUrl: './seller-profile.component.html',
  styleUrls: ['./seller-profile.component.scss']
})
export class SellerProfileComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private titleService = inject(Title);
  private feelService = inject(FeelService);
  private productService = inject(ProductService);
  private userService = inject(UserService); // Yorum kaldırıldı

  sellerId = signal<number | null>(null);
  sellerProfile = signal<User | null>(null);
  sellerFeels = signal<FeelResponseDto[]>([]);
  sellerProducts = signal<Product[]>([]);

  isLoadingProfile = signal(false);
  isLoadingFeels = signal(false);
  isLoadingProducts = signal(false);

  feelsPage = 0;
  feelsPageSize = 6;
  allFeelsLoaded = false;

  productsPage = 0;
  productsPageSize = 8;
  allProductsLoaded = false;

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('sellerId');
      if (id) {
        this.sellerId.set(+id);
        this.titleService.setTitle(`Seller Profile | Fibiyo`);
        this.loadSellerProfile();
        this.loadSellerFeels(true);
        this.loadSellerProducts(true);
      }
    });
  }

  loadSellerProfile(): void {
    if (!this.sellerId()) return;
    this.isLoadingProfile.set(true);
    // TODO: Backend'de public satıcı profili için endpoint (örn: /users/{id}/public-profile) gerekli.
    // Şimdilik UserService'te böyle bir metod olmadığını varsayarak,
    // feel'lerden gelen sellerUsername'i kullanmaya devam ediyoruz.
    // Eğer UserService'te getPublicSellerProfileById gibi bir metod varsa o kullanılmalı.
    // this.userService.getPublicSellerProfileById(this.sellerId()!).subscribe({
    //   next: (profile) => {
    //     this.sellerProfile.set(profile);
    //     this.titleService.setTitle(`${profile.username}'s Profile | Fibiyo`);
    //     this.isLoadingProfile.set(false);
    //   },
    //   error: () => {
    //     console.error(`Failed to load profile for seller ID: ${this.sellerId()}`);
    //     this.isLoadingProfile.set(false);
    //     // Hata durumunda geçici başlık
    //     if (this.sellerFeels().length > 0) {
    //        const sellerUsername = this.sellerFeels()[0].sellerUsername;
    //        this.titleService.setTitle(`${sellerUsername}'s Profile (Error) | Fibiyo`);
    //        this.sellerProfile.set({ username: sellerUsername, id: this.sellerId()! } as User);
    //     }
    //   }
    // });
    console.warn('loadSellerProfile: Using temporary seller profile data from feels. Implement public seller profile endpoint.');
    if (this.sellerId() && this.sellerFeels().length > 0 && !this.sellerProfile()) {
        const sellerUsername = this.sellerFeels()[0].sellerUsername;
        this.titleService.setTitle(`${sellerUsername}'s Profile | Fibiyo`);
        this.sellerProfile.set({ username: sellerUsername, id: this.sellerId()! } as User);
    } else if (this.sellerId() && !this.sellerProfile()) {
        this.titleService.setTitle(`Seller ${this.sellerId()}'s Profile | Fibiyo`);
    }
    this.isLoadingProfile.set(false);
  }

  loadSellerFeels(isInitialLoad: boolean = false): void {
    if (!this.sellerId() || this.isLoadingFeels() || (this.allFeelsLoaded && !isInitialLoad)) return;

    if (isInitialLoad) {
      this.feelsPage = 0;
      this.allFeelsLoaded = false;
      this.sellerFeels.set([]);
    }
    this.isLoadingFeels.set(true);
    this.feelService.getFeelsBySeller(this.sellerId()!, this.feelsPage, this.feelsPageSize)
      .subscribe({
        next: (page: Page<FeelResponseDto>) => {
          this.sellerFeels.update(current => [...current, ...page.content]);
          this.feelsPage++;
          if (page.last) {
            this.allFeelsLoaded = true;
          }
          this.isLoadingFeels.set(false);
          if (isInitialLoad && this.sellerFeels().length > 0 && !this.sellerProfile()) {
             const sellerUsername = this.sellerFeels()[0].sellerUsername;
             this.titleService.setTitle(`${sellerUsername}'s Profile | Fibiyo`);
             this.sellerProfile.set({ username: sellerUsername, id: this.sellerId()! } as User);
          }
        },
        error: () => {
          this.isLoadingFeels.set(false);
        }
      });
  }

  loadSellerProducts(isInitialLoad: boolean = false): void {
    if (!this.sellerId() || this.isLoadingProducts() || (this.allProductsLoaded && !isInitialLoad)) return;

    if (isInitialLoad) {
      this.productsPage = 0;
      this.allProductsLoaded = false;
      this.sellerProducts.set([]);
    }
    this.isLoadingProducts.set(true);
    const params = new HttpParams()
      .set('sellerId', this.sellerId()!.toString())
      .set('page', this.productsPage.toString())
      .set('size', this.productsPageSize.toString())
      .set('sort', 'createdAt,desc');

    this.productService.getPublicProducts(params)
      .subscribe({
        next: (page: Page<Product>) => {
          this.sellerProducts.update(current => [...current, ...page.content]);
          this.productsPage++;
          if (page.last) {
            this.allProductsLoaded = true;
          }
          this.isLoadingProducts.set(false);
        },
        error: () => {
          this.isLoadingProducts.set(false);
        }
      });
  }
}
