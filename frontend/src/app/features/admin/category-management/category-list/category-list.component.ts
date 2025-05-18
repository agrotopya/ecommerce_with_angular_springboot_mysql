import { Component, OnInit, signal, WritableSignal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { CategoryService } from '../../../../features/categories/category.service';
import { CategoryResponseDto } from '../../../../shared/models/category.model';
import { Page } from '../../../../shared/models/page.model';
import { PaginatorComponent } from '../../../../shared/components/paginator/paginator.component';
import { ConfirmationModalComponent } from '../../../../shared/components/confirmation-modal/confirmation-modal.component';
import { NotificationService } from '../../../../core/services/notification.service';
import { HttpParams } from '@angular/common/http';
import { Observable, Subject, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, catchError, tap } from 'rxjs/operators';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PaginatorComponent,
    ConfirmationModalComponent
  ],
  templateUrl: './category-list.component.html',
  styleUrl: './category-list.component.scss'
})
export class AdminCategoryListComponent implements OnInit {
  private categoryService = inject(CategoryService);
  private router = inject(Router);
  private notificationService = inject(NotificationService);

  categories: WritableSignal<CategoryResponseDto[]> = signal([]);
  currentPage: WritableSignal<number> = signal(0);
  totalPages: WritableSignal<number> = signal(0);
  totalElements: WritableSignal<number> = signal(0);
  pageSize: WritableSignal<number> = signal(10);

  searchTerm: string = '';
  private searchSubject = new Subject<string>();

  showDeleteConfirmationModal = signal(false);
  categoryToDeleteId: number | null = null;

  constructor() {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term => this.loadCategories(0, this.pageSize(), term))
    ).subscribe();
  }

  ngOnInit(): void {
    this.loadCategories(this.currentPage(), this.pageSize(), this.searchTerm).subscribe();
  }

  loadCategories(page: number, size: number, searchTerm?: string): Observable<Page<CategoryResponseDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (searchTerm && searchTerm.trim() !== '') {
      params = params.set('name', searchTerm.trim());
    }

    return this.categoryService.getAllCategoriesForAdmin(params).pipe(
      tap(response => {
        this.categories.set(response.content);
        this.currentPage.set(response.number);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
      }),
      catchError(error => {
        this.notificationService.showError('Kategoriler yüklenirken bir hata oluştu.');
        console.error('Error loading categories:', error);
        this.categories.set([]);
        this.totalPages.set(0);
        this.totalElements.set(0);
        return of({
          content: [],
          pageable: {
            sort: {
              sorted: false,
              unsorted: true,
              empty: true
            },
            offset: 0,
            pageNumber: 0,
            pageSize: size,
            paged: true,
            unpaged: false
          },
          number: 0,
          totalPages: 0,
          totalElements: 0,
          size: size,
          sort: {
            sorted: false,
            unsorted: true,
            empty: true
          },
          first: true,
          last: true,
          empty: true,
          numberOfElements: 0
        } as Page<CategoryResponseDto>);
      })
    );
  }

  onSearchChange(searchValue: string): void {
    this.searchTerm = searchValue;
    this.searchSubject.next(searchValue.trim());
  }

  onPageChange(page: number): void {
    this.loadCategories(page, this.pageSize(), this.searchTerm).subscribe();
  }

  onPageSizeChange(event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    const newSize = Number(selectElement.value);
    this.pageSize.set(newSize);
    this.loadCategories(0, newSize, this.searchTerm).subscribe();
  }

  navigateToAddCategory(): void {
    this.router.navigate(['/admin/categories/new']);
  }

  navigateToEditCategory(categoryId: number): void {
    this.router.navigate(['/admin/categories/edit', categoryId]);
  }

  openDeleteConfirmation(categoryId: number): void {
    this.categoryToDeleteId = categoryId;
    this.showDeleteConfirmationModal.set(true);
  }

  confirmDelete(): void {
    if (this.categoryToDeleteId !== null) {
      this.categoryService.deleteCategory(this.categoryToDeleteId).subscribe({
        next: () => {
          this.notificationService.showSuccess('Kategori başarıyla silindi.');
          this.showDeleteConfirmationModal.set(false);

          const currentTotalElements = this.totalElements();
          const currentPageVal = this.currentPage();
          const pageSizeVal = this.pageSize();

          if (currentTotalElements -1 === 0 && currentPageVal > 0) {
             this.loadCategories(currentPageVal - 1, pageSizeVal, this.searchTerm).subscribe();
          } else {
            const pageToLoad = (currentTotalElements === 1 && currentPageVal === 0) ? 0 : currentPageVal;
             this.loadCategories(pageToLoad, pageSizeVal, this.searchTerm).subscribe();
          }
          this.categoryToDeleteId = null;
        },
        error: (err) => {
          console.error('Error deleting category:', err);
          if (err.error && err.error.message && err.error.message.includes('Cannot delete category with associated products or subcategories')) {
            this.notificationService.showError('Bu kategoriye bağlı ürünler veya alt kategoriler olduğu için silinemez.');
          } else {
            this.notificationService.showError('Kategori silinirken bir hata oluştu.');
          }
          this.showDeleteConfirmationModal.set(false);
          this.categoryToDeleteId = null;
        }
      });
    }
  }

  cancelDelete(): void {
    this.showDeleteConfirmationModal.set(false);
    this.categoryToDeleteId = null;
  }
}
