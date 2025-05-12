import { Component, inject, signal, OnInit } from '@angular/core'; // OnInit eklendi
import { CommonModule, NgIf } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router'; // ActivatedRoute eklendi
import { FeelService } from '../../../../feels/services/feel.service';
import { NotificationService } from '@core/services/notification.service';
import { CreateFeelRequestDto, FeelResponseDto } from '@shared/models/feel.model'; // FeelResponseDto eklendi
// import { ProductService } from '../../../../products/product.service'; // Ürün arama/seçme için
// import { Product } from '@shared/models/product.model';
// import { debounceTime, distinctUntilChanged, switchMap, catchError, map, startWith } from 'rxjs/operators';
// import { Observable, of } from 'rxjs';

@Component({
  selector: 'app-create-feel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgIf],
  templateUrl: './create-feel.component.html',
  styleUrls: ['./create-feel.component.scss']
})
export class CreateFeelComponent implements OnInit { // OnInit implement edildi
  private fb = inject(FormBuilder);
  private feelService = inject(FeelService);
  private notificationService = inject(NotificationService);
  public router = inject(Router);
  private route = inject(ActivatedRoute); // ActivatedRoute inject edildi
  // private productService = inject(ProductService); // Ürün arama için

  createFeelForm: FormGroup;
  editMode = signal(false);
  feelIdToEdit = signal<number | null>(null);
  isSubmitting = signal(false);
  selectedVideoFile: File | null = null;
  selectedThumbnailFile: File | null = null;
  videoPreviewUrl: string | ArrayBuffer | null = null;
  thumbnailPreviewUrl: string | ArrayBuffer | null = null;

  // searchedProducts$: Observable<Product[]> | undefined;
  // selectedProduct: Product | null = null;

  constructor() {
    this.createFeelForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(1000)]],
      productId: [null],
      videoFile: [null], // Edit modunda zorunlu değil
      thumbnailFile: [null]
    });
  }

  ngOnInit(): void {
    const feelIdParam = this.route.snapshot.paramMap.get('feelId');
    if (feelIdParam) {
      this.editMode.set(true);
      this.feelIdToEdit.set(+feelIdParam);
      this.createFeelForm.get('videoFile')?.clearValidators(); // Video zorunluluğunu kaldır
      this.createFeelForm.get('videoFile')?.updateValueAndValidity();
      this.loadFeelForEditing(+feelIdParam);
    } else {
      this.createFeelForm.get('videoFile')?.setValidators([Validators.required]); // Yeni feel için video zorunlu
      this.createFeelForm.get('videoFile')?.updateValueAndValidity();
    }
  }

  loadFeelForEditing(id: number): void {
    this.isSubmitting.set(true); // Yükleniyor durumu
    this.feelService.getFeelById(id).subscribe({ // id.toString() kaldırıldı
      next: (feel) => {
        this.createFeelForm.patchValue({
          title: feel.title,
          description: feel.description,
          productId: feel.productId
        });
        // Mevcut video ve thumbnail URL'lerini önizleme için ayarlayabiliriz (backend'den geliyorsa)
        // this.videoPreviewUrl = feel.videoUrl;
        // this.thumbnailPreviewUrl = feel.thumbnailUrl;
        this.isSubmitting.set(false);
      },
      error: (err) => {
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
      // Video önizlemesi
      const reader = new FileReader();
      reader.onload = () => {
        this.videoPreviewUrl = reader.result;
      };
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
      // Thumbnail önizlemesi
      const reader = new FileReader();
      reader.onload = () => {
        this.thumbnailPreviewUrl = reader.result;
      };
      reader.readAsDataURL(this.selectedThumbnailFile);
    } else {
      this.selectedThumbnailFile = null;
      this.thumbnailPreviewUrl = null;
      this.createFeelForm.patchValue({ thumbnailFile: null });
    }
  }

  // Ürün arama fonksiyonu (ileride eklenebilir)
  // searchProducts(event: Event): void {
  //   const searchTerm = (event.target as HTMLInputElement).value;
  //   if (searchTerm && searchTerm.length > 2) {
  //     this.searchedProducts$ = this.productService.searchProducts(searchTerm, 0, 5).pipe(
  //       map(page => page.content)
  //     );
  //   } else {
  //     this.searchedProducts$ = of([]);
  //   }
  // }

  // selectProduct(product: Product): void {
  //   this.selectedProduct = product;
  //   this.createFeelForm.patchValue({ productId: product.id });
  //   this.searchedProducts$ = of([]); // Aramayı temizle
  // }

  onSubmit(): void {
    if (this.createFeelForm.invalid) {
      this.notificationService.showError('Please fill all required fields.');
      this.createFeelForm.markAllAsTouched();
      return;
    }
    if (!this.editMode() && !this.selectedVideoFile) {
      this.notificationService.showError('Please select a video file for the new feel.');
      return;
    }

    this.isSubmitting.set(true);

    const dto: CreateFeelRequestDto = { // Düzenleme için de bu DTO kullanılabilir, backend PUT isteği için farklı bir DTO bekleyebilir.
      title: this.createFeelForm.value.title,
      description: this.createFeelForm.value.description || undefined,
      productId: this.createFeelForm.value.productId ? Number(this.createFeelForm.value.productId) : undefined,
    };

    if (this.editMode() && this.feelIdToEdit()) {
      this.feelService.updateFeel(this.feelIdToEdit()!, dto, this.selectedVideoFile || undefined, this.selectedThumbnailFile || undefined)
        .subscribe({
          next: () => {
            this.isSubmitting.set(false);
            this.notificationService.showSuccess('Feel updated successfully!');
            this.router.navigate(['/seller/feels/my']);
          },
          error: (err) => {
            this.isSubmitting.set(false);
            // Hata mesajı FeelService içinde zaten gösteriliyor.
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
          error: (err) => {
            this.isSubmitting.set(false);
            console.error('Error creating feel:', err);
          }
        });
    }
  }
}
