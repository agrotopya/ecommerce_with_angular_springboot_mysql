import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { FeelService } from '../../../../feels/services/feel.service';
import { NotificationService } from '@core/services/notification.service';
import { CreateFeelRequestDto, FeelResponseDto } from '@shared/models/feel.model';
import { Product } from '@shared/models/product.model';
import { ProductService } from '@features/products/product.service';
// Page modeli artık loadSellerProducts için gerekmeyebilir, eğer getProductsBySellerForDropdown Product[] döndürüyorsa.
// import { Page } from '@shared/models/page.model';

@Component({
  selector: 'app-create-feel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgIf],
  templateUrl: './create-feel.component.html',
  styleUrls: ['./create-feel.component.scss']
})
export class CreateFeelComponent implements OnInit {
  private fb = inject(FormBuilder);
  private feelService = inject(FeelService);
  private notificationService = inject(NotificationService);
  public router = inject(Router);
  private route = inject(ActivatedRoute);
  private productService = inject(ProductService);

  createFeelForm: FormGroup;
  editMode = signal(false);
  feelIdToEdit = signal<number | null>(null);
  isSubmitting = signal(false);
  selectedVideoFile: File | null = null;
  selectedThumbnailFile: File | null = null;
  videoPreviewUrl: string | ArrayBuffer | null = null;
  thumbnailPreviewUrl: string | ArrayBuffer | null = null;

  sellerProducts = signal<Product[]>([]);
  isLoadingProducts = signal(false);

  constructor() {
    this.createFeelForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(1000)]],
      productId: [null, [Validators.required]],
      videoFile: [null],
      thumbnailFile: [null]
    });
  }

  ngOnInit(): void {
    this.loadSellerProducts();
    const feelIdParam = this.route.snapshot.paramMap.get('feelId');
    if (feelIdParam) {
      this.editMode.set(true);
      this.feelIdToEdit.set(+feelIdParam);
      this.createFeelForm.get('videoFile')?.clearValidators();
      this.createFeelForm.get('videoFile')?.updateValueAndValidity();
      this.loadFeelForEditing(+feelIdParam);
    } else {
      this.createFeelForm.get('videoFile')?.setValidators([Validators.required]);
      this.createFeelForm.get('videoFile')?.updateValueAndValidity();
    }
  }

  loadSellerProducts(): void {
    this.isLoadingProducts.set(true);
    this.productService.getProductsBySellerForDropdown() // Metod adı düzeltildi
      .subscribe({
        next: (products: Product[]) => { // Dönüş tipi Product[] olarak düzeltildi
          // TODO: Sadece feel'i olmayan ürünleri filtrele (backend'de veya burada)
          this.sellerProducts.set(products);
          this.isLoadingProducts.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.isLoadingProducts.set(false);
          this.notificationService.showError('Failed to load your products.');
          console.error('Error loading seller products:', err);
        }
      });
  }

  loadFeelForEditing(id: number): void {
    this.isSubmitting.set(true);
    this.feelService.getFeelById(id).subscribe({
      next: (feel) => {
        this.createFeelForm.patchValue({
          title: feel.title,
          description: feel.description,
          productId: feel.productId
        });
        this.isSubmitting.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        this.notificationService.showError('Failed to load feel data for editing.');
        this.router.navigate(['/seller/feels/my']);
      }
    });
  }

  onVideoFileSelected(event: Event): void {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;
    if (fileList && fileList[0]) {
      this.selectedVideoFile = fileList[0];
      this.createFeelForm.patchValue({ videoFile: this.selectedVideoFile });
      const reader = new FileReader();
      reader.onload = () => { this.videoPreviewUrl = reader.result; };
      reader.readAsDataURL(this.selectedVideoFile);
    } else {
      this.selectedVideoFile = null;
      this.videoPreviewUrl = null;
      this.createFeelForm.patchValue({ videoFile: null });
    }
  }

  onThumbnailFileSelected(event: Event): void {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;
    if (fileList && fileList[0]) {
      this.selectedThumbnailFile = fileList[0];
      this.createFeelForm.patchValue({ thumbnailFile: this.selectedThumbnailFile });
      const reader = new FileReader();
      reader.onload = () => { this.thumbnailPreviewUrl = reader.result; };
      reader.readAsDataURL(this.selectedThumbnailFile);
    } else {
      this.selectedThumbnailFile = null;
      this.thumbnailPreviewUrl = null;
      this.createFeelForm.patchValue({ thumbnailFile: null });
    }
  }

  onSubmit(): void {
    if (this.createFeelForm.invalid) {
      this.notificationService.showError('Please fill all required fields, including selecting a product.');
      this.createFeelForm.markAllAsTouched();
      return;
    }
    if (!this.editMode() && !this.selectedVideoFile) {
      this.notificationService.showError('Please select a video file for the new feel.');
      return;
    }

    this.isSubmitting.set(true);

    const dto: CreateFeelRequestDto = {
      title: this.createFeelForm.value.title,
      description: this.createFeelForm.value.description || undefined,
      productId: Number(this.createFeelForm.value.productId),
    };

    if (this.editMode() && this.feelIdToEdit()) {
      this.feelService.updateFeel(this.feelIdToEdit()!, dto, this.selectedVideoFile || undefined, this.selectedThumbnailFile || undefined)
        .subscribe({
          next: () => {
            this.isSubmitting.set(false);
            this.notificationService.showSuccess('Feel updated successfully!');
            this.router.navigate(['/seller/feels/my']);
          },
          error: (err: HttpErrorResponse) => {
            this.isSubmitting.set(false);
            console.error('Error updating feel:', err);
          }
        });
    } else {
      this.feelService.createFeel(dto, this.selectedVideoFile!, this.selectedThumbnailFile || undefined)
        .subscribe({
          next: () => {
            this.isSubmitting.set(false);
            this.notificationService.showSuccess('Feel created successfully!');
            this.router.navigate(['/seller/feels/my']);
          },
          error: (err: HttpErrorResponse) => {
            this.isSubmitting.set(false);
            console.error('Error creating feel:', err);
          }
        });
    }
  }
}
