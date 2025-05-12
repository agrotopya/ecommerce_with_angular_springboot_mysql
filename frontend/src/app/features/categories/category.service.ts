import { Injectable, inject } from '@angular/core';
import { HttpParams } from '@angular/common/http'; // HttpClient kaldırıldı, HttpParams kaldı
import { Observable } from 'rxjs';
import { CategoryRequestDto, CategoryResponseDto } from '../../shared/models/category.model';
import { Page } from '../../shared/models/page.model';
import { ApiResponse } from '../../shared/models/user.model';
import { ApiService } from '../../core/services/api.service'; // ApiService eklendi
import { CATEGORY_ENDPOINTS, ADMIN_ENDPOINTS } from '../../core/constants/api-endpoints'; // CATEGORY_ENDPOINTS eklendi

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private apiService = inject(ApiService); // ApiService inject edildi

  constructor() { }

  // Public access for categories
  getAllCategories(params?: HttpParams): Observable<Page<CategoryResponseDto>> { // params opsiyonel yapıldı
    return this.apiService.get<Page<CategoryResponseDto>>(CATEGORY_ENDPOINTS.BASE, params);
  }

  getCategoryById(id: number): Observable<CategoryResponseDto> {
    return this.apiService.get<CategoryResponseDto>(CATEGORY_ENDPOINTS.DETAIL(id));
  }

  getCategoryBySlug(slug: string): Observable<CategoryResponseDto> {
    return this.apiService.get<CategoryResponseDto>(CATEGORY_ENDPOINTS.BY_SLUG(slug));
  }

  getActiveCategories(): Observable<CategoryResponseDto[]> {
    return this.apiService.get<CategoryResponseDto[]>(CATEGORY_ENDPOINTS.ACTIVE);
  }

  findRootCategories(): Observable<CategoryResponseDto[]> { // Yeni metod eklendi
    return this.apiService.get<CategoryResponseDto[]>(CATEGORY_ENDPOINTS.ROOTS);
  }

  getSubCategories(parentId: number): Observable<CategoryResponseDto[]> { // Yeni metod eklendi
    return this.apiService.get<CategoryResponseDto[]>(CATEGORY_ENDPOINTS.SUB_CATEGORIES(parentId));
  }

  // Admin-only category management
  createCategory(categoryData: CategoryRequestDto): Observable<CategoryResponseDto> {
    // ADMIN_ENDPOINTS.ADMIN_CATEGORY_CREATE '/categories' olduğu için CATEGORY_ENDPOINTS.BASE kullanılabilir
    // veya ADMIN_ENDPOINTS'e özel bir /admin/categories eklenebilir.
    // Şimdilik backend'deki @PreAuthorize("hasRole('ADMIN')")'e güveniyoruz.
    return this.apiService.post<CategoryResponseDto>(CATEGORY_ENDPOINTS.BASE, categoryData);
  }

  updateCategory(id: number, categoryData: CategoryRequestDto): Observable<CategoryResponseDto> {
    return this.apiService.put<CategoryResponseDto>(CATEGORY_ENDPOINTS.DETAIL(id), categoryData);
  }

  deleteCategory(id: number): Observable<ApiResponse> {
    return this.apiService.delete<ApiResponse>(CATEGORY_ENDPOINTS.DETAIL(id));
  }
}
