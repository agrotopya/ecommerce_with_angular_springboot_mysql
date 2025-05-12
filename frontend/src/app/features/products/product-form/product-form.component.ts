import { Component, OnInit, ElementRef, ViewChild, inject, signal } from "@angular/core"; // inject ve signal eklendi
import { CommonModule } from "@angular/common"; // CommonModule eklendi
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormArray } from "@angular/forms"; // ReactiveFormsModule eklendi
import { ActivatedRoute, Router } from "@angular/router"; // RouterLink kaldırıldı
import { MatSnackBar, MatSnackBarModule } from "@angular/material/snack-bar"; // MatSnackBarModule eklendi
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { MatChipsModule, MatChipInputEvent } from "@angular/material/chips"; // MatChipsModule eklendi
import { MatIconModule } from "@angular/material/icon";
import { MatButtonModule } from "@angular/material/button";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { COMMA, ENTER } from "@angular/cdk/keycodes";
import { switchMap, catchError, filter, startWith, map } from "rxjs/operators";
import { Observable, of } from "rxjs";
import { HttpParams, HttpErrorResponse } from "@angular/common/http"; // HttpErrorResponse eklendi

import { SellerService } from "../../seller/seller.service";
import { CategoryService } from "../../categories/category.service";
import { ProductRequest, Product, ProductImage, ProductAttributeDto } from "../../../shared/models/product.model";
import { CategoryResponseDto } from "../../../shared/models/category.model";
import { AuthService } from "../../../core/services/auth.service";
import { Role } from "../../../shared/enums/role.enum";
import { FeelService } from '../../feels/services/feel.service';
import { FeelResponseDto } from '../../../shared/models/feel.model';
import { Page } from '../../../shared/models/page.model'; // Page import edildi

@Component({
  selector: "app-product-form",
  standalone: true, // standalone yapıldı
  imports: [
    CommonModule,
    ReactiveFormsModule,
    // RouterLink kaldırıldı
    MatSnackBarModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule
    // HttpClientModule gerekirse eklenecek
  ],
  templateUrl: "./product-form.component.html",
  styleUrls: ["./product-form.component.scss"]
})
export class ProductFormComponent implements OnInit {
  // fb, productService, categoryService, route, router, snackBar, authService inject ile alınabilir.
  public Role = Role; // Role enum'ını template'de kullanmak için public yapıldı
  productForm!: FormGroup;
  isEditMode = false;
  productId: number | null = null;
  isLoading = false;
  pageTitle = "Create New Product";
  categories: CategoryResponseDto[] = [];
  readonly separatorKeysCodes: number[] = [ENTER, COMMA];
  tags: string[] = [];

  // For image previews
  imagePreviews: { url: string, file?: File, isPrimary: boolean, existingId?: number }[] = [];
  productFeels = signal<FeelResponseDto[]>([]); // Ürüne ait feel'ler için sinyal
  isLoadingFeels = signal(false);

  @ViewChild("tagInput") tagInput!: ElementRef<HTMLInputElement>;

  constructor(
    private fb: FormBuilder,
    private sellerService: SellerService, // productService -> sellerService
    private categoryService: CategoryService,
    private route: ActivatedRoute,
    public router: Router, // private -> public
    private snackBar: MatSnackBar,
    public authService: AuthService, // For role checks if needed for form fields
    private feelService: FeelService // FeelService inject edildi
  ) { }

  ngOnInit(): void {
    this.productForm = this.fb.group({
      name: ["", Validators.required],
      description: ["", Validators.required],
      price: [null, [Validators.required, Validators.min(0.01)]],
      stock: [0, [Validators.required, Validators.min(0)]], // stockQuantity -> stock
      categoryId: [null, Validators.required],
      brand: [""],
      sku: [""],
      // tags are handled separately by the MatChipsInput
      // images are handled separately
      attributes: this.fb.array([]),
      isActive: [true, Validators.required],
      isFeatured: [false]
    });

    this.loadCategories();

    this.route.paramMap.pipe(
      filter(params => params.has("id")),
      switchMap(params => {
        this.isEditMode = true;
        this.pageTitle = "Edit Product";
        this.productId = Number(params.get("id"));
        console.log('Product ID in Edit Mode:', this.productId); // productId kontrolü için log eklendi
        this.isLoading = true;
        // Paralel olarak ürün ve feel'leri yükleyebiliriz veya ürün yüklendikten sonra feel'leri.
        // Şimdilik ürün yüklendikten sonra feel'leri yükleyelim.
        return this.sellerService.getProductByIdForSeller(this.productId).pipe( // productService -> sellerService
          catchError(err => {
            this.isLoading = false;
            this.snackBar.open(`Error loading product: ${err.error?.message || "Unknown error"}`, "Close", { duration: 5000 });
            this.router.navigate([this.authService.hasRole(Role.ADMIN) ? "/admin/products" : "/seller/products"]); // ROLE_ADMIN -> Role.ADMIN ve /products -> /seller/products
            return of(null);
          })
        );
      })
    ).subscribe((product: Product | null) => { // product tipini Product | null olarak güncelledik
      if (product) {
        this.populateForm(product);
        if (this.isEditMode && product.id) {
          this.loadProductFeels(product.id);
        }
        this.isLoading = false;
      }
    });
  }

