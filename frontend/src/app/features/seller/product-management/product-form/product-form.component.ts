// src/app/features/seller/product-management/product-form/product-form.component.ts
import { Component, inject, OnInit, signal, WritableSignal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { SellerService } from '../../seller.service';
import { Product, ProductRequest, ProductImage } from '../../../../shared/models/product.model';
import { CategoryService } from '../../../categories/category.service';
import { CategoryResponseDto } from '../../../../shared/models/category.model';
import { HttpErrorResponse } from '@angular/common/http';
import { AiImageGenerationRequest, AiImageGenerationResponse, SetAiImageAsProductRequest, ProductEnhancementRequestDto, ProductEnhancementResponseDto } from '../../../../shared/models/ai.model'; // AI DTO'ları eklendi
import { AiService } from '@core/services/ai.service'; // AiService eklendi
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component'; // LoadingSpinnerComponent eklendi

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, LoadingSpinnerComponent], // LoadingSpinnerComponent eklendi
  templateUrl: './product-form.component.html',
  styleUrls: ['./product-form.component.scss']
})
export class ProductFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private sellerService = inject(SellerService);
  private categoryService = inject(CategoryService);
  public router = inject(Router);
  private route = inject(ActivatedRoute);
  private aiService = inject(AiService); // AiService enjekte edildi

  productForm: FormGroup;
  isEditMode = signal(false);
  productId = signal<number | null>(null);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  rootCategories: WritableSignal<CategoryResponseDto[]> = signal([]);
  childCategories: WritableSignal<CategoryResponseDto[]> = signal([]);
  selectedRootCategoryId: WritableSignal<number | null> = signal(null);

  selectedFiles: File[] = [];
  newImagePreviews: { url: string | ArrayBuffer | null, file: File }[] = [];
  existingProductImages = signal<ProductImage[]>([]);

  aiPrompt: string = '';
  selectedReferenceImageForAI: File | string | null = null;
  aiNumImagesToGenerate: number = 1;
  isGeneratingAiImage = signal(false);
  aiGeneratedImageUrls = signal<string[]>([]);
  aiGenerationError = signal<string | null>(null);

  // AI Product Enhancement Signals
  isEnhancingProductDetails = signal(false);
  productEnhancements = signal<ProductEnhancementResponseDto | null>(null);
  aiEnhancementError = signal<string | null>(null);

  constructor() {
    this.productForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      price: [null, [Validators.required, Validators.min(0.01)]],
      stock: [0, [Validators.required, Validators.min(0)]],
      sku: [''],
      categoryId: [null, [Validators.required]],
      active: [true]
    });
  }

  ngOnInit(): void {
    this.loadCategories();
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
    this.categoryService.findRootCategories().subscribe({
      next: (cats) => this.rootCategories.set(cats),
      error: (err) => {
        console.error('Failed to load root categories for product form:', err);
        this.errorMessage.set('Could not load root categories. Please try again.');
      }
    });
  }

  onRootCategoryChange(event: Event | null): void {
    const rootCategoryId = event ? +(event.target as HTMLSelectElement).value : null;
    this.selectedRootCategoryId.set(rootCategoryId);
    this.childCategories.set([]);
    this.productForm.patchValue({ categoryId: rootCategoryId });

    if (rootCategoryId) {
      this.categoryService.getSubCategories(rootCategoryId).subscribe({
        next: (subCats) => this.childCategories.set(subCats),
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
      this.productForm.patchValue({ categoryId: this.selectedRootCategoryId() });
    }
  }

  async loadProductForEdit(id: number): Promise<void> {
    this.isLoading.set(true);
    this.sellerService.getProductByIdForSeller(id).subscribe({
      next: async (product) => {
        this.productForm.patchValue({
          name: product.name,
          description: product.description,
          price: product.price,
          stock: product.stock,
          sku: product.sku,
          active: product.active
        });

        if (product.categoryId) {
          try {
            const productCategory = await (this.categoryService.getCategoryById(product.categoryId).toPromise() ?? null);
            if (productCategory) {
              if (productCategory.parentId) {
                this.selectedRootCategoryId.set(productCategory.parentId);
                const subCats = (await this.categoryService.getSubCategories(productCategory.parentId).toPromise()) ?? [];
                this.childCategories.set(subCats);
                this.productForm.patchValue({ categoryId: productCategory.id });
              } else {
                this.selectedRootCategoryId.set(productCategory.id);
                this.productForm.patchValue({ categoryId: productCategory.id });
                this.childCategories.set([]);
              }
            }
          } catch (catError) {
            console.error('Error setting category for editing product:', catError);
            this.errorMessage.set('Failed to set product category for editing.');
          }
        }

        if (product.productImages && product.productImages.length > 0) {
          this.existingProductImages.set(product.productImages);
        } else {
          this.existingProductImages.set([]);
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

  onFilesSelected(event: Event): void {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;
    if (fileList && fileList.length > 0) {
      for (let i = 0; i < fileList.length; i++) {
        const file = fileList[i];
        if (file.size > 2 * 1024 * 1024) {
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
      element.value = '';
    }
  }

  removeNewImage(index: number): void {
    this.selectedFiles.splice(index, 1);
    this.newImagePreviews.splice(index, 1);
  }

  getAiEnhancements(): void {
    if (!this.productForm.value.name && !this.productForm.value.description) {
      this.aiEnhancementError.set('Please enter a product name or description to get AI suggestions.');
      return;
    }
    this.isEnhancingProductDetails.set(true);
    this.aiEnhancementError.set(null);
    this.productEnhancements.set(null);

    const selectedCategoryId = this.productForm.value.categoryId;
    let categoryName = '';
    if (selectedCategoryId) {
      const foundInCategoryList = (list: CategoryResponseDto[]) => list.find(cat => cat.id === selectedCategoryId);
      const rootCat = foundInCategoryList(this.rootCategories());
      const childCat = foundInCategoryList(this.childCategories());
      categoryName = childCat?.name || rootCat?.name || '';
    }

    const request: ProductEnhancementRequestDto = {
      currentName: this.productForm.value.name || '',
      currentDescription: this.productForm.value.description || '',
      categoryName: categoryName
    };

    this.aiService.enhanceProductDetails(request).subscribe({
      next: (response) => {
        this.isEnhancingProductDetails.set(false);
        if (response.success) {
          this.productEnhancements.set(response);
        } else {
          this.aiEnhancementError.set(response.errorMessage || 'Failed to get AI enhancements.');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.isEnhancingProductDetails.set(false);
        this.aiEnhancementError.set(err.error?.message || 'Error communicating with AI service.');
        console.error('AI enhancement error:', err);
      }
    });
  }

  applySuggestion(field: 'name' | 'description', value: string): void {
    this.productForm.patchValue({ [field]: value });
  }

  applyImagePrompt(prompt: string): void {
    this.aiPrompt = prompt; // AI Image Generation bölümündeki prompt'u günceller
  }

  generateAiImagesHandler(): void {
    if (!this.aiPrompt.trim()) {
      this.aiGenerationError.set('Please enter a prompt for AI image generation.');
      return;
    }
    this.isGeneratingAiImage.set(true);
    this.aiGenerationError.set(null);
    this.aiGeneratedImageUrls.set([]);

    const request: AiImageGenerationRequest = {
      prompt: this.aiPrompt,
      model: 'gpt-image-1',
      n: this.aiNumImagesToGenerate < 1 ? 1 : this.aiNumImagesToGenerate > 4 ? 4 : this.aiNumImagesToGenerate,
      size: '1024x1024',
    };

    if (this.selectedReferenceImageForAI) {
      if (typeof this.selectedReferenceImageForAI === 'string') {
        const baseUrlPattern = /^https?:\/\/[^/]+\/uploads\//;
        let relativePath = this.selectedReferenceImageForAI.replace(baseUrlPattern, '');
        const uploadsMarker = '/uploads/';
        const uploadsIndex = this.selectedReferenceImageForAI.indexOf(uploadsMarker);
        if (uploadsIndex !== -1) {
            relativePath = this.selectedReferenceImageForAI.substring(uploadsIndex + uploadsMarker.length);
        } else {
            console.warn('Reference image URL does not match expected format for extracting relative path:', this.selectedReferenceImageForAI);
            this.aiGenerationError.set('Could not process reference image URL.');
            this.isGeneratingAiImage.set(false);
            return;
        }
        request.referenceImageIdentifier = relativePath;
      } else {
        this.aiGenerationError.set('Uploading a new file as reference is not yet supported. Please select an existing product image.');
        this.isGeneratingAiImage.set(false);
        return;
      }
    }

    this.sellerService.generateAiImage(request).subscribe({
      next: (response: AiImageGenerationResponse) => {
        this.aiGeneratedImageUrls.set(response.imageUrls || []);
        this.isGeneratingAiImage.set(false);
        if (!response.imageUrls || response.imageUrls.length === 0) {
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
    if (!imageUrl || imageUrl.trim() === '') {
      this.errorMessage.set('Invalid AI image URL provided.');
      return;
    }
    if (!this.productId()) {
      this.errorMessage.set('Product ID is not available. Save the product first.');
      return;
    }
    const request: SetAiImageAsProductRequest = {
      productId: this.productId()!,
      aiImageUrl: imageUrl,
      setAsPrimary: this.existingProductImages().length === 0 && this.newImagePreviews.length === 0,
    };

    this.isLoading.set(true);
    this.sellerService.setAiImageAsProduct(request).subscribe({
      next: (updatedProduct) => {
        this.isLoading.set(false);
        this.successMessage.set('AI image set as product image successfully!');
        this.aiGeneratedImageUrls.set([]);
        this.aiPrompt = '';
        this.loadProductForEdit(updatedProduct.id);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to set AI image for product.');
        console.error('Error setting AI image:', err);
      }
    });
  }

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

    if (this.isEditMode() && this.productId()) {
      this.sellerService.updateProduct(this.productId()!, productData).subscribe({
        next: (updatedProduct) => {
          this.isLoading.set(false);
          this.successMessage.set('Product updated successfully!');
          if (this.selectedFiles.length > 0 && updatedProduct.id) {
            this.uploadSelectedImages(updatedProduct.id);
          }
        },
        error: (err: HttpErrorResponse) => {
          this.isLoading.set(false);
          this.errorMessage.set(err.error?.message || 'Failed to update product.');
          console.error('Error updating product:', err);
        }
      });
    } else {
      this.sellerService.createProduct(productData).subscribe({
        next: (createdProduct) => {
          this.isLoading.set(false);
          this.successMessage.set('Product created successfully!');
          this.productForm.reset({ active: true, categoryId: null });
          if (this.selectedFiles.length > 0 && createdProduct.id) {
            this.productId.set(createdProduct.id);
            this.uploadSelectedImages(createdProduct.id);
          } else {
            this.newImagePreviews = [];
            this.selectedFiles = [];
          }
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
    this.sellerService.uploadProductImages(productId, this.selectedFiles).subscribe({
      next: (imageUrls) => {
        this.successMessage.set((this.successMessage() || '') + ` ${imageUrls.length} image(s) uploaded successfully!`);
        this.selectedFiles = [];
        this.newImagePreviews = [];
        if (this.isEditMode()) {
          this.loadProductForEdit(productId);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set((this.errorMessage() ? this.errorMessage() + ' ' : '') + (err.error?.message || 'Failed to upload images.'));
        console.error('Error uploading images for product ' + productId, err);
      }
    });
  }
}
