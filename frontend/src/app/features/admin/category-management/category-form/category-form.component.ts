import { Component, inject, OnInit, signal, WritableSignal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CategoryService } from '../../../../features/categories/category.service';
// import { AdminService } from '../../admin.service'; // AdminService yerine CategoryService kullanılacak
import { CategoryRequestDto, CategoryResponseDto } from '../../../../shared/models/category.model';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-category-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './category-form.component.html',
  styleUrls: ['./category-form.component.scss']
})
export class CategoryFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private categoryService = inject(CategoryService);
  // private adminService = inject(AdminService); // Kaldırıldı
  public router = inject(Router); // private -> public
  private route = inject(ActivatedRoute);

  categoryForm: FormGroup;
  isEditMode = signal(false);
  categoryId = signal<number | null>(null);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  // Üst kategori seçimi için
  allCategories: WritableSignal<CategoryResponseDto[]> = signal([]);

  // Resim yükleme için property'ler
  selectedFile: File | null = null;
  imagePreviewUrl: string | ArrayBuffer | null = null;

  constructor() {
    this.categoryForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      description: [''],
      parentId: [null], // Üst kategori ID'si
      isActive: [true],
      imageUrl: [''] // Şimdilik string, sonra dosya yükleme eklenecek
    });
  }

  ngOnInit(): void {
    this.loadAllCategoriesForDropdown();
    this.route.paramMap.subscribe(params => {
      const id = params.get('categoryId');
      if (id) {
        this.isEditMode.set(true);
        this.categoryId.set(+id);
        this.loadCategoryForEdit(+id);
      }
    });
  }

  loadAllCategoriesForDropdown(): void {
    // Üst kategori seçimi için sadece aktif kategorileri listeleyelim.
    this.categoryService.getActiveCategories().subscribe({
      next: (categories) => {
        this.allCategories.set(categories);
      },
      error: (err) => {
        console.error('Failed to load active categories for parent dropdown:', err);
        this.errorMessage.set('Could not load active categories for parent selection.');
      }
    });
  }

  loadCategoryForEdit(id: number): void {
    this.isLoading.set(true);
    this.categoryService.getCategoryById(id).subscribe({
      next: (category) => {
        this.categoryForm.patchValue({
          name: category.name,
          description: category.description,
          parentId: category.parentId,
          isActive: category.isActive,
          imageUrl: category.imageUrl
        });
        // this.imagePreviewUrl = category.imageUrl; // Eğer resim gösterilecekse
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(err.error?.message || 'Failed to load category details.');
        this.isLoading.set(false);
      }
    });
  }

  onSubmit(): void {
    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const categoryData: CategoryRequestDto = this.categoryForm.value;
    // parentId null değilse number'a çevir, backend null veya number bekliyor olabilir.
    if (categoryData.parentId) {
        categoryData.parentId = +categoryData.parentId;
    }


    // TODO: Resim yükleme mantığı eklenecek.
    // Eğer this.selectedFile varsa, önce resmi yükle, URL'i al, sonra categoryData.imageUrl'e ata.
    // Bu mantık, kategori kaydedildikten SONRA çalışmalı.

    const operation = this.isEditMode() && this.categoryId()
      ? this.categoryService.updateCategory(this.categoryId()!, categoryData)
      : this.categoryService.createCategory(categoryData);

    operation.subscribe({
      next: (savedCategory) => {
        this.isLoading.set(false);
        const message = this.isEditMode() ? 'Category updated successfully!' : 'Category created successfully!';
        this.successMessage.set(message);

        if (this.selectedFile && savedCategory.id) {
          this.uploadCategoryImage(savedCategory.id);
        } else {
          if (!this.isEditMode()) {
            this.categoryForm.reset({ parentId: null, isActive: true, imageUrl: '' });
            this.imagePreviewUrl = null;
          }
          // Opsiyonel: this.router.navigate(['/admin/categories']);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        const action = this.isEditMode() ? 'update' : 'create';
        this.errorMessage.set(err.error?.message || `Failed to ${action} category.`);
      }
    });
  }

  onFileSelected(event: Event): void {
    const element = event.currentTarget as HTMLInputElement;
    const fileList: FileList | null = element.files;
    if (fileList && fileList[0]) {
      const file = fileList[0];
      if (file.size > 2 * 1024 * 1024) { // 2MB
        this.errorMessage.set(`File ${file.name} is too large (max 2MB).`);
        this.selectedFile = null;
        this.imagePreviewUrl = null;
        element.value = ''; // Dosya seçimini sıfırla
        return;
      }
      if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
        this.errorMessage.set(`File ${file.name} has an unsupported type.`);
        this.selectedFile = null;
        this.imagePreviewUrl = null;
        element.value = ''; // Dosya seçimini sıfırla
        return;
      }
      this.selectedFile = file;
      const reader = new FileReader();
      reader.onload = () => {
        this.imagePreviewUrl = reader.result;
      };
      reader.readAsDataURL(this.selectedFile);
      this.errorMessage.set(null); // Önceki dosya hatalarını temizle
    } else {
      this.selectedFile = null;
      this.imagePreviewUrl = null;
    }
  }

  uploadCategoryImage(categoryId: number): void {
    if (!this.selectedFile) return;

    this.isLoading.set(true); // Belki ayrı bir isUploadingImage sinyali daha iyi olur
    this.categoryService.uploadCategoryImage(categoryId, this.selectedFile).subscribe({
      next: (updatedCategoryWithImage) => {
        this.isLoading.set(false);
        this.successMessage.set((this.successMessage() || '') + ' Image uploaded successfully!');
        this.categoryForm.patchValue({ imageUrl: updatedCategoryWithImage.imageUrl });
        this.imagePreviewUrl = updatedCategoryWithImage.imageUrl ?? null; // Önizlemeyi güncel URL ile set et
        this.selectedFile = null; // Dosya seçimini sıfırla
        // Formu sıfırlama veya yönlendirme burada yapılabilir.
        if (!this.isEditMode()) {
            this.categoryForm.reset({ parentId: null, isActive: true, imageUrl: '' });
            // this.imagePreviewUrl = null; // Zaten yeni URL ile set edildi.
        }
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.errorMessage.set((this.errorMessage() ? this.errorMessage() + ' ' : '') + (err.error?.message || 'Failed to upload category image.'));
        console.error('Error uploading category image:', err);
      }
    });
  }
}