  loadCategories(): void {
    this.isLoading = true;
    // Fetch all active categories for the dropdown
    const params = new HttpParams().set("size", "1000").set("isActive", "true");
    this.categoryService.getAllCategories(params).subscribe({
      next: page => {
        this.categories = page.content;
        this.isLoading = false;
      },
      error: err => {
        this.snackBar.open(`Error loading categories: ${err.error?.message || "Unknown error"}`, "Close", { duration: 3000 });
        this.isLoading = false;
      }
    });
  }

  populateForm(product: Product): void { // ProductResponseDto -> Product
    this.productForm.patchValue({
      name: product.name,
      description: product.description,
      price: product.price,
      stock: product.stock, // stockQuantity -> stock
      categoryId: product.category?.id,
      brand: product.brand,
      sku: product.sku,
      isActive: product.active,
      isFeatured: product.isFeatured
    });
    this.tags = product.tags ? [...product.tags] : [];
    if (product.attributes) {
      product.attributes.forEach(attr => this.addExistingAttribute(attr));
    }
    if (product.productImages && product.productImages.length > 0) {
      this.imagePreviews = product.productImages.map(img => ({
        url: img.imageUrl,
        isPrimary: img.isPrimary,
        existingId: img.id
      }));
    } else if (product.mainImageUrl) { // imageUrl -> mainImageUrl
      this.imagePreviews = [{ url: product.mainImageUrl, isPrimary: true, existingId: undefined }];
    } else {
      this.imagePreviews = []; // Resim yoksa boş dizi
    }
  }

  loadProductFeels(productId: number): void {
    this.isLoadingFeels.set(true);
    this.feelService.getFeelsByProductId(productId, 0, 100).subscribe({ // productId.toString() kaldırıldı
      next: (feelsPage: Page<FeelResponseDto>) => { // feelsPage tipi eklendi
        this.productFeels.set(feelsPage.content);
        this.isLoadingFeels.set(false);
      },
      error: (err: HttpErrorResponse) => { // err tipi eklendi
        this.isLoadingFeels.set(false);
        this.snackBar.open(`Error loading product feels: ${err.error?.message || 'Unknown error'}`, 'Close', { duration: 3000 });
      }
    });
  }

  // --- Tag Management ---
  addTag(event: MatChipInputEvent): void {
    const value = (event.value || "").trim();
    if (value && !this.tags.includes(value)) {
      this.tags.push(value);
    }
    event.chipInput!.clear();
  }

  removeTag(tag: string): void {
    const index = this.tags.indexOf(tag);
    if (index >= 0) {
      this.tags.splice(index, 1);
    }
  }

  // --- Attribute Management ---
  get attributes(): FormArray {
    return this.productForm.get("attributes") as FormArray;
  }

  newAttribute(): FormGroup {
    return this.fb.group({
      name: ["", Validators.required],
      value: ["", Validators.required]
    });
  }

  addExistingAttribute(attribute: ProductAttributeDto): void {
    this.attributes.push(this.fb.group({
        name: [attribute.name, Validators.required],
        value: [attribute.value, Validators.required]
    }));
  }

  addAttribute(): void {
    this.attributes.push(this.newAttribute());
  }

  removeAttribute(index: number): void {
    this.attributes.removeAt(index);
  }

