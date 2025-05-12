// src/app/features/seller/components/create-feel/create-feel.component.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
// import { MatSnackBar } from '@angular/material/snack-bar'; // Bildirimler için NotificationService kullanılacak
import { NotificationService } from '../../../../core/services/notification.service'; // NotificationService import edildi

import { FeelService } from '../../../feels/services/feel.service';
import { CreateFeelRequestDto } from '../../../../shared/models/feel.model';
import { ProductService } from '../../../products/product.service'; // Satıcının ürünlerini listelemek için
import { Product } from '../../../../shared/models/product.model';
import { HttpErrorResponse } from '@angular/common/http';
import { Page } from '../../../../shared/models/page.model';

@Component({
  selector: 'app-create-feel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule], // Gerekli modüller
  templateUrl: './create-feel.component.html',
  styleUrls: ['./create-feel.component.scss']
})
export class CreateFeelComponent implements OnInit {
  private fb = inject(FormBuilder);
  private feelService = inject(FeelService);
  private productService = inject(ProductService); // Satıcının ürünlerini çekmek için
  private router = inject(Router);
  private route = inject(ActivatedRoute); // İlişkili ürün ID'sini query params'tan almak için
  // private snackBar = inject(MatSnackBar);
  private notificationService = inject(NotificationService); // NotificationService inject edildi

  feelForm: FormGroup;
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  formSubmitted = signal(false); // Formun gönderilip gönderilmediğini takip etmek için

  selectedVideoFile: File | null = null;
  videoPreviewUrl: string | ArrayBuffer | null = null;
  selectedThumbnailFile: File | null = null;
  thumbnailPreviewUrl: string | ArrayBuffer | null = null;

  sellerProducts = signal<Product[]>([]); // Satıcının ürünleri için dropdown
  initialProductId: number | null = null;

  constructor() {
    this.feelForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(5)]],
      description: ['', Validators.maxLength(1000)],
      productId: [null] // Opsiyonel, feel bir ürünle ilişkilendirilebilir
    });
  }

  ngOnInit(): void {
    this.loadSellerProducts();
    this.route.queryParamMap.subscribe(params => {
      const productIdFromQuery = params.get('productId');
      if (productIdFromQuery) {
        this.initialProductId = +productIdFromQuery;
        this.feelForm.patchValue({ productId: this.initialProductId });
      }
    });
  }

  loadSellerProducts(): void {
    // Satıcının kendi ürünlerini (sadece ID ve isim yeterli olabilir) çek
    // ProductService'e veya SellerService'e bunun için bir metod eklenebilir.
    // Şimdilik SellerService'te getMyProducts olduğunu varsayalım (sayfalama olmadan tümü)
    // Veya ProductService'e getProductsBySellerId gibi bir metod eklenebilir.
    // Bu örnekte, ProductService'e getProductsForSellerDropdown gibi bir metod eklediğimizi varsayalım.
    this.productService.getProductsBySellerForDropdown().subscribe({
        next: (products) => {
            this.sellerProducts.set(products);
        },
        error: (err) => {
            console.error('Error loading seller products for feel form:', err);
            this.notificationService.showError('Could not load your products for selection.');
        }
    });
  }

  onVideoFileSelected(event: Event): void {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;
    if (fileList && fileList[0]) {
      if (fileList[0].type.startsWith('video/')) {
        this.selectedVideoFile = fileList[0];
        const reader = new FileReader();
        reader.onload = () => this.videoPreviewUrl = reader.result;
        reader.readAsDataURL(this.selectedVideoFile);
        this.errorMessage.set(null);
      } else {
        this.selectedVideoFile = null;
        this.videoPreviewUrl = null;
        this.errorMessage.set('Please select a valid video file.');
      }
    }
  }

  onThumbnailFileSelected(event: Event): void {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;
    if (fileList && fileList[0]) {
      if (fileList[0].type.startsWith('image/')) {
        this.selectedThumbnailFile = fileList[0];
        const reader = new FileReader();
        reader.onload = () => this.thumbnailPreviewUrl = reader.result;
        reader.readAsDataURL(this.selectedThumbnailFile);
      } else {
        this.selectedThumbnailFile = null;
        this.thumbnailPreviewUrl = null;
        this.notificationService.showError('Please select a valid image file for thumbnail.');
      }
    }
  }

  onSubmit(): void {
    this.formSubmitted.set(true); // Form gönderildi olarak işaretle
    if (this.feelForm.invalid) {
      this.feelForm.markAllAsTouched();
      this.errorMessage.set('Please fill all required fields.');
      return;
    }
    if (!this.selectedVideoFile) {
      this.errorMessage.set('A video file is required to create a Feel.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const dto: CreateFeelRequestDto = {
      title: this.feelForm.value.title,
      description: this.feelForm.value.description || undefined,
      productId: this.feelForm.value.productId ? +this.feelForm.value.productId : undefined
    };

    this.feelService.createFeel(dto, this.selectedVideoFile, this.selectedThumbnailFile || undefined)
      .subscribe({
        next: (createdFeel) => {
          this.isLoading.set(false);
          this.successMessage.set(`Feel "${createdFeel.title}" created successfully!`);
          this.notificationService.showSuccess(`Feel "${createdFeel.title}" created!`);
          this.feelForm.reset();
          this.selectedVideoFile = null;
          this.videoPreviewUrl = null;
          this.selectedThumbnailFile = null;
          this.thumbnailPreviewUrl = null;
          // TODO: Satıcının feel listesine yönlendir
          // this.router.navigate(['/seller/my-feels']); // veya /seller/feels/manage
        },
        error: (err: HttpErrorResponse) => {
          this.isLoading.set(false);
          this.errorMessage.set(err.error?.message || 'Failed to create Feel. Please try again.');
          console.error('Error creating feel:', err);
        }
      });
  }

  navigateToDashboard(): void {
    this.router.navigate(['/seller/dashboard']);
  }
}
