// src/app/features/seller/product-management/product-form/product-form.component.ts
import { Component, inject, OnInit, signal, WritableSignal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms'; // FormsModule eklendi
import { ActivatedRoute, Router } from '@angular/router';
import { SellerService } from '../../seller.service';
import { Product, ProductRequest, ProductImage } from '../../../../shared/models/product.model';
import { CategoryService } from '../../../categories/category.service';
import { CategoryResponseDto } from '../../../../shared/models/category.model';
import { HttpErrorResponse } from '@angular/common/http';
import { AiImageGenerationRequest, AiImageGenerationResponse, SetAiImageAsProductRequest } from '../../../../shared/models/ai.model'; // AI DTO'ları eklendi

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule], // FormsModule eklendi
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

  // Hiyerarşik kategori seçimi için
  rootCategories: WritableSignal<CategoryResponseDto[]> = signal([]);
  childCategories: WritableSignal<CategoryResponseDto[]> = signal([]);
  selectedRootCategoryId: WritableSignal<number | null> = signal(null);
  // categories sinyali artık kullanılmayacak, rootCategories ve childCategories kullanılacak.

  // Çoklu görsel için
  selectedFiles: File[] = [];
  newImagePreviews: { url: string | ArrayBuffer | null, file: File }[] = [];
  existingProductImages = signal<ProductImage[]>([]); // ProductImageDto -> ProductImage

  // Eski tekil görsel property'leri kaldırılacak veya yorum satırı yapılacak
  // selectedFile: File | null = null;
  // imagePreviewUrl: string | ArrayBuffer | null = null;

  // AI Image Generation Properties
  aiPrompt: string = '';
  selectedReferenceImageForAI: File | string | null = null; // Can be a File object or an existing image URL/identifier
  aiNumImagesToGenerate: number = 1;
  isGeneratingAiImage = signal(false);
  aiGeneratedImageUrls = signal<string[]>([]);
  aiGenerationError = signal<string | null>(null);

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

  loadCategories(): void { // Bu metod artık loadRootCategories olacak
    this.categoryService.findRootCategories().subscribe({
      next: (cats) => {
        this.rootCategories.set(cats);
        // Eğer düzenleme modundaysak ve ürünün kategorisi bir alt kategori ise,
        // root kategoriyi ve ardından alt kategorileri yüklememiz gerekebilir.
        // Bu, loadProductForEdit içinde ele alınacak.
      },
      error: (err) => {
        console.error('Failed to load root categories for product form:', err);
        this.errorMessage.set('Could not load root categories. Please try again.');
      }
    });
  }

  onRootCategoryChange(event: Event | null): void {
    const rootCategoryId = event ? +(event.target as HTMLSelectElement).value : null;
    this.selectedRootCategoryId.set(rootCategoryId);
    this.childCategories.set([]); // Alt kategorileri sıfırla
    this.productForm.patchValue({ categoryId: rootCategoryId }); // Formdaki categoryId'yi güncelle

    if (rootCategoryId) {
      this.categoryService.getSubCategories(rootCategoryId).subscribe({
        next: (subCats) => {
          this.childCategories.set(subCats);
          // Eğer düzenleme modunda değilsek ve alt kategoriler varsa,
          // formu alt kategori seçmeye zorlamak için categoryId'yi null yapabiliriz.
          // Veya kullanıcıya bırakabiliriz. Şimdilik böyle kalsın.
          // Eğer düzenleme modundaysak ve ürünün kategorisi bu alt kategorilerden biriyse,
          // onu seçili hale getirmek loadProductForEdit'te yapılacak.
        },
        error: (err) => {
          console.error(`Failed to load sub-categories for root ID ${rootCategoryId}:`, err);
          this.errorMessage.set('Could not load sub-categories.');
        }
      });
    }
  }

  onChildCategoryChange(event: Event | null): void {
    const childCategoryId = event ? +(event.target as HTMLSelectElement).value : null;
    if (childCategoryId) {
      this.productForm.patchValue({ categoryId: childCategoryId });
    } else {
      // Eğer alt kategori seçimi kaldırılırsa, formdaki categoryId root kategori ID'si olmalı.
      this.productForm.patchValue({ categoryId: this.selectedRootCategoryId() });
    }
  }

  async loadProductForEdit(id: number): Promise<void> {
    this.isLoading.set(true);
    this.sellerService.getProductByIdForSeller(id).subscribe({ // subscribe içindeki callback async olmalıydı veya Promise'e çevrilmeliydi.
      next: async (product) => { // next callback'ini async yaptık
        this.productForm.patchValue({
          name: product.name,
          description: product.description,
          price: product.price,
          stock: product.stock, // stockQuantity -> stock
          sku: product.sku,
          // categoryId burada set edilmeyecek, aşağıda hiyerarşik olarak set edilecek
          active: product.active
        });

        // Kategori dropdown'larını ayarla
        if (product.categoryId) {
          try {
            const productCategory = await (this.categoryService.getCategoryById(product.categoryId).toPromise() ?? null);
            if (productCategory) {
              if (productCategory.parentId) { // Bu bir alt kategori
                this.selectedRootCategoryId.set(productCategory.parentId);
                // Root kategoriyi seçili hale getir (eğer dropdown'da varsa)
                // Ardından alt kategorileri yükle ve ürünün kategorisini seçili yap
                const subCats = (await this.categoryService.getSubCategories(productCategory.parentId).toPromise()) ?? [];
                this.childCategories.set(subCats);
                this.productForm.patchValue({ categoryId: productCategory.id });
              } else { // Bu bir kök kategori
                this.selectedRootCategoryId.set(productCategory.id);
                this.productForm.patchValue({ categoryId: productCategory.id });
                this.childCategories.set([]); // Alt kategori yok veya seçilmemiş
              }
            }
          } catch (catError) {
            console.error('Error setting category for editing product:', catError);
            this.errorMessage.set('Failed to set product category for editing.');
          }
        }

        // Image preview for existing product
        // Mevcut görselleri yükle
        if (product.productImages && product.productImages.length > 0) {
          this.existingProductImages.set(product.productImages);
        } else {
          this.existingProductImages.set([]);
        }
        // Ana görseli (eğer varsa) formda bir yerde göstermek için ayrı bir mantık gerekebilir
        // Şimdilik sadece listeliyoruz.
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(err.error?.message || 'Failed to load product details.');
        this.isLoading.set(false);
        console.error('Error loading product for edit:', err);
      }
    });
  }

  onFilesSelected(event: Event): void {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;
    if (fileList && fileList.length > 0) {
      // this.selectedFiles = Array.from(fileList); // Mevcutları silip yenilerini ekler
      // Mevcut seçilenlere ekleme yapmak için:
      for (let i = 0; i < fileList.length; i++) {
        const file = fileList[i];
        // Dosya boyutu ve tipi kontrolü eklenebilir
        if (file.size > 2 * 1024 * 1024) { // 2MB
          this.errorMessage.set(`File ${file.name} is too large (max 2MB).`);
          continue;
        }
        if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
          this.errorMessage.set(`File ${file.name} has an unsupported type.`);
          continue;
        }
        this.selectedFiles.push(file);
        const reader = new FileReader();
        reader.onload = () => {
          this.newImagePreviews.push({ url: reader.result, file: file });
        };
        reader.readAsDataURL(file);
      }
      element.value = ''; // Input'u sıfırla ki aynı dosyalar tekrar seçilebilsin
    }
  }

  removeNewImage(index: number): void {
    this.selectedFiles.splice(index, 1);
    this.newImagePreviews.splice(index, 1);
  }

  // TODO: deleteExistingImage(imageId: number) ve setAsPrimaryImage(imageId: number) metodları
  // backend endpoint'leri oluşturulduktan sonra implemente edilecek.

  generateAiImagesHandler(): void {
    if (!this.aiPrompt.trim()) {
      this.aiGenerationError.set('Please enter a prompt for AI image generation.');
      return;
    }
    if (!this.productId()) {
        this.aiGenerationError.set('Product must be saved before generating AI images or provide a reference image to edit.');
        // Veya referans görsel varsa ve model destekliyorsa direkt edit çağrılabilir.
        // Şimdilik productId'nin var olmasını bekleyelim.
        return;
    }

    this.isGeneratingAiImage.set(true);
    this.aiGenerationError.set(null);
    this.aiGeneratedImageUrls.set([]);

    const request: AiImageGenerationRequest = {
      prompt: this.aiPrompt,
      model: 'gpt-image-1', // Varsayılan model
      n: this.aiNumImagesToGenerate < 1 ? 1 : this.aiNumImagesToGenerate > 4 ? 4 : this.aiNumImagesToGenerate, // 1-4 arası
      size: '1024x1024', // Varsayılan boyut, DTO'ya eklenebilir
      // referenceImageIdentifier: this.selectedReferenceImageForAI instanceof File ? undefined : this.selectedReferenceImageForAI, // Eğer string ise ID'dir.
      // TODO: Eğer selectedReferenceImageForAI bir File ise, önce onu yükleyip ID'sini alıp request'e eklemek gerekebilir.
      // Şimdilik referans görseli backend'in işlemesini bekliyoruz (eğer string ID ise).
      // Veya frontend'den direkt dosya gönderme mantığı backend'de desteklenmeli.
      // OpenAiServiceImpl'deki referans görsel mantığı storage'dan ID ile yüklüyordu.
    };

    // Eğer referans görsel seçilmişse
    if (this.selectedReferenceImageForAI) {
      if (typeof this.selectedReferenceImageForAI === 'string') {
        // Eğer string ise, bu bir URL. Göreli yolu çıkarmamız lazım.
        // Örn: http://localhost:8080/uploads/products/images/xyz.jpg -> products/images/xyz.jpg
        const baseUrlPattern = /^https?:\/\/[^/]+\/uploads\//;
        let relativePath = this.selectedReferenceImageForAI.replace(baseUrlPattern, '');
        if (relativePath === this.selectedReferenceImageForAI) { // Eğer replace bir şey değiştirmediyse, zaten göreli yol olabilir veya format farklı.
            // Güvenlik için, eğer /uploads/ ile başlamıyorsa null yapalım veya hata verelim.
            // Şimdilik, /uploads/ içermiyorsa direkt kullanmayı deneyebilir, ama bu riskli.
            // Daha güvenli bir yol, backend'den gelen ProductImage nesnesinde relativePath tutmak.
            // Şimdilik, URL'den "uploads/" sonrasını almaya çalışalım.
            const uploadsMarker = '/uploads/';
            const uploadsIndex = this.selectedReferenceImageForAI.indexOf(uploadsMarker);
            if (uploadsIndex !== -1) {
                relativePath = this.selectedReferenceImageForAI.substring(uploadsIndex + uploadsMarker.length);
            } else {
                // Eğer URL beklenen formatta değilse, logla ve identifier'ı boş bırak.
                console.warn('Reference image URL does not match expected format for extracting relative path:', this.selectedReferenceImageForAI);
                this.aiGenerationError.set('Could not process reference image URL.');
                this.isGeneratingAiImage.set(false);
                return;
            }
        }
        request.referenceImageIdentifier = relativePath;
        console.log('Using reference image with relative path:', relativePath);
      } else {
        // Eğer File ise, bu dosyanın önce yüklenip identifier'ının alınması lazım.
        // Bu senaryo şimdilik desteklenmiyor, kullanıcıya hata göster.
        this.aiGenerationError.set('Uploading a new file as reference is not yet supported. Please select an existing product image.');
        this.isGeneratingAiImage.set(false);
        return;
      }
    }
    // TODO: Eğer this.selectedReferenceImageForAI bir File ise, bu dosyayı önce backend'e yükleyip
    // dönen identifier'ı request.referenceImageIdentifier olarak kullanmak daha doğru olur.
    // Bu kısım şimdilik basitleştirilmiştir.

    this.sellerService.generateAiImage(request).subscribe({
      next: (response: AiImageGenerationResponse) => {
        this.aiGeneratedImageUrls.set(response.imageUrls || []);
        // TODO: Kalan kotayı kullanıcıya göster (response.remainingQuota)
        this.isGeneratingAiImage.set(false);
        if (response.imageUrls.length === 0) {
          this.aiGenerationError.set(response.message || 'AI did not return any images.');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.isGeneratingAiImage.set(false);
        this.aiGenerationError.set(err.error?.message || 'Failed to generate AI images.');
        console.error('Error generating AI images:', err);
      }
    });
  }

  setAiImageAsProductImage(imageUrl: string): void {
    console.log('Attempting to set AI image. Received URL:', imageUrl); // Log eklendi
    if (!imageUrl || imageUrl.trim() === '') {
      this.errorMessage.set('Invalid AI image URL provided.');
      console.error('setAiImageAsProductImage called with empty or invalid URL:', imageUrl);
      return;
    }
    if (!this.productId()) {
      this.errorMessage.set('Product ID is not available. Save the product first.');
      return;
    }
    // Bu fonksiyon, seçilen AI görselini ürünün görsellerine ekler (yeni bir ProductImage olarak)
    // ve isteğe bağlı olarak ana görsel yapar.
    // Şimdilik, bu görseli direkt `selectedFiles`'a ekleyip `uploadSelectedImages` ile yüklemeyi deneyebiliriz
    // veya backend'e "bu URL'yi yeni bir ProductImage olarak ekle" diye bir istek atabiliriz.
    // En basiti, bu URL'yi bir şekilde `selectedFiles`'a dönüştürüp mevcut yükleme akışına dahil etmek
    // ya da `SetAiImageAsProductRequest` kullanarak backend'e göndermek.

    // Örnek: SetAiImageAsProductRequest kullanarak backend'e gönderme
    const request: SetAiImageAsProductRequest = {
      productId: this.productId()!,
      aiImageUrl: imageUrl, // generatedImageUrl -> aiImageUrl olarak değiştirildi
      setAsPrimary: this.existingProductImages().length === 0 && this.newImagePreviews.length === 0, // İlk AI görseli ana olsun
    };

    this.isLoading.set(true); // Genel yükleme göstergesi
    this.sellerService.setAiImageAsProduct(request).subscribe({
      next: (updatedProduct) => {
        this.isLoading.set(false);
        this.successMessage.set('AI image set as product image successfully!');
        this.aiGeneratedImageUrls.set([]); // Oluşturulanları temizle
        this.aiPrompt = ''; // Prompt'u temizle
        this.loadProductForEdit(updatedProduct.id); // Ürün formunu ve görselleri yenile
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to set AI image for product.');
        console.error('Error setting AI image:', err);
      }
    });
  }


  // uploadImage(): void { // Bu metod artık kullanılmayacak, uploadSelectedImages kullanılacak
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
          if (this.selectedFiles.length > 0 && updatedProduct.id) {
            this.uploadSelectedImages(updatedProduct.id); // Çoklu görsel yükle
          } else {
            // this.router.navigate(['/seller/products']);
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
          this.productForm.reset({ active: true, categoryId: null });
          if (this.selectedFiles.length > 0 && createdProduct.id) {
            this.productId.set(createdProduct.id);
            this.uploadSelectedImages(createdProduct.id); // Çoklu görsel yükle
          } else {
            this.newImagePreviews = []; // Önizlemeleri temizle
            this.selectedFiles = [];    // Seçili dosyaları temizle
          }
          // this.router.navigate(['/seller/products']);
        },
        error: (err: HttpErrorResponse) => {
          this.isLoading.set(false);
          this.errorMessage.set(err.error?.message || 'Failed to create product.');
          console.error('Error creating product:', err);
        }
      });
    }
  }

  uploadSelectedImages(productId: number): void {
    if (this.selectedFiles.length === 0) return;

    // Yükleme için ayrı bir isLoading state'i kullanılabilir
    // this.isUploadingImages.set(true);
    this.sellerService.uploadProductImages(productId, this.selectedFiles).subscribe({
      next: (imageUrls) => {
        // this.isUploadingImages.set(false);
        this.successMessage.set((this.successMessage() || '') + ` ${imageUrls.length} image(s) uploaded successfully!`);
        this.selectedFiles = []; // Dosya seçimini sıfırla
        this.newImagePreviews = []; // Önizlemeleri sıfırla
        // Düzenleme modundaysa, mevcut görselleri yeniden yükleyebiliriz veya dönen URL'leri ekleyebiliriz.
        if (this.isEditMode()) {
          this.loadProductForEdit(productId); // En basit yol, ürünü yeniden yüklemek
        }
        // Yeni ürün eklendiyse ve resimler yüklendiyse, belki listeye yönlendir
        // else { this.router.navigate(['/seller/products']); }
      },
      error: (err: HttpErrorResponse) => {
        // this.isUploadingImages.set(false);
        this.errorMessage.set((this.errorMessage() ? this.errorMessage() + ' ' : '') + (err.error?.message || 'Failed to upload images.'));
        console.error('Error uploading images for product ' + productId, err);
      }
    });
  }
}