  // --- Image Management ---
  onFileSelected(event: Event): void {
    const files = (event.target as HTMLInputElement).files;
    if (files) {
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        if (file.type.startsWith("image/")) {
          const reader = new FileReader();
          reader.onload = (e: any) => {
            this.imagePreviews.push({ url: e.target.result, file: file, isPrimary: this.imagePreviews.length === 0 && !this.imagePreviews.some(p => p.isPrimary) });
          };
          reader.readAsDataURL(file);
        }
      }
      // Clear the input value to allow selecting the same file again if removed
      (event.target as HTMLInputElement).value = "";
    }
  }

  removeImage(index: number): void {
    const removedImage = this.imagePreviews.splice(index, 1)[0];
    // If the removed image was primary, and there are other images, make the new first one primary
    if (removedImage.isPrimary && this.imagePreviews.length > 0 && !this.imagePreviews.some(img => img.isPrimary)) {
        this.imagePreviews[0].isPrimary = true;
    }
  }

  setPrimaryImage(index: number): void {
    this.imagePreviews.forEach((img, i) => {
      img.isPrimary = (i === index);
    });
  }

  onSubmit(): void {
    if (this.productForm.invalid) {
      this.snackBar.open("Please fill all required fields correctly.", "Close", { duration: 3000 });
      return;
    }

    this.isLoading = true;
    const formValue = this.productForm.value;
    const productData: ProductRequest = { // ProductRequestDto -> ProductRequest
      ...formValue,
      tags: this.tags,
      // Image handling will be more complex, potentially involving multipart form data upload
      // For now, let's assume ProductImageDto structure is sent, and backend handles uploads separately or via URLs
      images: this.imagePreviews.map(p => ({
        id: p.existingId, // For existing images during update
        imageUrl: p.file ? "placeholder_for_new_upload" : p.url, // Backend needs to handle new file uploads
        isPrimary: p.isPrimary,
        // altText and displayOrder can be added if form fields exist
      }))
    };

    // TODO: Implement actual file upload logic.
    // This often requires a separate service call or using FormData.
    // For simplicity here, we are sending image URLs/placeholders.
    // A real implementation would upload files and then link their URLs.

    let saveObservable: Observable<Product>; // ProductResponseDto -> Product

    if (this.isEditMode && this.productId) {
      saveObservable = this.sellerService.updateProduct(this.productId, productData); // productService -> sellerService
    } else {
      saveObservable = this.sellerService.createProduct(productData); // productService -> sellerService
    }

    saveObservable.subscribe({
      next: (savedProduct: Product) => { // savedProduct tipini Product olarak güncelledik
        this.isLoading = false;
        this.snackBar.open(`Product "${savedProduct.name}" ${this.isEditMode ? "updated" : "created"} successfully!`, "Close", { duration: 3000 });
        // Satıcı kendi ürünlerini yönettiği için /seller/products'a yönlendirilmeli.
        // Admin ise /admin/products'a. Bu formun seller altında olduğunu varsayarak /seller/products'a yönlendirelim.
        // Eğer admin de bu formu kullanıyorsa, bu navigasyon daha dinamik olmalı.
        // Şimdilik, bu formun seller'a ait olduğunu varsayarak /seller/products'a yönlendirelim.
        // Admin için ayrı bir form veya bu formun admin tarafından da kullanılması durumunda navigasyon güncellenmeli.
        this.router.navigate(['/seller/products']);
      },
      error: (err) => {
        this.isLoading = false;
        this.snackBar.open(`Error ${this.isEditMode ? "updating" : "creating"} product: ${err.error?.message || "Unknown error"}`, "Close", { duration: 5000 });
      }
    });
  }

  // Cancel button navigation
  cancelForm(): void {
    // Bu formun seller altında olduğunu varsayarak /seller/products'a yönlendirelim.
    // Admin ise /admin/products'a yönlendirilmeli.
    // authService.hasRole(Role.ADMIN) kontrolü burada da yapılabilir.
    const targetRoute = this.authService.hasRole(Role.ADMIN) ? '/admin/products' : '/seller/products';
    this.router.navigate([targetRoute]);
  }

  // --- Feel Management ---
  manageProductFeels(): void {
    if (this.productId) {
      // TODO: Satıcının kendi feel'lerini yöneteceği sayfaya yönlendir (ürün ID'si ile)
      // Veya bir modal içinde feel ekleme/listeleme yapılabilir.
      // Şimdilik basit bir log ve snackbar mesajı.
      this.router.navigate(['/seller/feels/manage', this.productId]); // Örnek bir rota
      this.snackBar.open(`Navigating to manage feels for product ID: ${this.productId}`, "Close", { duration: 3000 });
    } else {
      this.snackBar.open("Please save the product first to manage its feels.", "Close", { duration: 3000 });
    }
  }

  // Yeni feel ekleme sayfasına yönlendirme (ürün ID'si ile)
  navigateToAddFeel(): void {
    if (this.productId) {
      // TODO: Satıcının yeni feel ekleyeceği forma yönlendir (ürün ID'si ile)
      // Bu form, CreateFeelComponent olabilir ve productId'yi alabilir.
      this.router.navigate(['/seller/feels/create'], { queryParams: { productId: this.productId } });
    } else {
      this.snackBar.open("Please save the product first to add a feel.", "Close", { duration: 3000 });
    }
  }
}
