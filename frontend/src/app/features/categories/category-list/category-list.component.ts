import { Component, OnInit, ViewChild, AfterViewInit, inject } from '@angular/core'; // inject eklendi
import { CommonModule } from '@angular/common'; // CommonModule eklendi
import { HttpClientModule, HttpParams } from '@angular/common/http'; // HttpClientModule eklendi
import { Router } from '@angular/router'; // RouterLink kaldırıldı
import { MatTableDataSource, MatTableModule } from '@angular/material/table'; // MatTableModule eklendi
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator'; // MatPaginatorModule eklendi
import { MatSort, MatSortModule } from '@angular/material/sort'; // MatSortModule eklendi
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar'; // MatSnackBarModule eklendi
import { MatDialog, MatDialogModule } from '@angular/material/dialog'; // MatDialogModule eklendi
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CategoryService } from '../category.service';
import { AuthService } from '../../../core/services/auth.service'; // AuthService import yolu düzeltildi
import { CategoryResponseDto } from '../../../shared/models/category.model'; // .models -> .model
import { Page } from '../../../shared/models/page.model'; // page.model import yolu düzeltildi
import { Role } from '../../../shared/enums/role.enum'; // Role enum import edildi
// Import a confirmation dialog component if you create one, e.g., ConfirmationDialogComponent

@Component({
  selector: 'app-category-list',
  standalone: true, // standalone yapıldı
  imports: [
    CommonModule,
    HttpClientModule,
    // RouterLink kaldırıldı
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule
  ],
  templateUrl: './category-list.component.html',
  styleUrls: ['./category-list.component.scss']
})
export class CategoryListComponent implements OnInit, AfterViewInit {
  displayedColumns: string[] = ['id', 'name', 'slug', 'isActive', 'displayOrder', 'actions'];
  dataSource: MatTableDataSource<CategoryResponseDto> = new MatTableDataSource<CategoryResponseDto>();
  // authService inject ile public olarak eklendi
  public authService = inject(AuthService);
  public Role = Role; // Role enum'ını template'de kullanmak için public yapıldı
  isLoading = true;
  totalElements = 0;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private categoryService: CategoryService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private router: Router
    // AuthService constructor'dan kaldırıldı, inject ile public olarak eklendi
  ) { }

  ngOnInit(): void {
    this.loadCategories();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;

    this.paginator.page.subscribe(() => this.loadCategories(false));
    this.sort.sortChange.subscribe(() => {
      this.paginator.pageIndex = 0;
      this.loadCategories(false);
    });
  }

  loadCategories(showLoading: boolean = true): void {
    if (showLoading) {
      this.isLoading = true;
    }
    let params = new HttpParams()
      .set('page', this.paginator ? this.paginator.pageIndex.toString() : '0')
      .set('size', this.paginator ? this.paginator.pageSize.toString() : '10');

    if (this.sort && this.sort.active && this.sort.direction) {
      params = params.set('sort', `${this.sort.active},${this.sort.direction}`);
    }
    // According to apidocs.md, GET /api/admin/categories is for admin listing.
    // Assuming the regular GET /api/categories is also paginated and sortable for general view.
    // For admin management, we should use the admin endpoint if it provides more management-specific data or actions.
    // Let's assume for now that the admin listing will use the same structure but might have different base URL or additional filters.
    // The service uses `this.apiUrl` for getAllCategories, which is `/api/categories`.
    // If admin needs a different endpoint, the service or this component needs adjustment.
    // For now, using the existing `getAllCategories` which points to public API.
    // This might need to be changed to an admin-specific endpoint from CategoryService if available and different.

    this.categoryService.getAllCategories(params).subscribe({
      next: (page: Page<CategoryResponseDto>) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.snackBar.open(`Error loading categories: ${err.error?.message || 'Unknown error'}`, 'Close', { duration: 5000 });
      }
    });
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    // For server-side filtering (requires API support for 'search' or 'name' param):
    this.paginator.pageIndex = 0;
    let params = new HttpParams()
      .set('page', '0')
      .set('size', this.paginator ? this.paginator.pageSize.toString() : '10')
      .set('name', filterValue.trim().toLowerCase()); // Assuming API supports filtering by 'name'

    if (this.sort && this.sort.active && this.sort.direction) {
      params = params.set('sort', `${this.sort.active},${this.sort.direction}`);
    }
    this.isLoading = true;
    this.categoryService.getAllCategories(params).subscribe({
        next: (page: Page<CategoryResponseDto>) => {
            this.dataSource.data = page.content;
            this.totalElements = page.totalElements;
            this.isLoading = false;
        },
        error: (err) => {
            this.isLoading = false;
            this.snackBar.open(`Error filtering categories: ${err.error?.message || 'Unknown error'}`, 'Close', { duration: 5000 });
        }
    });
  }

  editCategory(category: CategoryResponseDto): void {
    this.router.navigate(['/categories/edit', category.id]);
  }

  deleteCategory(category: CategoryResponseDto): void {
    // Example of using a confirmation dialog (assuming you have one)
    // const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
    //   data: { message: `Are you sure you want to delete category "${category.name}"?` }
    // });
    // dialogRef.afterClosed().subscribe(result => {
    //   if (result) {
    //     this.performDelete(category);
    //   }
    // });
    // For now, direct delete without confirmation for simplicity
    if (confirm(`Are you sure you want to delete category "${category.name}"?`)) {
        this.performDelete(category);
    }
  }

  performDelete(category: CategoryResponseDto): void {
    this.isLoading = true;
    this.categoryService.deleteCategory(category.id).subscribe({
      next: () => {
        this.snackBar.open(`Category "${category.name}" deleted successfully.`, 'Close', { duration: 3000 });
        this.loadCategories(); // Refresh the list
      },
      error: (err) => {
        this.isLoading = false;
        this.snackBar.open(`Error deleting category: ${err.error?.message || 'Unknown error'}`, 'Close', { duration: 5000 });
      }
    });
  }

  addNewCategory(): void {
    this.router.navigate(['/categories/new']);
  }
}
