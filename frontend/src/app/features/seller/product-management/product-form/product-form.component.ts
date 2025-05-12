// src/app/features/seller/product-management/product-form/product-form.component.ts
import { Component, inject, OnInit, signal, WritableSignal } from '@angular/core'; // WritableSignal eklendi
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { SellerService } from '../../seller.service'; // SellerService kullanacağız
import { Product, ProductRequest } from '../../../../shared/models/product.model';
import { CategoryService } from '../../../categories/category.service'; // Kategori seçimi için
import { CategoryResponseDto } from '../../../../shared/models/category.model'; // .models -> .model
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './product-form.component.html',
  styleUrls: ['./product-form.component.scss']
})
export class ProductFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private sellerService = inject(SellerService);
  private categoryService = inject(CategoryService);
  public router = inject(Router); // public yapılmıştı, öyle kalsın
  private route = inject(ActivatedRoute);

  productForm: FormGroup;
  isEditMode = signal(false);
  productId = signal<number | null>(null);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  categories: WritableSignal<CategoryResponseDto[]> = signal([]); // Kategori dropdown için

  selectedFile: File | null = null;
  imagePreviewUrl: string | ArrayBuffer | null = null;

  constructor() {
    this.productForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      price: [null, [Validators.required, Validators.min(0.01)]],
      stock: [0, [Validators.required, Validators.min(0)]], // Modelde 'stock' olarak güncellendi
      sku: [''],
      categoryId: [null, [Validators.required]],
      active: [true] // Modelde 'active' olarak güncellendi
    });
  }

  ngOnInit(): void {
    this.loadCategories(); // Kategori dropdown'ı doldur
    this.route.paramMap.subscribe(params => {
      const id = params.get('productId');
      if (id) {
        this.isEditMode.set(true);
        this.productId.set(+id);
        this.loadProductForEdit(+id);
      }
    });
  }

  loadCategories(): void {
    this.categoryService.getActiveCategories().subscribe({
      next: (cats) => this.categories.set(cats),
      error: (err) => {
        console.error('Failed to load categories for product form:', err);
        this.errorMessage.set('Could not load categories. Please try again.');
      }
    });
  }

  loadProductForEdit(id: number): void {
    this.isLoading.set(true);
    this.sellerService.getProductByIdForSeller(id).subscribe({
      next: (product) => {
        this.productForm.patchValue({
          name: product.name,
          description: product.description,
          price: product.price,
          stock: product.stock, // stockQuantity -> stock
          sku: product.sku,
          categoryId: product.categoryId,
          active: product.active // isActive -> active
        });
        // Image preview for existing product
        if (product.mainImageUrl) {
          this.imagePreviewUrl = product.mainImageUrl;
        } else if (product.productImages && product.productImages.length > 0) {
          this.imagePreviewUrl = product.productImages[0].imageUrl;
        } else {
          this.imagePreviewUrl = null;
        }
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(err.error?.message || 'Failed to load product details.');
        this.isLoading.set(false);
        console.error('Error loading product for edit:', err);
      }
    });
  }

  onFileSelected(event: Event): void {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;
    if (fileList && fileList[0]) {
      this.selectedFile = fileList[0];

      // Önizleme için
      const reader = new FileReader();
      reader.onload = () => {
        this.imagePreviewUrl = reader.result;
      };
      reader.readAsDataURL(this.selectedFile);

      // TODO: Resmi hemen yüklemek yerine, form submit edildiğinde ürün ID'si ile birlikte yükle
      // Eğer ürün yeni oluşturuluyorsa, önce ürün oluşturulmalı, sonra ID ile resim yüklenmeli.
      // Eğer ürün düzenleniyorsa, mevcut ürün ID'si ile resim yüklenebilir.
      // this.uploadImage(); // Otomatik yükleme isteniyorsa
    } else {
      this.selectedFile = null;
      this.imagePreviewUrl = null;
    }
  }

  // uploadImage(): void {
  //   if (!this.selectedFile || !this.productId()) { // productId edit modunda dolu olmalı
  //     this.errorMessage.set('Please select an image and ensure product is saved.');
  //     return;
  //   }
  //   const formData = new FormData();
  //   formData.append('image', this.selectedFile, this.selectedFile.name);

  //   this.isLoading.set(true);
  //   this.sellerService.uploadProductImage(this.productId()!, formData).subscribe({
  //     next: (updatedProduct) => {
  //       this.isLoading.set(false);
  //       this.successMessage.set('Image uploaded successfully!');
  //       // Formdaki imageUrl'i güncellemek veya sayfayı yenilemek gerekebilir
  //       // this.productForm.patchValue({ imageUrl: updatedProduct.imageUrl });
  //       if (this.imagePreviewUrl && typeof this.imagePreviewUrl === 'string' && updatedProduct.imageUrl) {
  //         this.imagePreviewUrl = updatedProduct.imageUrl; // Eğer backend tam URL dönüyorsa
  //       }
  //     },
  //     error: (err: HttpErrorResponse) => {
  //       this.isLoading.set(false);
  //       this.errorMessage.set(err.error?.message || 'Failed to upload image.');
  //       console.error('Error uploading image:', err);
  //     }
  //   });
  // }

  onSubmit(): void {
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const productData: ProductRequest = this.productForm.value;
    productData.categoryId = +productData.categoryId;
    // productData.images alanı ProductRequest'te yok, resim yükleme ayrı handle ediliyor.


    if (this.isEditMode() && this.productId()) {
      this.sellerService.updateProduct(this.productId()!, productData).subscribe({
        next: (updatedProduct) => { // Backend'den güncellenmiş ürün dönebilir
          this.isLoading.set(false);
          this.successMessage.set('Product updated successfully!');
          // Eğer resim seçilmişse ve ürün güncellendiyse, resmi yükle
          if (this.selectedFile && updatedProduct.id) {
            this.uploadSelectedImage(updatedProduct.id);
          } else {
            // this.router.navigate(['/seller/products']); // İsteğe bağlı yönlendirme
          }
        },
        error: (err: HttpErrorResponse) => {
          this.isLoading.set(false);
          this.errorMessage.set(err.error?.message || 'Failed to update product.');
          console.error('Error updating product:', err);
        }
      });
    } else { // Yeni ürün oluşturma
      this.sellerService.createProduct(productData).subscribe({
        next: (createdProduct) => {
          this.isLoading.set(false);
          this.successMessage.set('Product created successfully!');
          this.productForm.reset({ active: true, categoryId: null }); // isActive -> active
          // Eğer resim seçilmişse ve ürün oluşturulduysa, resmi yükle
          if (this.selectedFile && createdProduct.id) {
            this.productId.set(createdProduct.id); // Yeni ürünün ID'sini al
            this.uploadSelectedImage(createdProduct.id);
          } else {
            this.imagePreviewUrl = null; // Form sıfırlandığı için önizlemeyi de temizle
            this.selectedFile = null;
          }
          // this.router.navigate(['/seller/products']); // İsteğe bağlı yönlendirme
        },
        error: (err: HttpErrorResponse) => {
          this.isLoading.set(false);
          this.errorMessage.set(err.error?.message || 'Failed to create product.');
          console.error('Error creating product:', err);
        }
      });
    }
  }

  uploadSelectedImage(productId: number): void {
    if (!this.selectedFile) return;

    const formData = new FormData();
    formData.append('file', this.selectedFile, this.selectedFile.name); // 'imageFile' -> 'file' (apidocs.md'ye göre)

    // Yükleme için ayrı bir isLoading state'i kullanılabilir
    // this.isUploadingImage.set(true);
    this.sellerService.uploadProductImage(productId, formData).subscribe({
      next: (productWithImage) => {
        // this.isUploadingImage.set(false);
        this.successMessage.set(this.successMessage() + ' Image uploaded successfully!'); // Mevcut mesaja ekle
        // productWithImage backend'den Product tipinde geliyor ve mainImageUrl alanı var.
        this.imagePreviewUrl = productWithImage.mainImageUrl || this.imagePreviewUrl; // Güncel resmi göster
        this.selectedFile = null; // Dosya seçimi sıfırla
        if (this.isEditMode()) {
          // Düzenleme modunda kal, belki başka değişiklikler yapmak ister
        } else {
          // Yeni ürün eklendiyse ve resim yüklendiyse, belki listeye yönlendir
          // this.router.navigate(['/seller/products']);
        }
      },
      error: (err: HttpErrorResponse) => {
        // this.isUploadingImage.set(false);
        this.errorMessage.set((this.errorMessage() ? this.errorMessage() + ' ' : '') + (err.error?.message || 'Failed to upload image.'));
        console.error('Error uploading image for product ' + productId, err);
      }
    });
  }
}
